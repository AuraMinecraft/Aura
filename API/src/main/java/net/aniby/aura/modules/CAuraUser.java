package net.aniby.aura.modules;

import com.github.twitch4j.pubsub.PubSubSubscription;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import net.aniby.aura.AuraAPI;
import net.aniby.aura.tools.AuraUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@DatabaseTable(tableName = "users")
@NoArgsConstructor
public class CAuraUser {
    public void transferFrom(CAuraUser user) {
        if (user.playerName != null)
            this.playerName = user.playerName;
        this.aura += user.aura;
        if (user.whitelisted)
            this.whitelisted = true;

        this.discordId = user.discordId;

        this.accessToken = user.accessToken;
        this.refreshToken = user.refreshToken;
        this.expiresAt = user.expiresAt;
        this.twitchName = user.twitchName;
    }

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

    private String accessToken = null;

    private long expiresAt = 0; // seconds

    @DatabaseField(canBeNull = false, defaultValue = "0.0")
    private double aura = 0.0;

    @DatabaseField(canBeNull = false, defaultValue = "0")
    private boolean whitelisted = false;

    @DatabaseField(columnName = "promo_discord_id")
    private String promoDiscordId = null;

    public double getFormattedAura() {
        return AuraUtils.roundDouble(aura);
    }

    // Constructor
    private final List<PubSubSubscription> subscriptions = new ArrayList<>();

    public CAuraUser(String playerName,
              String discordId,
              String twitchId,
              String refreshToken,
              double aura,
              boolean whitelisted,
              String promoDiscordId) {
        this.playerName = playerName;
        this.discordId = discordId;
        this.twitchId = twitchId;
        this.refreshToken = refreshToken;
        this.aura = aura;
        this.whitelisted = whitelisted;
        this.promoDiscordId = promoDiscordId;
    }

    CAuraUser(String playerName,
              String discordId,
              String twitchName,
              String twitchId,
              String accessToken,
              String refreshToken,
              long expiresAt,
              double aura,
              boolean whitelisted,
              String promoDiscordId) {
        this.playerName = playerName;
        this.discordId = discordId;
        this.twitchId = twitchId;
        this.twitchName = twitchName;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.aura = aura;
        this.whitelisted = whitelisted;
        this.promoDiscordId = promoDiscordId;
    }


    // Methods
    @SneakyThrows
    public int delete() {
        return AuraAPI.getDatabase().getUsers().delete(this);
    }

    @SneakyThrows
    public void save() {
        if (this.id > 0)
            AuraAPI.getDatabase().getUsers().update(this);
        else
            AuraAPI.getDatabase().getUsers().create(this);
    }
}
