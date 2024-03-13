package net.aniby.aura.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import lombok.Getter;
import net.aniby.aura.http.IOHelper;
import org.json.simple.JSONObject;

import java.net.http.HttpResponse;
import java.util.AbstractMap;
import java.util.Map;

@Getter
public class TwitchIRC {
    private final String clientId;
    private final String clientSecret;
    private final String accessToken;
    private final TwitchClient client;
    private final OAuth2Credential credential;
    private final String redirectURI;

    public TwitchIRC(String clientId, String clientSecret, String redirectURI) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectURI = redirectURI;

        this.accessToken = getAccessToken();
        this.credential = new OAuth2Credential("twitch", this.accessToken);

        this.client = TwitchClientBuilder.builder()
                .withEnableHelix(true)
                .withEnablePubSub(true)
                .withClientId(this.clientId)
                .withClientSecret(this.clientSecret)
                .withDefaultAuthToken(this.credential)
                .build();
    }
    private String getAccessToken() {
        try {
            Map<String, String> body = Map.of(
                    "client_id", this.clientId,
                    "client_secret", this.clientSecret,
                    "grant_type", "client_credentials"
            );
            HttpResponse<String> response = IOHelper.post("https://id.twitch.tv/oauth2/token", body);
            JSONObject object = IOHelper.parse(response);

            if (object.containsKey("access_token"))
                return (String) object.get("access_token");
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        return null;
    }

    public String generateOAuthCodeRequest(String state) {
        //  user:read:subscriptions
        String scopes = "channel:read:redemptions channel:read:subscriptions";
        return "https://id.twitch.tv/oauth2/authorize?client_id=" + clientId + "&redirect_uri=" + redirectURI + "&response_type=code&scope=" + scopes + "&state=" + state;
    }

    public Map.Entry<String, Map<String, String>> generateOAuthTokenRequest(String code) {
        Map<String, String> headers = Map.of(
                "client_id", this.clientId,
                "client_secret", this.clientSecret,
                "code", code,
                "grant_type", "authorization_code",
                "redirect_uri", redirectURI
        );
        return new AbstractMap.SimpleEntry<>(
                "https://id.twitch.tv/oauth2/token", headers
        );
    }

    public Map.Entry<String, Map<String, String>> generateRefreshRequest(String refreshToken) {
        Map<String, String> headers = Map.of(
                "client_id", this.clientId,
                "client_secret", this.clientSecret,
                "grant_type", "refresh_token",
                "refresh_token", refreshToken
        );
        return new AbstractMap.SimpleEntry<>(
                "https://id.twitch.tv/oauth2/token", headers
        );
    }

    public void sendMessage(String channel, String message) {
        this.getClient().getChat().sendMessage(channel, message);
    }
}
