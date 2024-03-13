package net.aniby.aura.discord.commands;

import net.aniby.aura.AuraBackend;
import net.aniby.aura.modules.AuraUser;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.tool.Replacer;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.List;
import java.util.Objects;

public class DonateCommand implements ACommand {
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        User user = event.getUser();
        if (user.isBot()) {
            event.getHook().editOriginal(AuraBackend.getConfig().getMessage("invalid_executor")).queue();
            return;
        }
        AuraConfig config = AuraBackend.getConfig();

        String discordId = user.getId();
        AuraUser auraUser = AuraUser.getByWith("discord_id", discordId);
        if (auraUser == null || auraUser.getRefreshToken() == null) {
            event.getHook().editOriginal(config.getMessage("need_linked_twitch")).queue();
            return;
        }

        List<Replacer> replacerList = auraUser.getReplacers();

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
                AuraUser inviter = AuraUser.getByWith("twitch_name", twitchName);

                if (inviter == null || !inviter.isStreamer()) {
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
                auraUser.save();
            }
        }

        double amount = event.getOption("amount").getAsDouble();
        OptionMapping paymentTypeMapping = event.getOption("payment_type");
        String paymentType = paymentTypeMapping != null ? paymentTypeMapping.getAsString() : "yoomoney";

        String url;
        try {
            url = Objects.equals(paymentType, "yoomoney")
                    ? AuraBackend.getDonation().createYooMoneyDonateURL(user.getId(), amount)
                    : config.getRoot().getNode("donation", "donationalerts", "url").getString();
            if (url == null || url.isEmpty())
                throw new RuntimeException("DonationAlerts URL is empty!");
        } catch (Exception e) {
            e.printStackTrace();
            event.getHook().editOriginal(config.getMessage("something_went_wrong", replacerList)).queue();
            return;
        }

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
                .addOptions(new OptionData(OptionType.STRING, "payment_type", "Способ оплаты", false)
                        .addChoice("ЮMoney", "yoomoney")
//                        .addChoice("DonationAlerts", "donationalerts")
                )
                .addOption(OptionType.STRING, "promo_user", "Ссылка на канал Twitch стримера-пригласителя", false)
                .setGuildOnly(false);
    }
}