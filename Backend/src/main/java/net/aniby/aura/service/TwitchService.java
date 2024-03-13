package net.aniby.aura.service;

import com.github.philippheuer.events4j.core.EventManager;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.pubsub.PubSubSubscription;
import com.github.twitch4j.pubsub.domain.ChannelPointsRedemption;
import com.github.twitch4j.pubsub.domain.ChannelPointsUser;
import com.github.twitch4j.pubsub.events.RedemptionStatusUpdateEvent;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.tool.AuraUtils;
import net.aniby.aura.tool.ConsoleColors;
import net.aniby.aura.tool.Replacer;
import net.aniby.aura.twitch.TwitchBot;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import ninja.leaping.configurate.ConfigurationNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.util.List;
import java.util.Objects;

import static net.aniby.aura.tool.Replacer.r;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TwitchService {
    AuraConfig config;
    double rewardCost;
    TwitchBot twitchBot;
    UserService userService;
    UserRepository userRepository;

    public TwitchService(TwitchBot twitchBot, AuraConfig config, UserService userService, UserRepository userRepository) {
        this.twitchBot = twitchBot;
        this.config = config;
        this.userService = userService;
        this.userRepository = userRepository;

        this.rewardCost = config.getRoot().getNode("aura", "per_points").getDouble();

        EventManager manager = this.twitchBot.getClient().getEventManager();
        manager.onEvent(RewardRedeemedEvent.class, this::onRewardRedeemed);
        manager.onEvent(RedemptionStatusUpdateEvent.class, this::onRedemptionStatusUpdate);
        manager.onEvent(ChannelGoLiveEvent.class, this::onGoLive);
        manager.onEvent(ChannelGoOfflineEvent.class, this::onGoOffline);
    }

    public void registerStreamer(AuraUser streamer) {
        String name = streamer.getTwitchName(),
                id = streamer.getTwitchId();
        if (name == null) {
            userService.updateTwitchName(streamer);
            name = streamer.getTwitchName();
            userRepository.update(streamer);
        }

        if (id == null || name == null)
            return;

        boolean created = twitchBot.getClient().getClientHelper().enableStreamEventListener(id, name);
        if (!created) {
            AuraBackend.getLogger().info(ConsoleColors.RED + "Can't enable stream event listener for " + name + ConsoleColors.WHITE);
            return;
        }

        List<PubSubSubscription> subscriptionList = List.of(
                twitchBot.getClient().getPubSub().listenForChannelPointsRedemptionEvents(twitchBot.getCredential(), id)
        );
    }

    void onStream(Stream stream, EventChannel eventChannel) {
        String streamerId = eventChannel.getId();
        AuraUser user = userService.getByWith("twitch_id", streamerId);
        if (user == null || !userService.isStreamer(user))
            return;

        List<Replacer> replacerList = userService.getReplacers(user);
        replacerList.add(r("twitch_url", "https://twitch.tv/" + eventChannel.getName()));

        String avatarUrl = config.getMessage("twitch_icon_url");
        User twitchUser = userService.getTwitchUser(user);
        if (twitchUser != null)
            avatarUrl = twitchUser.getProfileImageUrl();
        replacerList.add(r("twitch_avatar_url", avatarUrl));

        TextChannel channel = AuraBackend.getDiscord().getChannels().get("streams");

        String type = stream != null ? "go_live" : "go_offline";
        MessageCreateAction message;
        try {
            message = channel.sendMessage(config.getMessage(type, replacerList));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        if (stream != null) {
            replacerList.add(r("stream_title", stream.getTitle()));
            replacerList.add(r("stream_game", stream.getGameName()));

            String thumbnail = stream.getThumbnailUrl();
            try {
                BufferedInputStream image = AuraUtils.downloadFile(thumbnail);
                message = message.addFiles(FileUpload.fromData(image, user.getTwitchName() + ".jpg"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            message = message.addEmbeds(config.getEmbed(type, replacerList));
        }
        try {
            message.queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void onGoLive(ChannelGoLiveEvent event) {
        onStream(event.getStream(), event.getChannel());
    }

    void onGoOffline(ChannelGoOfflineEvent event) {
        onStream(null, event.getChannel());
    }

    void onRedemptionStatusUpdate(RedemptionStatusUpdateEvent event) {
        ChannelPointsRedemption redemption = event.getRedemption();
        if (!redemption.getStatus().equalsIgnoreCase("canceled"))
            return;

        ChannelPointsUser viewerUser = redemption.getUser();
        AuraUser viewer = userService.getByWith("twitch_id", viewerUser.getId());
        if (viewer == null)
            return;

        String streamerId = redemption.getChannelId();
        AuraUser streamer = userService.getByWith("twitch_id", streamerId);
        if (streamer == null)
            return;

        ConfigurationNode allows = config.getRoot().getNode("aura", "allow_only_if");
        boolean allowIfHasRole = allows.getNode("has_streamer_role").getBoolean(false);
        if (allowIfHasRole && !userService.isStreamer(streamer))
            return;

        double aura = redemption.getReward().getCost() / rewardCost;

        double streamerRejectedAura = userService.calculateAura(aura, null);
        streamer.setAura(streamer.getAura() - streamerRejectedAura);
        userRepository.update(streamer);
        double viewerRejectedAura = userService.calculateAura(aura, streamer);
        viewer.setAura(viewer.getAura() - viewerRejectedAura);
        userRepository.update(viewer);

        AuraBackend.getDiscord().getLogger().auraReject(viewerUser.getDisplayName(), viewer, streamer, streamerRejectedAura, viewerRejectedAura);
    }

    public void onRewardRedeemed(RewardRedeemedEvent event) {
        ChannelPointsRedemption redemption = event.getRedemption();
        String twitchStreamId = redemption.getChannelId();

        AuraUser streamer = userService.getByWith("twitch_id", twitchStreamId);
        if (streamer == null)
            return;

        ChannelPointsUser viewerUser = redemption.getUser();
        String viewerId = viewerUser.getId();
        if (Objects.equals(viewerId, twitchStreamId))
            return;

        ConfigurationNode allows = config.getRoot().getNode("aura", "allow_only_if");

        boolean allowIfHasRole = allows.getNode("has_streamer_role").getBoolean(false);
        boolean allowIfStreaming = allows.getNode("streaming").getBoolean(false);
        if (allowIfHasRole && !userService.isStreamer(streamer))
            return;
        if (allowIfStreaming && !userService.isStreamingNow(streamer))
            return;

        double aura = redemption.getReward().getCost() / rewardCost;

        String viewerName = viewerUser.getDisplayName();
        AuraUser viewer = userService.upsertWithTwitch(viewerName, viewerId);

        double streamerAura = userService.addAura(streamer, aura, null);
        userRepository.update(streamer);
        double viewerAura = userService.addAura(viewer, aura, streamer);
        userRepository.update(viewer);

        AuraBackend.getDiscord().getLogger().viewerEarnedAura(viewerUser.getDisplayName(), viewer, viewerAura, streamer.getTwitchName(), streamerAura);
    }
}
