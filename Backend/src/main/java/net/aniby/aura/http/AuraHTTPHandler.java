package net.aniby.aura.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.aniby.aura.AuraAPI;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.modules.AuraUser;
import net.aniby.aura.modules.CAuraUser;
import net.aniby.aura.twitch.TwitchBot;
import net.aniby.aura.twitch.TwitchIRC;
import net.aniby.aura.twitch.TwitchLinkState;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Objects;

public class AuraHTTPHandler {
    public static class Link implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            AuraHTTPUtils.Path path = new AuraHTTPUtils.Path(exchange);
            String[] way = path.getWay();

            assert way.length >= 1;
            assert Objects.equals(way[0], "link");

            if (path.getMethod().equals("GET") && way.length >= 2) {
                switch (way[1]) {
                    case "twitch" -> {
                        linkTwitch(exchange, path);
                        return;
                    }
                    case "auth" -> {
                        linkAuth(exchange, path);
                        return;
                    }
                }
            }

            AuraHTTPUtils.Answer.NOT_FOUND.handle(exchange);
        }

        public void linkAuth(HttpExchange httpExchange, AuraHTTPUtils.Path path) throws IOException {
            Map<String, String> query = path.getQuery();
            String queryCode = query.get("code");
            String queryId = query.get("id");

            TwitchLinkState state = TwitchLinkState.getByCode(queryCode);
            if (state == null || !state.getDiscordId().equals(queryId)) {
                AuraHTTPUtils.Answer.BAD_REQUEST.handle(httpExchange);
                return;
            }

            String url = AuraBackend.getTwitch().generateOAuthCodeRequest(state.getUuid());
            AuraHTTPUtils.redirect(httpExchange, url);
        }

        public void linkTwitch(HttpExchange exchange, AuraHTTPUtils.Path path) throws IOException {
            TwitchBot twitch = AuraBackend.getTwitch();

            Map<String, String> query = path.getQuery();
            if (query.containsKey("state") && query.containsKey("code")) {
                String uuid = query.get("state");
                String discordId = TwitchLinkState.getByUUID(uuid);
                if (discordId != null) {
                    String code = query.get("code");

                    Map.Entry<String, Map<String, String>> entry = twitch.generateOAuthTokenRequest(code);
                    String url = entry.getKey();
                    Map<String, String> body = entry.getValue();

                    JSONObject object;
                    try {
                        HttpResponse<String> response = IOHelper.post(url, body);
                        object = IOHelper.parse(response);
                    } catch (URISyntaxException | InterruptedException | ParseException e) {
                        AuraHTTPUtils.Answer.FORBIDDEN.handle(exchange);
                        return;
                    }

                    if (object.containsKey("access_token") && object.containsKey("refresh_token")) {
                        AuraUser streamer = AuraUser.fromRequestData(
                                discordId,
                                (String) object.get("access_token"),
                                (String) object.get("refresh_token"),
                                (long) object.get("expires_in")
                        );
                        if (streamer == null) {
                            AuraHTTPUtils.Answer.FORBIDDEN.handle(exchange);
                            return;
                        }
                        twitch.registerStreamer(streamer);

                        String redirectURL = AuraBackend.getConfig().getRoot().getNode("discord", "invite_url").getString();
                        AuraHTTPUtils.redirect(exchange, redirectURL);
                        return;
                    }
                }
            }
            AuraHTTPUtils.Answer.BAD_REQUEST.handle(exchange);
        }
    }
}
