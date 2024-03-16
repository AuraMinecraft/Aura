package net.aniby.aura.service.discord.commands;

import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.service.user.UserService;
import net.aniby.aura.service.donate.DonateService;
import net.aniby.aura.tool.Replacer;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@FieldDefaults(makeFinal = true)
public class DonateCommand implements ACommand {
    AuraConfig config;
    UserService userService;
    UserRepository userRepository;
    DonateService donateService;

    public DonateCommand(AuraConfig config, UserRepository userRepository, DonateService donateService, @Lazy UserService userService) {
        this.config = config;
        this.userRepository = userRepository;
        this.userService = userService;
        this.donateService = donateService;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        User user = event.getUser();
        if (user.isBot()) {
            event.getHook().editOriginal(config.getMessage("invalid_executor")).queue();
            return;
        }

        String discordId = user.getId();
        AuraUser auraUser = userService.getByWith("discord_id", discordId);
        if (auraUser == null || auraUser.getTwitchId() == null) {
            event.getHook().editOriginal(config.getMessage("need_linked_twitch")).queue();
            return;
        }

        List<Replacer> replacerList = userService.getReplacers(auraUser);

        OptionMapping promoMapping = event.getOption("promo_user");
        if (promoMapping != null) {
            String identifier = promoMapping.getAsString();
            if (identifier.startsWith("http") || identifier.startsWith("www"))
                identifier = identifier
                        .replace("http://", "")
                        .replace("https://", "");
            if (identifier.startsWith("www") || identifier.startsWith("m"))
                identifier = identifier
                        .replace("www.", "")
                        .replace("m.", "");

            if (identifier.startsWith("twitch/") || identifier.startsWith("t/") || identifier.startsWith("twitch.tv/")) {
                String twitchName = identifier.split("/", 2)[1];
                AuraUser inviter = userService.getByWith("twitch_name", twitchName);

                if (inviter == null || !userService.isStreamer(inviter)) {
                    event.getHook().editOriginal(
                            config.getMessage("not_streamer", replacerList)
                    ).queue();
                    return;
                }

                if (auraUser.getPromoDiscordId() != null) {
                    event.getHook().editOriginal(
                            config.getMessage("already_used_promo", replacerList)
                    ).queue();
                    return;
                }

                auraUser.setPromoDiscordId(inviter.getDiscordId());
                userRepository.update(auraUser);
            }
        }

        double amount = event.getOption("amount").getAsDouble();
        OptionMapping paymentTypeMapping = event.getOption("payment_method");
        String paymentType = paymentTypeMapping != null ? paymentTypeMapping.getAsString() : "yoomoney";

        String url = config.getRoot().getNode("http_server", "external_url").getString();
        url += "donate/?method=" + paymentType + "&amount=" + amount + "&discord=" + user.getId();

        event.getHook().editOriginal(
                config.getMessage("donate_" + paymentType, replacerList)
        ).setActionRow(
                Button.link(url, "Оплатить")
        ).queue();
    }

    @Override
    public SlashCommandData slashCommandData() {
        return Commands.slash("donate", "Отображает количество ауры пользователя")
                .addOption(OptionType.NUMBER, "amount", "Количество в рублях", true, false)
                .addOption(OptionType.STRING, "promo_user", "Ссылка на канал Twitch стримера-пригласителя", false)
                .addOptions(new OptionData(OptionType.STRING, "payment_method", "Способ оплаты", false)
                        .addChoice("ЮMoney", "yoomoney")
//                        .addChoice("DonationAlerts", "donationalerts")
                )
                .setGuildOnly(false);
    }
}