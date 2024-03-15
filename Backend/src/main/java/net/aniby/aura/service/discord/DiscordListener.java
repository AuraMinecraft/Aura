package net.aniby.aura.service.discord;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.service.user.UserService;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DiscordListener extends ListenerAdapter {
    Logger logger = LoggerFactory.getLogger(DiscordListener.class);

    UserService userService;
    DiscordIRC discordIRC;

    public DiscordListener(
            @Lazy UserService userService,
            @Lazy DiscordIRC discordIRC
            ) {
        this.userService = userService;
        this.discordIRC = discordIRC;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        discordIRC.getHandler().executeDiscord(event);
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        discordIRC.getHandler().executeButton(event);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        discordIRC.getHandler().executeModal(event);
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        if (!guild.equals(discordIRC.getDefaultGuild()))
            return;

        User user = event.getUser();
        AuraUser auraUser = userService.getByWith("discord_id", user.getId());
        if (auraUser == null)
            return;

        Member member = event.getMember();
        try {
            member.modifyNickname(auraUser.getPlayerName()).queue();
            if (auraUser.getTwitchId() != null)
                guild.addRoleToMember(user, discordIRC.getRoles().get("twitch")).queue();
        } catch (Exception exception) {
            logger.info(
                    "Can't modify guild member. User: @" + event.getUser().getName() + ", nickname: " + auraUser.getPlayerName()
            );
        }
    }
}
