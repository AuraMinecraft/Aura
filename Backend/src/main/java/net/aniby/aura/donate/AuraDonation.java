package net.aniby.aura.donate;

import lombok.Getter;
import net.aniby.aura.AuraAPI;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.module.AuraDonate;
import net.aniby.aura.modules.AuraUser;
import net.aniby.aura.tool.AuraCache;
import net.aniby.aura.tool.AuraUtils;
import net.aniby.yoomoney.client.YooMoneyClient;
import net.aniby.yoomoney.modules.notifications.IncomingNotification;
import ninja.leaping.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Getter
public class AuraDonation {
    final YooMoneyClient yooMoney;
    final AuraCache yooMoneyCache;
    final String notificationSecret;
    final static Map<String, Double> currencyMap = new HashMap<>();



    public AuraDonation() throws IOException {
        // Config init
        ConfigurationNode root = AuraBackend.getConfig().getRoot().getNode("donation");

        // Currency map
        ConfigurationNode currencyNode = root.getNode("currency_map");
        for (String key : AuraConfig.getNodeKeys(currencyNode)) {
            double value = currencyNode.getNode(key).getDouble();
            currencyMap.put(key.toUpperCase(Locale.ROOT), value);
        }

        // Cache
        File cacheFile = new File("cache/yoomoney.cache");
        AuraUtils.saveDefaultFile(cacheFile, "cache/yoomoney.cache", AuraAPI.class);
        yooMoneyCache = new AuraCache(cacheFile);
        yooMoneyCache.load();

        // YooMoney client
        ConfigurationNode ymNode = root.getNode("yoomoney");
        yooMoney = new YooMoneyClient(
                ymNode.getNode("client_id").getString(),
                ymNode.getNode("access_token").getString()
        );
        notificationSecret = ymNode.getNode("notification_secret").getString();
    }

    public String createYooMoneyDonateURL(String discordId, double amount) throws IOException {
        String label = "discord:" + discordId;
        return yooMoney.createQuickPayForm(amount, label);
    }

    public void onYooMoneyNotification(IncomingNotification notification) {
        try {
            if (!yooMoneyCache.cache(notification.operationId))
                return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (notification.isTestNotification())
            return;
        if (!notification.verify(this.notificationSecret))
            return;

        String label = notification.getLabel();
        if (label == null)
            return;
        if (!label.startsWith("discord:"))
            return;

        String discordId = label.split(":", 2)[1];
        long timestamp = new Date().getTime();

        create(discordId, notification.getWithdrawAmount(), notification.getAmount(), timestamp);
    }

    public static AuraDonate create(@NotNull String discordId, double amount, double realAmount, long timestamp) {
        AuraUser user = AuraUser.getByWith("discord_id", discordId);

        String promo = user.getPromoDiscordId();
        AuraUser streamer = promo != null ? AuraUser.getByWith("discord_id", promo) : null;
        boolean isStreamer = streamer != null && streamer.isStreamer();

        ConfigurationNode node = AuraBackend.getConfig().getRoot().getNode("aura");
        double auraCost = node.getNode("per_ruble").getDouble();

        double aura = amount / auraCost;
        double returnsRubles = 0.0;
        if (isStreamer) {
            aura *= node.getNode("donate_promo_multiplier").getDouble();
            returnsRubles = realAmount / auraCost * node.getNode("donate_promo_returns").getDouble();
        }
        user.addAura(aura, streamer);
        user.save();

        AuraDonate donate = new AuraDonate(0, discordId, amount, aura, timestamp);
        donate.save();

        AuraBackend.getDiscord().getLogger().donate(donate, aura, returnsRubles);
        return donate;
    }
}
