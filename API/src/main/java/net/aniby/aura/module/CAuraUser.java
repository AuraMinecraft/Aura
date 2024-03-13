package net.aniby.aura.module;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.aniby.aura.tool.AuraUtils;

@Getter
@Setter
@DatabaseTable(tableName = "users")
@NoArgsConstructor
@AllArgsConstructor
public class CAuraUser {


    // === User Only ===
    // Main
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
}
