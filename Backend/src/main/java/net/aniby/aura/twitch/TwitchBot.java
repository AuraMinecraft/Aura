package net.aniby.aura.twitch;

import com.github.twitch4j.pubsub.PubSubSubscription;
import lombok.Getter;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.modules.AuraUser;
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
        new TwitchListener(this.getClient());
    }

    public String getRedirectURI() {
        return AuraBackend.getExternalURL() + "link/twitch/";
    }

    public String generateTwitchLink(String discordId) {
        TwitchLinkState state = new TwitchLinkState(discordId, AuraUtils.minute * 15);
        return AuraBackend.getExternalURL() + "link/auth/?id=" + discordId + "&code=" + state.getCode();
    }

    public void registerStreamer(AuraUser streamer) {
        String name = streamer.getTwitchName(),
                id = streamer.getTwitchId();
        if (name == null) {
            streamer.updateTwitchName();
            name = streamer.getTwitchName();
            streamer.save();
        }

        if (id == null || name == null)
            return;

        boolean created = getClient().getClientHelper().enableStreamEventListener(id, name);
        if (!created) {
            AuraBackend.getLogger().info(ConsoleColors.RED + "Can't enable stream event listener for " + name + ConsoleColors.WHITE);
            return;
        }

        List<PubSubSubscription> subscriptionList = List.of(
                getClient().getPubSub().listenForChannelPointsRedemptionEvents(this.getCredential(), id)
        );
        streamer.getSubscriptions().addAll(subscriptionList);
    }
}
