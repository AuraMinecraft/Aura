package net.aniby.aura.modules;

import com.github.twitch4j.domain.ChannelCache;
import com.github.twitch4j.helix.TwitchHelix;
import com.github.twitch4j.helix.domain.User;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;
import lombok.*;
import net.aniby.aura.AuraAPI;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.discord.DiscordIRC;
import net.aniby.aura.http.IOHelper;
import net.aniby.aura.tools.Replacer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import ninja.leaping.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.util.*;

import static net.aniby.aura.tools.Replacer.r;

@Getter
@Setter
@DatabaseTable(tableName = "users")
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuraUser extends CAuraUser {
    public static AuraUser cast(CAuraUser old) {
        return new AuraUser(
                old.getPlayerName(),
                old.getTwitchId(),
                old.getTwitchName(),
                old.getRefreshToken(),
                old.getDiscordId(),
                old.getAccessToken(),
                old.getExpiresAt(),
                old.getAura(),
                old.isWhitelisted(),
                old.getPromoDiscordId()
        );
    }

    @SneakyThrows
    public static AuraUser getByWith(Object... args) {
        List<Object> list = Arrays.stream(args).toList();
        if (list.size() % 2 == 0) {
            Where<CAuraUser, Integer> where = AuraAPI.getDatabase().getUsers()
                    .queryBuilder()
                    .where();
            int index = 0;
            while (index < list.size()) {
                where = where.eq((String) list.get(index), list.get(index + 1));
                index += 2;
                if (index < list.size())
                    where = where.and();
            }
            return cast(where.queryForFirst());
        }
        return null;
    }

    public void init() throws SQLException {
        if (this.checkForForce())
            AuraBackend.getTwitch().registerStreamer(this);
        else if (this.getTwitchId() != null)
            this.updateTwitchName();
    }

    // Constructors
    public static AuraUser upsertWithDiscordId(String discordId) {
        AuraUser user = getByWith("discord_id", discordId);
        if (user == null) {
            user = new AuraUser(
                    null, discordId, null, null, null, null,
                    0, 0, false, null
            );
            user.save();
        }
        return user;
    }
    public static AuraUser upsertWithTwitch(String twitchName, String twitchId) {
        AuraUser user = getByWith("twitch_id", twitchId);
        if (user == null) {
            user = new AuraUser(
                    null, null, twitchName, twitchId, null, null,
                    0, 0, false, null
            );
            user.save();
        } else if (user.getTwitchName() == null) {
            user.setTwitchName(twitchName);
            user.save();
        }
        return user;
    }

    public AuraUser(String playerName,
                      String discordId,
                      String twitchName,
                      String twitchId,
                      String accessToken,
                      String refreshToken,
                      long expiresAt,
                      double aura,
                      boolean whitelisted,
                      String promoDiscordId) {
        super(playerName, discordId, twitchName, twitchId, accessToken, refreshToken, expiresAt, aura, whitelisted, promoDiscordId);
    }
    // Aura
    public double addAura(double aura, AuraUser streamer) {
        double calculated = calculateAura(aura, streamer);
        this.setAura(calculated + this.getAura());
        return calculated;
    }

    public double calculateAura(double aura, AuraUser streamer) {
        return aura * (streamer == null ? getStreamerAuraAmplifier() : getViewerAuraAmplifier(streamer));
    }

    public double getEventAmplifier() {
        return 1 / getViewerAuraAmplifier(null);
    }

    private double getStreamerAuraAmplifier() {
        return 1;
    }

    private double getViewerAuraAmplifier(AuraUser streamer) {
        ConfigurationNode auraNode = AuraBackend.getConfig().getRoot().getNode("aura");
        double viewerAmplifier = auraNode.getNode("viewer_amplifier").getDouble();
        return streamer != null ? streamer.getStreamerAuraAmplifier() * viewerAmplifier : 1;
    }

    // Twitch Work
    boolean checkForForce() throws SQLException {
        if (this.getRefreshToken() != null) {
            if (this.updateFields(true)) {
                return true;
            }

            this.setRefreshToken(null);
            this.save();
            AuraBackend.getLogger().warning(
                    String.format("Invalid refreshToken for (%s / %s)", getPlayerName(), getTwitchName())
            );
        }
        return false;
    }

    boolean updateTwitchName() {
        if (this.getTwitchId() != null && this.getTwitchName() != null) {
            User user = getTwitchUser();
            if (user != null) {
                this.setTwitchName(user.getDisplayName());
                return true;
            }
        }
        return false;
    }

    boolean updateFields(boolean force) {
        if (this.getRefreshToken() != null) {
            if (new Date().getTime() >= this.getExpiresAt() || force) {
                try {
                    this.setAccessToken(null);
                    if (!refresh())
                        return false;
                } catch (Exception ignored) {}
            }

            if (getTwitchName() == null) {
                if (this.getTwitchId() != null || this.getAccessToken() != null) {
                    TwitchHelix helix = AuraBackend.getTwitch().getClient().getHelix();
                    List<User> list = this.getTwitchId() != null
                            ? helix.getUsers(null, List.of(this.getTwitchId()), null).execute().getUsers()
                            : helix.getUsers(this.getAccessToken(), null, null).execute().getUsers();

                    if (list.size() == 1) {
                        User user = list.get(0);
                        this.setTwitchName(user.getDisplayName());
                        this.setTwitchId(user.getId());
                        return true;
                    }
                }
                return false;
            }
        } else if (this.getTwitchId() != null) {
            if (force || this.getTwitchName() == null) {
                List<User> list = AuraBackend.getTwitch().getClient().getHelix()
                        .getUsers(null, Collections.singletonList(this.getTwitchId()), null)
                        .execute().getUsers();

                if (list.size() == 1) {
                    this.setTwitchName(list.get(0).getDisplayName());
                    return true;
                }
            }
        }
        return false;
    }

    // Getters
    public List<Replacer> getReplacers() {
        net.dv8tion.jda.api.entities.User discordUser = this.getDiscordUser();
        String username = "";
        if (this.getPlayerName() != null)
            username = this.getPlayerName();
        else if (discordUser != null)
            username = discordUser.getName();
        else if (this.getTwitchName() != null)
            username = this.getTwitchName();

        String avatar = getPlayerName() != null
                ? "https://cravatar.eu/helmavatar/" + getPlayerName() + "/64.png"
                : AuraBackend.getConfig().getMessage("twitch_icon_url");
        String promoMention = getPromoDiscordId() == null ? "-" : "<@" + getPromoDiscordId() + ">";
        return new ArrayList<>(List.of(
                r("promo_discord_mention", promoMention),
                r("promo_discord_id", getPromoDiscordId() == null ? "-" : getPromoDiscordId()),
                r("discord_mention", discordUser == null ? "-" : discordUser.getAsMention()),
                r("discord_name", discordUser == null ? "-" : discordUser.getName()),
                r("discord_id", getDiscordId() == null ? "-" : getDiscordId()),
                r("twitch_name", getTwitchName() == null ? "-" : getTwitchName()),
                r("twitch_id", getTwitchId() == null ? "-" : getTwitchId()),
                r("player_name", getPlayerName() == null ? "-" : getPlayerName()),
                r("user_name", username),
                r("avatar", avatar),
                r("aura", String.valueOf(this.getFormattedAura()))
        ));
    }

    public @Nullable Member getGuildMember() {
        Guild guild = AuraBackend.getDiscord().getDefaultGuild();
        if (this.getDiscordId() == null || guild == null)
            return null;
        try {
            return guild.retrieveMemberById(this.getDiscordId()).complete();
        } catch (ErrorResponseException exception) {
            return null;
        }
    }

    public boolean isStreamer() {
        Role role = AuraBackend.getDiscord().getRoles().get("streamer");
        if (role != null) {
            Member member = this.getGuildMember();
            if (member != null) {
                return member.getRoles().contains(role);
            }
        }
        return false;
    }

    public boolean isStreamingNow() {
        return getTwitchId() != null && AuraBackend.getTwitch().getClient().getClientHelper()
                .getCachedInformation(getTwitchId())
                .map(ChannelCache::getIsLive).orElse(false);
    }

    public @Nullable User getTwitchUser() {
        try {
            if (this.getTwitchId() != null)
                return AuraBackend.getTwitch().getClient().getHelix()
                        .getUsers(null, List.of(this.getTwitchId()), null)
                        .execute()
                        .getUsers()
                        .get(0);
        } catch (Exception ignored) {}
        return null;
    }

    public @Nullable net.dv8tion.jda.api.entities.User getDiscordUser() {
        try {
            if (this.getDiscordId() != null) {
                return AuraBackend.getDiscord().getJda().retrieveUserById(this.getDiscordId()).complete();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public boolean inGuild() {
        return getGuildMember() != null;
    }

    // Static Methods
    @SneakyThrows
    public static AuraUser fromRequestData(@NotNull String discordId, @NotNull String accessToken, @NotNull String refreshToken, long expiresIn) {
        long expiresAt = expiresIn * 1000L + new Date().getTime();

        AuraUser user;
        AuraUser foundByDiscord = getByWith("discord_id", discordId);
        if (foundByDiscord != null) {
            user = foundByDiscord;
            user.setRefreshToken(refreshToken);
        } else {
            user = new AuraUser(
                    null, discordId, null, null, accessToken, refreshToken, expiresAt, 0,
                    false, null);
        }
        net.dv8tion.jda.api.entities.User discordUser = user.getDiscordUser();
        if (discordUser == null || !user.updateFields(true))
            return null;

        String twitchId = user.getTwitchId();
        AuraUser foundByTwitch = getByWith("twitch_id", twitchId);
        if (foundByTwitch != null) {
            foundByTwitch.transferFrom(user);

            user = foundByTwitch;
        }
        user.save();

        try {
            List<Replacer> replacers = user.getReplacers();
            discordUser.openPrivateChannel()
                    .flatMap(privateChannel ->
                            privateChannel.sendMessage(
                                    AuraBackend.getConfig().getMessage(
                                            "lc_twitch_successfully_linked",
                                            replacers
                                    )
                            )
                    )
                    .queue();
        } catch (Exception ignored) {}

        try {
            if (user.getPlayerName() != null) {
                Member member = user.getGuildMember();
                if (member != null)
                    member.modifyNickname(user.getPlayerName()).queue();
            }
        } catch (Exception ignored) {}

        try {
            DiscordIRC irc = AuraBackend.getDiscord();
            irc.getDefaultGuild().addRoleToMember(discordUser, irc.getRoles().get("twitch")).queue();
        } catch (Exception ignored) {}
        return user;
    }

    public boolean refresh() throws URISyntaxException, IOException, InterruptedException, ParseException {
        Map.Entry<String, Map<String, String>> entry = AuraBackend.getTwitch().generateRefreshRequest(this.getRefreshToken());
        HttpResponse<String> response = IOHelper.post(entry.getKey(), entry.getValue());
        JSONObject object = IOHelper.parse(response);
        if (object.containsKey("access_token") && object.containsKey("refresh_token")) {
            this.setAccessToken((String) object.get("access_token"));
            this.setRefreshToken((String) object.get("refresh_token"));
            this.setExpiresAt((long) object.get("expires_in") * 1000L + new Date().getTime());
            return true;
        }
        return false;
    }
}
