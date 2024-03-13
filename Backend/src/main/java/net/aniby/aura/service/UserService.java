package net.aniby.aura.service;

import com.github.twitch4j.domain.ChannelCache;
import com.github.twitch4j.helix.domain.User;
import com.j256.ormlite.stmt.Where;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.discord.DiscordIRC;
import net.aniby.aura.http.IOHelper;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.tool.Replacer;
import net.aniby.aura.twitch.AccessData;
import net.aniby.aura.twitch.TwitchBot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import ninja.leaping.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.*;

import static net.aniby.aura.tool.Replacer.r;

import net.aniby.aura.entity.AuraUser;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    AuraConfig config;
    AuraDatabase database;
    UserRepository userRepository;
    TwitchBot twitchBot;

    @Autowired
    @SneakyThrows
    public UserService(AuraConfig config, TwitchBot twitchBot, AuraDatabase database, UserRepository userRepository) {
        this.config = config;
        this.userRepository = userRepository;
        this.database = database;
        this.twitchBot = twitchBot;
    }


    @SneakyThrows
    public AuraUser getByWithOr(Object... args) {
        List<Object> list = Arrays.stream(args).toList();
        if (list.size() % 2 == 0) {
            Where<AuraUser, Integer> where =
                    database.getUsers()
                            .queryBuilder()
                            .where();
            int index = 0;
            while (index < list.size()) {
                where = where.eq((String) list.get(index), list.get(index + 1));
                index += 2;
                if (index < list.size())
                    where = where.or();
            }
            return where.queryForFirst();
        }
        return null;
    }

    @SneakyThrows
    public AuraUser getByWith(Object... args) {
        List<Object> list = Arrays.stream(args).toList();
        if (list.size() % 2 == 0) {
            Where<AuraUser, Integer> where =
                    database.getUsers()
                            .queryBuilder()
                            .where();
            int index = 0;
            while (index < list.size()) {
                where = where.eq((String) list.get(index), list.get(index + 1));
                index += 2;
                if (index < list.size())
                    where = where.and();
            }
            return where.queryForFirst();
        }
        return null;
    }

    public void init(AuraUser user) {
        if (user.getTwitchId() != null)
            twitchBot.registerStreamer(user);
    }

    // Constructors
    public AuraUser upsertWithTwitch(String twitchName, String twitchId) {
        AuraUser user = getByWith("twitch_id", twitchId);
        if (user == null) {
            user = new AuraUser();
            user.setTwitchId(twitchId);
            user.setTwitchName(twitchName);

            userRepository.update(user);
        } else if (user.getTwitchName() == null) {
            user.setTwitchName(twitchName);
            userRepository.update(user);
        }
        return user;
    }

    // Aura
    public double addAura(AuraUser user, double aura, AuraUser streamer) {
        double calculated = calculateAura(aura, streamer);
        user.setAura(calculated + user.getAura());
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
        ConfigurationNode auraNode = config.getRoot().getNode("aura");
        double viewerAmplifier = auraNode.getNode("viewer_amplifier").getDouble();
        return streamer != null ? getStreamerAuraAmplifier() * viewerAmplifier : 1;
    }

    // Twitch Work
    public boolean updateTwitchName(AuraUser auraUser) {
        User user = getTwitchUser(auraUser);
        if (user != null) {
            auraUser.setTwitchName(user.getDisplayName());
            userRepository.update(auraUser);
            return true;
        }
        return false;
    }

    // Getters
    public List<Replacer> getReplacers(AuraUser user) {
        net.dv8tion.jda.api.entities.User discordUser = this.getDiscordUser(user);
        String username = "";
        if (user.getPlayerName() != null)
            username = user.getPlayerName();
        else if (discordUser != null)
            username = discordUser.getName();
        else if (user.getTwitchName() != null)
            username = user.getTwitchName();

        String avatar = user.getPlayerName() != null
                ? "https://cravatar.eu/helmavatar/" + user.getPlayerName() + "/64.png"
                : config.getMessage("twitch_icon_url");
        String promoMention = user.getPromoDiscordId() == null ? "-" : "<@" + user.getPromoDiscordId() + ">";
        return new ArrayList<>(List.of(
                r("promo_discord_mention", promoMention),
                r("promo_discord_id", user.getPromoDiscordId() == null ? "-" : user.getPromoDiscordId()),
                r("discord_mention", discordUser == null ? "-" : discordUser.getAsMention()),
                r("discord_name", discordUser == null ? "-" : discordUser.getName()),
                r("discord_id", user.getDiscordId() == null ? "-" : user.getDiscordId()),
                r("twitch_name", user.getTwitchName() == null ? "-" : user.getTwitchName()),
                r("twitch_id", user.getTwitchId() == null ? "-" : user.getTwitchId()),
                r("player_name", user.getPlayerName() == null ? "-" : user.getPlayerName()),
                r("user_name", username),
                r("avatar", avatar),
                r("aura", String.valueOf(user.getFormattedAura()))
        ));
    }

    public @Nullable Member getGuildMember(AuraUser user) {
        Guild guild = AuraBackend.getDiscord().getDefaultGuild();
        if (user.getDiscordId() == null || guild == null)
            return null;
        try {
            return guild.retrieveMemberById(user.getDiscordId()).complete();
        } catch (ErrorResponseException exception) {
            return null;
        }
    }

    public boolean isStreamer(AuraUser user) {
        Role role = AuraBackend.getDiscord().getRoles().get("streamer");
        if (role != null) {
            Member member = getGuildMember(user);
            if (member != null) {
                return member.getRoles().contains(role);
            }
        }
        return false;
    }

    public boolean isStreamingNow(AuraUser user) {
        return user.getTwitchId() != null && twitchBot.getClient().getClientHelper()
                .getCachedInformation(user.getTwitchId())
                .map(ChannelCache::getIsLive).orElse(false);
    }

    public @Nullable User getTwitchUser(AuraUser user) {
        try {
            if (user.getTwitchId() != null)
                return twitchBot.getClient().getHelix()
                        .getUsers(null, List.of(user.getTwitchId()), null)
                        .execute()
                        .getUsers()
                        .get(0);
        } catch (Exception ignored) {
        }
        return null;
    }

    public @Nullable net.dv8tion.jda.api.entities.User getDiscordUser(AuraUser user) {
        try {
            if (user.getDiscordId() != null) {
                return AuraBackend.getDiscord().getJda().retrieveUserById(user.getDiscordId()).complete();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public boolean inGuild(AuraUser user) {
        return getGuildMember(user) != null;
    }

    // Static Methods
    @SneakyThrows
    public AuraUser fromRequestData(@NotNull String discordId, @NotNull String accessToken, @NotNull String refreshToken) {
        User twitchUser = twitchBot.getClient().getHelix()
                .getUsers(accessToken, null, null)
                .execute()
                .getUsers()
                .get(0);

        AuraUser user = database.getUsers().queryBuilder()
                .where()
                .eq("twitch_id", twitchUser.getId())
                .queryForFirst();
        if (user == null) {
            user = new AuraUser();
            user.setTwitchId(twitchUser.getId());
        }
        user.setTwitchName(twitchUser.getDisplayName());
        user.setRefreshToken(refreshToken);
        user.setDiscordId(discordId);
        userRepository.update(user);

        net.dv8tion.jda.api.entities.User discordUser = getDiscordUser(user);

        if (discordUser == null)
            return null;

        try {
            List<Replacer> replacers = getReplacers(user);
            discordUser.openPrivateChannel()
                    .flatMap(privateChannel ->
                            privateChannel.sendMessage(
                                    config.getMessage(
                                            "lc_twitch_successfully_linked",
                                            replacers
                                    )
                            )
                    )
                    .queue();
        } catch (Exception ignored) {
        }

        try {
            if (user.getPlayerName() != null) {
                Member member = getGuildMember(user);
                if (member != null)
                    member.modifyNickname(user.getPlayerName()).queue();
            }
        } catch (Exception ignored) {
        }

        try {
            DiscordIRC irc = AuraBackend.getDiscord();
            irc.getDefaultGuild().addRoleToMember(discordUser, irc.getRoles().get("twitch")).queue();
        } catch (Exception ignored) {
        }
        return user;
    }

    public AccessData getAccessData(String refreshToken) throws URISyntaxException, IOException, InterruptedException, ParseException {
        Map.Entry<String, Map<String, String>> entry = twitchBot.generateRefreshRequest(refreshToken);
        HttpResponse<String> response = IOHelper.post(entry.getKey(), entry.getValue());
        JSONObject object = IOHelper.parse(response);
        if (object.containsKey("access_token") && object.containsKey("refresh_token")) {
            return new AccessData(
                    (String) object.get("access_token"),
                    (String) object.get("refresh_token"),
                    (long) object.get("expires_in")
            );
        }
        return null;
    }
}
