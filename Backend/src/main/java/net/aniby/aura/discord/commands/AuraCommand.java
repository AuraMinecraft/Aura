package net.aniby.aura.discord.commands;

import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.service.UserService;
import net.aniby.aura.tool.Replacer;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(makeFinal = true)
public class AuraCommand implements ACommand {
    AuraConfig config;
    UserService userService;

    public AuraCommand(AuraConfig config, @Lazy UserService userService) {
        this.config = config;
        this.userService = userService;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        User user = event.getUser();
        if (user.isBot()) {
            event.getHook().editOriginal(config.getMessage("invalid_executor")).queue();
            return;
        }

        AuraUser CAuraUser = userService.getByWith("discord_id", user.getId());
        double aura = CAuraUser == null ? 0 : CAuraUser.getFormattedAura();

        String message = config.getMessage("aura_command", Replacer.r("aura", String.valueOf(aura)));
        event.getHook().editOriginal(message).queue();
    }

    @Override
    public SlashCommandData slashCommandData() {
        return Commands.slash("aura", "Отображает количество ауры пользователя");
    }
}