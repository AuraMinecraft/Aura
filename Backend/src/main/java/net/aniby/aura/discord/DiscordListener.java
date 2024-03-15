package net.aniby.aura.discord;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.entity.AuraUser;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DiscordListener extends ListenerAdapter {
    UserService userService;
    DiscordService discordService;

    public DiscordListener(
            @Lazy UserService userService,
            @Lazy DiscordService discordService
            ) {
        this.userService = userService;
        this.discordService = discordService;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        discordService.getHandler().executeDiscord(event);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        discordService.getHandler().executeButton(event);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        discordService.getHandler().executeModal(event);
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
