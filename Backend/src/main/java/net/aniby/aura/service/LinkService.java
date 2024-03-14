package net.aniby.aura.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.http.IOHelper;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.twitch.TwitchIRC;
import net.aniby.aura.twitch.TwitchLinkState;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LinkService {
    UserRepository userRepository;
    UserService userService;
    TwitchIRC twitchIRC;
    AuraConfig config;
    TwitchService twitchService;


    public void auth(HttpServletResponse response, String code, String id) throws IOException {
        TwitchLinkState state = TwitchLinkState.getByCode(code);
        if (state == null || !state.getDiscordId().equals(id))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        String url = twitchIRC.generateOAuthCodeRequest(state.getUuid());
        response.sendRedirect(url);
    }

    public void twitch(HttpServletResponse response, String code, String uuid) throws URISyntaxException, IOException, InterruptedException, ParseException {
        String discordId = TwitchLinkState.getByUUID(uuid);
        if (discordId == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        Map.Entry<String, Map<String, String>> entry = twitchIRC.generateOAuthTokenRequest(code);
        String url = entry.getKey();
        Map<String, String> body = entry.getValue();

        JSONObject object;

        object = IOHelper.parse(
                IOHelper.post(url, body)
        );

        if (object.containsKey("access_token") && object.containsKey("refresh_token")) {
            AuraUser streamer = userService.fromRequestData(
                    discordId,
                    (String) object.get("access_token"),
                    (String) object.get("refresh_token")
            );
            if (streamer == null)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);

            twitchService.registerStreamer(streamer);

            String redirectURL = config.getRoot().getNode("discord", "invite_url").getString();
            response.sendRedirect(redirectURL);
            return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
}
