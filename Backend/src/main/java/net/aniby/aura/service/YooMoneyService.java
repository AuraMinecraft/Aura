package net.aniby.aura.service;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.entity.AuraDonate;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.repository.DonateRepository;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.tool.AuraCache;
import net.aniby.yoomoney.client.YooMoneyClient;
import net.aniby.yoomoney.modules.notifications.IncomingNotification;
import ninja.leaping.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class YooMoneyService {
    AuraCache yooMoneyCache;

    YooMoneyClient client;
    String notificationSecret;

    UserRepository userRepository;
    DonateRepository donateRepository;
    DiscordLoggerService loggerService;

    UserService userService;
    AuraConfig config;

    @Autowired
    @SneakyThrows
    public YooMoneyService(AuraConfig config, AuraCache yooMoneyCache, DiscordLoggerService loggerService, UserRepository userRepository, DonateRepository donateRepository, UserService userService) {
        this.userService = userService;
        this.config = config;
        this.loggerService = loggerService;

        ConfigurationNode node = config.getRoot().getNode("donation", "yoomoney");
        this.notificationSecret = node.getNode("notification_secret").getString();
        this.client = new YooMoneyClient(
                node.getNode("client_id").getString(),
                node.getNode("client_secret").getString()
        );

        this.yooMoneyCache = yooMoneyCache;
        this.yooMoneyCache.load();

        this.userRepository = userRepository;
        this.donateRepository = donateRepository;
    }

    public void processNotification(Map<String, String> body) throws IllegalAccessException, IOException {
        IncomingNotification notification = IncomingNotification.get(body);

        if (!yooMoneyCache.cache(notification.operationId))
            return;

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

        createDonate(discordId, notification.getWithdrawAmount(), notification.getAmount(), timestamp);
    }

    public AuraDonate createDonate(@NotNull String discordId, double amount, double realAmount, long timestamp) {
        AuraUser user = userService.getByWith("discord_id", discordId);

        String promo = user.getPromoDiscordId();
        AuraUser streamer = promo != null ? userService.getByWith("discord_id", promo) : null;
        boolean isStreamer = streamer != null && userService.isStreamer(streamer);

        ConfigurationNode node = config.getRoot().getNode("aura");
        double auraCost = node.getNode("per_ruble").getDouble();

        double aura = amount / auraCost;
        double returnsRubles = 0.0;
        if (isStreamer) {
            aura *= node.getNode("donate_promo_multiplier").getDouble();
            returnsRubles = realAmount / auraCost * node.getNode("donate_promo_returns").getDouble();
        }
        userService.addAura(user, aura, streamer);
        userRepository.update(user);

        AuraDonate donate = new AuraDonate(0, user, amount, aura, timestamp);
        donateRepository.update(donate);

        loggerService.donate(donate, aura, returnsRubles);
        return donate;
    }

    public String createURL(String discordId, double amount) throws IOException {
        String label = "discord:" + discordId;
        return client.createQuickPayForm(amount, label);
    }
}
