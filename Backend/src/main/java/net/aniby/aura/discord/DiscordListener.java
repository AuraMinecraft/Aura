package net.aniby.aura.discord;

import net.aniby.aura.AuraAPI;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.modules.AuraUser;
import net.aniby.aura.modules.CAuraUser;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DiscordListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        AuraBackend.getHandler().executeDiscord(event);
    }
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        AuraBackend.getHandler().executeButton(event);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        AuraBackend.getHandler().executeModal(event);
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        if (!guild.equals(AuraBackend.getDiscord().getDefaultGuild()))
            return;

        User user = event.getUser();
        AuraUser CAuraUser = AuraUser.getByWith("discord_id", user.getId());
        if (CAuraUser == null)
            return;

        Member member = event.getMember();
        try {
            member.modifyNickname(CAuraUser.getPlayerName()).queue();
            if (CAuraUser.getTwitchId() != null)
                guild.addRoleToMember(user, AuraBackend.getDiscord().getRoles().get("twitch")).queue();
        } catch (Exception exception) {
            AuraBackend.getLogger().info(
                    "Can't modify guild member. User: @" + event.getUser().getName() + ", nickname: " + CAuraUser.getPlayerName()
            );
        }
    }
}
