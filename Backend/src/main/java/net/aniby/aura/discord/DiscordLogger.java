package net.aniby.aura.discord;

import net.aniby.aura.AuraBackend;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.modules.AuraDonate;
import net.aniby.aura.modules.AuraUser;
import net.aniby.aura.modules.CAuraUser;
import net.aniby.aura.tools.AuraUtils;
import net.aniby.aura.tools.Replacer;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.List;

public class DiscordLogger {
    TextChannel getChannel() {
        return AuraBackend.getDiscord().channels.get("logs");
    }

    public void viewerEarnedAura(String displayName, AuraUser user, double amount, String streamerName, double streamerAmount) {
        AuraConfig config = AuraBackend.getConfig();

        List<Replacer> replacerList = new ArrayList<>(user.getReplacers());
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

    public void auraReject(String displayName, AuraUser user, CAuraUser streamer, double userAmount, double streamerAmount) {
        AuraConfig config = AuraBackend.getConfig();

        List<Replacer> replacerList = new ArrayList<>(user.getReplacers());
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
        AuraConfig config = AuraBackend.getConfig();

        AuraUser user = AuraUser.getByWith("discord_id", donate.getDiscordId());
        List<Replacer> replacerList = new ArrayList<>(donate.getReplacers());
        AuraUser streamer = null;
        String ser = "-";
        if (user != null) {
            replacerList.addAll(user.getReplacers());
            if (user.getPromoDiscordId() != null) {
                streamer = AuraUser.getByWith("discord_id", user.getPromoDiscordId());
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
            User donated = user.getDiscordUser();
            if (donated != null)
                donated.openPrivateChannel().flatMap(channel ->
                        channel.sendMessage(config.getMessage("donate_notify", replacerList))
                ).queue();
        } catch (Exception ignored) {
        }
        try {
            if (streamer != null) {
                User referral = streamer.getDiscordUser();
                if (referral != null)
                    referral.openPrivateChannel().flatMap(channel ->
                            channel.sendMessage(config.getMessage("donate_notify_referral", replacerList))
                    ).queue();
            }
        } catch (Exception ignored) {
        }
    }
}
