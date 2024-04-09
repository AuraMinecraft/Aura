package net.aniby.aura.service.user;

import com.github.twitch4j.helix.domain.User;
import com.j256.ormlite.stmt.Where;
import io.graversen.minecraft.rcon.service.MinecraftRconService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.util.AuraConfig;
import net.aniby.aura.http.IOHelper;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.service.discord.DiscordIRC;
import net.aniby.aura.tool.Replacer;
import net.aniby.aura.service.twitch.AccessData;
import net.aniby.aura.service.twitch.TwitchIRC;
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

import static net.aniby.aura.tool.AuraUtils.onlyDigits;
import static net.aniby.aura.tool.Replacer.r;

import net.aniby.aura.entity.AuraUser;

@Service
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    AuraConfig config;
    AuraDatabase database;
    UserRepository userRepository;
    DiscordIRC discordIRC;
    MinecraftRconService rconService;

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

    @SneakyThrows
    public @Nullable String extractNameBySocialSelector(@NotNull String identifier) {
        AuraUser user;
        if (identifier.startsWith("tid/") || identifier.startsWith("twitch_id/")) {
            String twitchId = identifier.split("/", 2)[1];
            user = getByWith("twitch_id", twitchId);
            return user != null ? user.getTwitchName() : null;
        } else if (identifier.startsWith("twitch/") || identifier.startsWith("t/")) {
            return identifier.split("/", 2)[1];
        } else if (identifier.startsWith("<@") && identifier.endsWith(">") && identifier.length() >= 20) {
            String discordId = identifier.substring(2, identifier.length() - 1);
            user = getByWith("discord_id", discordId);
            if (user != null) {
                net.dv8tion.jda.api.entities.User discordUser = getDiscordUser(user);
                return discordUser != null ? discordUser.getName() : null;
            }
            return null;
        } else if (onlyDigits(identifier) && identifier.length() >= 17) {
            user = getByWith("discord_id", identifier);
            if (user != null) {
                net.dv8tion.jda.api.entities.User discordUser = getDiscordUser(user);
                return discordUser != null ? discordUser.getName() : null;
            }
            return null;
        }
        return identifier;
    }

    public @Nullable AuraUser extractBySocialSelector(@NotNull String identifier) {
        if (identifier.startsWith("tid/") || identifier.startsWith("twitch_id/")) {
            String twitchId = identifier.split("/")[1];
            return getByWith("twitch_id", twitchId);
        }
        else if (identifier.startsWith("twitch/") || identifier.startsWith("t/")) {
            String twitchName = identifier.split("/")[1];
            return getByWith("twitch_name", twitchName);
        } else if (identifier.startsWith("<@") && identifier.endsWith(">") && identifier.length() >= 20) {
            String discordId = identifier.replace("<@", "").replace(">", "");
            return getByWith("discord_id", discordId);
        } else if (onlyDigits(identifier) && identifier.length() >= 17) {
            return getByWith("discord_id", identifier);
        }
        return getByWith("player_name", identifier);
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


    // Getters
    public List<Replacer> getReplacers(AuraUser user) {
        return getReplacers(user, "");
    }

    public List<Replacer> getReplacers(AuraUser user, @NotNull String prefix) {
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
                r(prefix + "promo_discord_mention", promoMention),
                r(prefix + "promo_discord_id", user.getPromoDiscordId() == null ? "-" : user.getPromoDiscordId()),
                r(prefix + "discord_mention", discordUser == null ? "-" : discordUser.getAsMention()),
                r(prefix + "discord_name", discordUser == null ? "-" : discordUser.getName()),
                r(prefix + "discord_id", user.getDiscordId() == null ? "-" : user.getDiscordId()),
                r(prefix + "twitch_name", user.getTwitchName() == null ? "-" : user.getTwitchName()),
                r(prefix + "twitch_id", user.getTwitchId() == null ? "-" : user.getTwitchId()),
                r(prefix + "player_name", user.getPlayerName() == null ? "-" : user.getPlayerName()),
                r(prefix + "user_name", username),
                r(prefix + "avatar", avatar),
                r(prefix + "aura", String.valueOf(user.getFormattedAura()))
        ));
    }

    public @Nullable Member getGuildMember(AuraUser user) {
        Guild guild = discordIRC.getDefaultGuild();
        if (user.getDiscordId() == null || guild == null)
            return null;
        try {
            return guild.retrieveMemberById(user.getDiscordId()).complete();
        } catch (ErrorResponseException exception) {
            return null;
        }
    }

    public boolean isStreamer(AuraUser user) {
        Role role = discordIRC.getRoles().get("streamer");
        if (role != null) {
            Member member = getGuildMember(user);
            if (member != null) {
                return member.getRoles().contains(role);
            }
        }
        return false;
    }

    public @Nullable net.dv8tion.jda.api.entities.User getDiscordUser(AuraUser user) {
        try {
            if (user.getDiscordId() != null) {
                return discordIRC.getJda().retrieveUserById(user.getDiscordId()).complete();
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
    public AuraUser fromRequestData(TwitchIRC twitchIRC, @NotNull String discordId, @NotNull String accessToken, @NotNull String refreshToken) {
        User twitchUser = twitchIRC.getClient().getHelix()
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
        AuraUser dbDiscordUser = database.getUsers().queryBuilder()
                .where()
                .eq("discord_id", discordId)
                .queryForFirst();
        if (dbDiscordUser != null) {
            user.setAura(user.getAura() + dbDiscordUser.getAura());
            if (dbDiscordUser.getPlayerName() != null)
                user.setPlayerName(dbDiscordUser.getPlayerName());
            userRepository.delete(dbDiscordUser);
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
            discordIRC.getDefaultGuild().addRoleToMember(discordUser, discordIRC.getRoles().get("twitch")).queue();
        } catch (Exception ignored) {
        }
        return user;
    }

    public AccessData getAccessData(TwitchIRC twitchIRC, String refreshToken) throws URISyntaxException, IOException, InterruptedException, ParseException {
        Map.Entry<String, Map<String, String>> entry = twitchIRC.generateRefreshRequest(refreshToken);
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

    public void setWhitelist(AuraUser user, boolean whitelisted) {
        if (user.getPlayerName() == null)
            return;

        user.setWhitelisted(whitelisted);
        rconService.minecraftRcon().ifPresent(rcon -> rcon.sendAsync(() -> String.format(
                "simplewhitelist %s %s", whitelisted ? "add" : "remove", user.getPlayerName()
        )));
    }
}
