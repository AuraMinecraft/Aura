package net.aniby.aura;

import lombok.SneakyThrows;
import net.aniby.aura.modules.AuraUser;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.aniby.aura.tool.AuraUtils.onlyDigits;

public class BackendTools {
    @SneakyThrows
    public static @Nullable String extractNameBySocialSelector(@NotNull String identifier) {
        AuraUser user;
        if (identifier.startsWith("tid/") || identifier.startsWith("twitch_id/")) {
            String twitchId = identifier.split("/", 2)[1];
            user = AuraUser.getByWith("twitch_id", twitchId);
            return user != null ? user.getTwitchName() : null;
        } else if (identifier.startsWith("twitch/") || identifier.startsWith("t/")) {
            return identifier.split("/", 2)[1];
        } else if (identifier.startsWith("<@") && identifier.endsWith(">") && identifier.length() >= 20) {
            String discordId = identifier.substring(2, identifier.length() - 1);
            user = AuraUser.getByWith("discord_id", discordId);
            if (user != null) {
                User discordUser = user.getDiscordUser();
                return discordUser != null ? discordUser.getName() : null;
            }
            return null;
        } else if (onlyDigits(identifier) && identifier.length() >= 17) {
            user = AuraUser.getByWith("discord_id", identifier);
            if (user != null) {
                User discordUser = user.getDiscordUser();
                return discordUser != null ? discordUser.getName() : null;
            }
            return null;
        }
        return identifier;
    }

    public static @Nullable AuraUser extractBySocialSelector(@NotNull String identifier) {
        if (identifier.startsWith("tid/") || identifier.startsWith("twitch_id/")) {
            String twitchId = identifier.split("/")[1];
            return AuraUser.getByWith("twitch_id", twitchId);
//        }
//        else if (identifier.startsWith("twitch/") || identifier.startsWith("t/")) {
//            String twitchName = identifier.split("/")[1];
//            return CAuraUser.getByTwitchName(twitchName);
        } else if (identifier.startsWith("<@") && identifier.endsWith(">") && identifier.length() >= 20) {
            String discordId = identifier.replace("<@", "").replace(">", "");
            return AuraUser.getByWith("discord_id", discordId);
        } else if (onlyDigits(identifier) && identifier.length() >= 17) {
            return AuraUser.getByWith("discord_id", identifier);
        }
        return AuraUser.getByWith("player_name", identifier);
    }
}
