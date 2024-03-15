package net.aniby.aura.service.twitch;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.philippheuer.events4j.core.EventManager;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.common.events.domain.EventChannel;
import com.github.twitch4j.domain.ChannelCache;
import com.github.twitch4j.events.ChannelGoLiveEvent;
import com.github.twitch4j.events.ChannelGoOfflineEvent;
import com.github.twitch4j.helix.domain.Stream;
import com.github.twitch4j.helix.domain.User;
import com.github.twitch4j.pubsub.domain.ChannelPointsRedemption;
import com.github.twitch4j.pubsub.domain.ChannelPointsUser;
import com.github.twitch4j.pubsub.events.RedemptionStatusUpdateEvent;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;
import lombok.Getter;
import lombok.SneakyThrows;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.http.IOHelper;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.service.discord.DiscordLogger;
import net.aniby.aura.service.discord.DiscordIRC;
import net.aniby.aura.service.user.UserService;
import net.aniby.aura.tool.AuraUtils;
import net.aniby.aura.tool.Replacer;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import ninja.leaping.configurate.ConfigurationNode;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.net.http.HttpResponse;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static net.aniby.aura.tool.Replacer.r;

@Service
public class TwitchIRC {
    AuraConfig config;
    UserService userService;
    UserRepository userRepository;
    DiscordLogger loggerService;
    DiscordIRC discordIRC;

    @Getter final double rewardCost;
    @Getter final String clientId;
    @Getter final String clientSecret;
    @Getter final TwitchClient client;
    @Getter final OAuth2Credential credential;
    @Getter final String redirectURI;

    @SneakyThrows
    public TwitchIRC(AuraConfig config,
                     @Lazy UserService userService,
                     UserRepository userRepository,
                     DiscordLogger loggerService,
                     @Lazy DiscordIRC discordIRC,
                     AuraDatabase database) {
        this.config = config;
        this.userService = userService;
        this.userRepository = userRepository;
        this.loggerService = loggerService;
        this.discordIRC = discordIRC;

        ConfigurationNode node = config.getRoot().getNode("twitch", "application");
        this.clientId = node.getNode("client_id").getString();
        this.clientSecret = node.getNode("client_secret").getString();

        String url = config.getRoot().getNode("http_server", "external_url").getString();
        this.redirectURI = url + "link/twitch/";

        this.credential = new OAuth2Credential("twitch", this.generateAccessToken());

        this.client = TwitchClientBuilder.builder()
                .withEnableHelix(true)
                .withEnablePubSub(true)
                .withClientId(this.clientId)
                .withClientSecret(this.clientSecret)
                .withDefaultAuthToken(this.credential)
                .build();

        this.rewardCost = config.getRoot().getNode("aura", "per_points").getDouble();

        EventManager manager = this.client.getEventManager();
        manager.onEvent(RewardRedeemedEvent.class, this::onRewardRedeemed);
        manager.onEvent(RedemptionStatusUpdateEvent.class, this::onRedemptionStatusUpdate);
        manager.onEvent(ChannelGoLiveEvent.class, this::onGoLive);
        manager.onEvent(ChannelGoOfflineEvent.class, this::onGoOffline);

        database.getUsers().queryForAll().forEach(this::initUser);
    }

    public boolean isStreamingNow(AuraUser user) {
        return user.getTwitchId() != null && client.getClientHelper()
                .getCachedInformation(user.getTwitchId())
                .map(ChannelCache::getIsLive).orElse(false);
    }

    public @Nullable User getTwitchUser(AuraUser user) {
        try {
            if (user.getTwitchId() != null)
                return client.getHelix()
                        .getUsers(null, List.of(user.getTwitchId()), null)
                        .execute()
                        .getUsers()
                        .get(0);
        } catch (Exception ignored) {
        }
        return null;
    }

    public void initUser(AuraUser user) {
        if (user.getTwitchId() != null)
            registerStreamer(user);
    }

    private String generateAccessToken() {
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


    public String generateTwitchLink(String discordId) {
        String url = config.getRoot().getNode("http_server", "external_url").getString();
        TwitchLinkState state = new TwitchLinkState(discordId, AuraUtils.minute * 15);
        return url + "link/auth/?id=" + discordId + "&code=" + state.getCode();
    }

    public void registerStreamer(AuraUser streamer) {
        String name = streamer.getTwitchName(),
                id = streamer.getTwitchId();
        if (name == null) {
            updateTwitchName(streamer);
            name = streamer.getTwitchName();
            userRepository.update(streamer);
        }

        if (id == null || name == null)
            return;

        boolean created = client.getClientHelper().enableStreamEventListener(id, name);
        if (!created) {
            return;
        }

        client.getPubSub().listenForChannelPointsRedemptionEvents(this.getCredential(), id);
    }

    void onStream(Stream stream, EventChannel eventChannel) {
        String streamerId = eventChannel.getId();
        AuraUser user = userService.getByWith("twitch_id", streamerId);
        if (user == null || !userService.isStreamer(user))
            return;

        List<Replacer> replacerList = userService.getReplacers(user);
        replacerList.add(r("twitch_url", "https://twitch.tv/" + eventChannel.getName()));

        String avatarUrl = config.getMessage("twitch_icon_url");
        User twitchUser = getTwitchUser(user);
        if (twitchUser != null)
            avatarUrl = twitchUser.getProfileImageUrl();
        replacerList.add(r("twitch_avatar_url", avatarUrl));

        TextChannel channel = discordIRC.getChannels().get("streams");

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

            String thumbnail = stream.getThumbnailUrl(860, 480);
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

    public boolean updateTwitchName(AuraUser auraUser) {
        User user = getTwitchUser(auraUser);
        if (user != null) {
            auraUser.setTwitchName(user.getDisplayName());
            userRepository.update(auraUser);
            return true;
        }
        return false;
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

        loggerService.auraReject(viewerUser.getDisplayName(), viewer, streamer, streamerRejectedAura, viewerRejectedAura);
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
        if (allowIfStreaming && !isStreamingNow(streamer))
            return;

        double aura = redemption.getReward().getCost() / rewardCost;

        String viewerName = viewerUser.getDisplayName();
        AuraUser viewer = userService.upsertWithTwitch(viewerName, viewerId);

        double streamerAura = userService.addAura(streamer, aura, null);
        userRepository.update(streamer);
        double viewerAura = userService.addAura(viewer, aura, streamer);
        userRepository.update(viewer);

        loggerService.viewerEarnedAura(viewerUser.getDisplayName(), viewer, viewerAura, streamer.getTwitchName(), streamerAura);
    }
}
