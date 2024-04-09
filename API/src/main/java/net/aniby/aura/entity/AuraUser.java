package net.aniby.aura.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.*;
import net.aniby.aura.tool.AuraUtils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

@Data
@DatabaseTable(tableName = "users")
@NoArgsConstructor
@AllArgsConstructor
public class AuraUser {
    @DatabaseField(generatedId = true)
    private int id = 0;

    @DatabaseField(columnName = "player_name", uniqueIndex = true)
    private String playerName = null;

    @DatabaseField(columnName = "twitch_id", uniqueIndex = true)
    private String twitchId = null;

    @DatabaseField(columnName = "twitch_name")
    private String twitchName = null;

    @DatabaseField(columnName = "refresh_token")
    private String refreshToken = null;

    @DatabaseField(columnName = "discord_id", uniqueIndex = true)
    private String discordId = null;

    @DatabaseField(canBeNull = false, defaultValue = "0.0")
    private double aura = 0.0;

    @DatabaseField(canBeNull = false, defaultValue = "0")
    private boolean whitelisted = false;

    @DatabaseField(columnName = "promo_discord_id")
    private String promoDiscordId = null;

    public double getFormattedAura() {
        return AuraUtils.roundDouble(aura);
    }

    public static Comparator<AuraUser> comparingByAura() {
        return (Comparator<AuraUser> & Serializable)
                (c1, c2) -> Double.compare(c1.getAura(), c2.getAura());
    }
}
