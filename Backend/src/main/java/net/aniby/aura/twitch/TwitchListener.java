package net.aniby.aura.twitch;

import com.github.philippheuer.events4j.core.EventManager;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.pubsub.domain.ChannelPointsRedemption;
import com.github.twitch4j.pubsub.domain.ChannelPointsUser;
import com.github.twitch4j.pubsub.events.RedemptionStatusUpdateEvent;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.modules.AuraUser;
import net.aniby.aura.tool.AuraUtils;
import net.aniby.aura.tool.Replacer;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import ninja.leaping.configurate.ConfigurationNode;

import java.io.BufferedInputStream;
import java.util.List;
import java.util.Objects;

import static net.aniby.aura.tool.Replacer.r;

public class TwitchListener {
    double rewardCost;
    TwitchClient client;

    public TwitchListener(TwitchClient client) {
        this.client = client;

        this.rewardCost = AuraBackend.getConfig().getRoot().getNode("aura", "per_points").getDouble();

        EventManager manager = this.client.getEventManager();
        manager.onEvent(RewardRedeemedEvent.class, this::onRewardRedeemed);
        manager.onEvent(RedemptionStatusUpdateEvent.class, this::onRedemptionStatusUpdate);
        manager.onEvent(ChannelGoLiveEvent.class, this::onGoLive);
        manager.onEvent(ChannelGoOfflineEvent.class, this::onGoOffline);
    }

    void onStream(Stream stream, EventChannel eventChannel) {
        String streamerId = eventChannel.getId();
        AuraUser user = AuraUser.getByWith("twitch_id", streamerId);
        if (user == null || !user.isStreamer())
            return;

        AuraConfig config = AuraBackend.getConfig();
        List<Replacer> replacerList = user.getReplacers();
        replacerList.add(r("twitch_url", "https://twitch.tv/" + eventChannel.getName()));

        String avatarUrl = config.getMessage("twitch_icon_url");
        User twitchUser = user.getTwitchUser();
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
        AuraUser viewer = AuraUser.getByWith("twitch_id", viewerUser.getId());
        if (viewer == null)
            return;

        String streamerId = redemption.getChannelId();
        AuraUser streamer = AuraUser.getByWith("twitch_id", streamerId);
        if (streamer == null)
            return;

        ConfigurationNode allows = AuraBackend.getConfig().getRoot().getNode("aura", "allow_only_if");
        boolean allowIfHasRole = allows.getNode("has_streamer_role").getBoolean(false);
        if (allowIfHasRole && !streamer.isStreamer())
            return;

        double aura = redemption.getReward().getCost() / rewardCost;

        double streamerRejectedAura = streamer.calculateAura(aura, null);
        streamer.setAura(streamer.getAura() - streamerRejectedAura);
        streamer.save();
        double viewerRejectedAura = viewer.calculateAura(aura, streamer);
        viewer.setAura(viewer.getAura() - viewer.calculateAura(aura, streamer));
        viewer.save();

        AuraBackend.getDiscord().getLogger().auraReject(viewerUser.getDisplayName(), viewer, streamer, streamerRejectedAura, viewerRejectedAura);
    }

    public void onRewardRedeemed(RewardRedeemedEvent event) {
        ChannelPointsRedemption redemption = event.getRedemption();
        String twitchStreamId = redemption.getChannelId();

        AuraUser streamer = AuraUser.getByWith("twitch_id", twitchStreamId);
        if (streamer == null)
            return;

        ChannelPointsUser viewerUser = redemption.getUser();
        String viewerId = viewerUser.getId();
        if (Objects.equals(viewerId, twitchStreamId))
            return;

        ConfigurationNode allows = AuraBackend.getConfig().getRoot().getNode("aura", "allow_only_if");

        boolean allowIfHasRole = allows.getNode("has_streamer_role").getBoolean(false);
        boolean allowIfStreaming = allows.getNode("streaming").getBoolean(false);
        if (allowIfHasRole && !streamer.isStreamer())
            return;
        if (allowIfStreaming && !streamer.isStreamingNow())
            return;

        double aura = redemption.getReward().getCost() / rewardCost;

        String viewerName = viewerUser.getDisplayName();
        AuraUser viewer = AuraUser.upsertWithTwitch(viewerName, viewerId);

        double streamerAura = streamer.addAura(aura, null);
        streamer.save();
        double viewerAura = viewer.addAura(aura, streamer);
        viewer.save();

        AuraBackend.getDiscord().getLogger().viewerEarnedAura(viewerUser.getDisplayName(), viewer, viewerAura, streamer.getTwitchName(), streamerAura);
    }
}
