package net.aniby.aura.service.donate;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.util.AuraConfig;
import net.aniby.aura.entity.AuraDonate;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.repository.DonateRepository;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.service.user.UserService;
import net.aniby.aura.service.discord.DiscordLogger;
import net.aniby.aura.tool.AuraCache;
import net.aniby.yoomoney.client.YooMoneyClient;
import net.aniby.yoomoney.modules.forms.QuickPay;
import net.aniby.yoomoney.modules.notifications.IncomingNotification;
import ninja.leaping.configurate.ConfigurationNode;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DonateService {
    AuraCache yooMoneyCache;

    @Getter
    YooMoneyClient client;
    @Getter
    String yooMoneyAccountNumber;
    String notificationSecret;

    UserRepository userRepository;
    DonateRepository donateRepository;
    DiscordLogger loggerService;

    UserService userService;
    AuraConfig config;

    @Autowired
    @SneakyThrows
    public DonateService(AuraConfig config, AuraCache yooMoneyCache, DiscordLogger loggerService, UserRepository userRepository, DonateRepository donateRepository, @Lazy UserService userService) {
        this.userService = userService;
        this.config = config;
        this.loggerService = loggerService;

        ConfigurationNode node = config.getRoot().getNode("donation", "yoomoney");
        this.notificationSecret = node.getNode("notification_secret").getString();
        this.client = new YooMoneyClient(
                node.getNode("client_id").getString(),
                node.getNode("access_token").getString()
        );
        this.yooMoneyAccountNumber = this.client.getAccountInfo().getAccount();

        this.yooMoneyCache = yooMoneyCache;
        this.yooMoneyCache.load();

        this.userRepository = userRepository;
        this.donateRepository = donateRepository;
    }

    public void processNotification(Map<String, String> body) throws IllegalAccessException, IOException {
        IncomingNotification notification = IncomingNotification.get(body);

        if (notification.isTestNotification())
            return;
        if (!notification.verify(this.notificationSecret))
            return;

        String label = notification.getLabel();
        if (label == null)
            return;
        if (!label.startsWith("discord:"))
            return;

        if (!yooMoneyCache.cache(notification.operationId))
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

    public void processRest(HttpServletResponse response, String method, String discord, double amount) throws IOException {
        switch (method) {
            case "yoomoney":
                String url = client.createQuickPayForm(
                        new QuickPay(
                                this.yooMoneyAccountNumber,
                                amount,
                                QuickPay.PAYMENT_TYPES,
                                "discord:" + discord
                        )
                );
                response.sendRedirect(url);
                return;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
}
