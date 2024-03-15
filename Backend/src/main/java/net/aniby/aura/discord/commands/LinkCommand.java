package net.aniby.aura.discord.commands;

import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.service.DiscordService;
import net.aniby.aura.service.UserService;
import net.aniby.aura.twitch.TwitchIRC;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(makeFinal = true)
public class LinkCommand implements ACommand {
    AuraConfig config;
    UserService userService;
    TwitchIRC twitchIRC;
    DiscordService discordService;

    public LinkCommand(AuraConfig config, @Lazy UserService userService, TwitchIRC twitchIRC, @Lazy DiscordService discordService) {
        this.config = config;
        this.userService = userService;
        this.twitchIRC = twitchIRC;
        this.discordService = discordService;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        // Init variables
        User user = event.getUser();

        // Is bot
        if (user.isBot()) {
            event.getHook().editOriginal(config.getMessage("invalid_executor")).queue();
            return;
        }

        // Check in guild
        try {
            discordService.getDefaultGuild().retrieveMember(user).complete();
        } catch (ErrorResponseException exception) {
            event.getHook().editOriginal(
                    config.getMessage("not_in_guild")
            ).queue();
            return;
        }

        String userId = user.getId();
        AuraUser CAuraUser = userService.getByWith("discord_id", userId);

        if (CAuraUser != null && CAuraUser.getTwitchId() != null) { // if already linked twitch
            event.getHook().editOriginal(
                    config.getMessage("lc_twitch_already_linked")
            ).queue();
            return;
        }

        String url = twitchIRC.generateTwitchLink(userId);
        event.getHook().editOriginal(
                config.getMessage("lc_twitch_link")
        ).setComponents(ActionRow.of(
                Button.link(url, config.getMessage("lc_twitch_link_button_label"))
        )).queue();
    }

    @Override
    public SlashCommandData slashCommandData() {
        return Commands.slash("link", "Привязывает аккаунт Twitch к AuraLink");
    }
}