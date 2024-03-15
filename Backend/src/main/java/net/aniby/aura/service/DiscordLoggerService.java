package net.aniby.aura.service;

import lombok.RequiredArgsConstructor;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.entity.AuraDonate;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.repository.DonateRepository;
import net.aniby.aura.tool.AuraUtils;
import net.aniby.aura.tool.Replacer;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DiscordLoggerService {
    AuraConfig config;
    UserService userService;
    DonateRepository donateRepository;
    DiscordService discordService;

    TextChannel getChannel() {
        return discordService.getChannels().get("logs");
    }

    public void viewerEarnedAura(String displayName, AuraUser user, double amount, String streamerName, double streamerAmount) {
        List<Replacer> replacerList = new ArrayList<>(userService.getReplacers(user));
        replacerList.add(Replacer.r("earned_aura", AuraUtils.roundDouble(amount)));
        replacerList.add(Replacer.r("streamer_name", streamerName));
        replacerList.add(Replacer.r("streamer_earned_aura", AuraUtils.roundDouble(streamerAmount)));
        replacerList.add(Replacer.r("viewer_name", displayName));

        try {
            getChannel().sendMessageEmbeds(config.getEmbed("earned_aura", replacerList)).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void auraReject(String displayName, AuraUser user, AuraUser streamer, double userAmount, double streamerAmount) {
        List<Replacer> replacerList = new ArrayList<>(userService.getReplacers(user));
        replacerList.add(Replacer.r("rejected_aura", AuraUtils.roundDouble(userAmount)));
        replacerList.add(Replacer.r("rejected_streamer_aura", AuraUtils.roundDouble(streamerAmount)));
        replacerList.add(Replacer.r("streamer_name", streamer.getTwitchName()));
        replacerList.add(Replacer.r("viewer_name", displayName));

        try {
            getChannel().sendMessageEmbeds(config.getEmbed("aura_reject", replacerList)).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void donate(AuraDonate donate, double earnedAura, double earnedRubles) {
        AuraUser user = donate.getUser();
        List<Replacer> replacerList = new ArrayList<>(donateRepository.getReplacers(donate));
        AuraUser streamer = null;
        String ser = "-";
        if (user != null) {
            replacerList.addAll(userService.getReplacers(user));
            if (user.getPromoDiscordId() != null) {
                streamer = userService.getByWith("discord_id", user.getPromoDiscordId());
                assert streamer != null;
                replacerList.add(Replacer.r("streamer_name", streamer.getTwitchName()));
                if (earnedRubles > 0)
                    ser = String.valueOf(AuraUtils.roundDouble(earnedRubles));
            }
        }
        replacerList.add(Replacer.r("earned_rubles", ser));
        replacerList.add(Replacer.r("earned_aura", AuraUtils.roundDouble(earnedAura)));

        try {
            getChannel().sendMessageEmbeds(config.getEmbed("donate", replacerList)).queue();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            User donated = userService.getDiscordUser(user);
            if (donated != null)
                donated.openPrivateChannel().flatMap(channel ->
                        channel.sendMessage(config.getMessage("donate_notify", replacerList))
                ).queue();
        } catch (Exception ignored) {
        }
        try {
            if (streamer != null) {
                User referral = userService.getDiscordUser(streamer);
                if (referral != null)
                    referral.openPrivateChannel().flatMap(channel ->
                            channel.sendMessage(config.getMessage("donate_notify_referral", replacerList))
                    ).queue();
            }
        } catch (Exception ignored) {
        }
    }
}
