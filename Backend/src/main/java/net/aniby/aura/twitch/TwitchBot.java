package net.aniby.aura.twitch;

import com.github.twitch4j.pubsub.PubSubSubscription;
import lombok.Getter;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.service.TwitchService;
import net.aniby.aura.tool.AuraUtils;
import net.aniby.aura.tool.ConsoleColors;
import ninja.leaping.configurate.ConfigurationNode;

import java.util.List;

@Getter
public class TwitchBot extends TwitchIRC {
    public TwitchBot(ConfigurationNode node) {
        super(
                node.getNode("client_id").getString(),
                node.getNode("client_secret").getString(),
                node.getNode("redirect_uri").getString()
        );
    }

    public String getRedirectURI() {
        return AuraBackend.getExternalURL() + "link/twitch/";
    }

    public String generateTwitchLink(String discordId) {
        TwitchLinkState state = new TwitchLinkState(discordId, AuraUtils.minute * 15);
        return AuraBackend.getExternalURL() + "link/auth/?id=" + discordId + "&code=" + state.getCode();
    }
}
