package net.aniby.aura.discord;

import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.service.DiscordCommandService;
import net.aniby.aura.service.DiscordService;
import net.aniby.aura.service.UserService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscordListener extends ListenerAdapter {
    @Autowired
    UserService userService;
    @Autowired
    DiscordService discordService;
    @Autowired
    DiscordCommandService discordCommandService;

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        discordCommandService.getHandler().executeDiscord(event);
    }
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        discordCommandService.getHandler().executeButton(event);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        discordCommandService.getHandler().executeModal(event);
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        if (!guild.equals(discordService.getDefaultGuild()))
            return;

        User user = event.getUser();
        AuraUser auraUser = userService.getByWith("discord_id", user.getId());
        if (auraUser == null)
            return;

        Member member = event.getMember();
        try {
            member.modifyNickname(auraUser.getPlayerName()).queue();
            if (auraUser.getTwitchId() != null)
                guild.addRoleToMember(user, discordService.getRoles().get("twitch")).queue();
        } catch (Exception exception) {
//            AuraBackend.getLogger().info(
//                    "Can't modify guild member. User: @" + event.getUser().getName() + ", nickname: " + auraUser.getPlayerName()
//            );
        }
    }
}
