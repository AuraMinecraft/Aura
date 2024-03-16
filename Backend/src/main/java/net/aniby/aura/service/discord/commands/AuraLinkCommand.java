package net.aniby.aura.service.discord.commands;

import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.service.discord.DiscordIRC;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(makeFinal = true)
public class AuraLinkCommand implements ACommand {
    AuraConfig config;
    DiscordIRC discordIRC;

    public AuraLinkCommand(AuraConfig config, @Lazy DiscordIRC discordIRC) {
        this.config = config;
        this.discordIRC = discordIRC;
    }

    public void execute(@Nullable String argument) {
        try {
            if (argument != null) {
                switch (argument) {
                    case "config" -> config.load();
                }
            } else {
                config.load();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public boolean hasPermission(SlashCommandInteractionEvent event) {
        // Init variables
        User source = event.getUser();

        // Is bot
        if (source.isBot()) {
            return false;
        }

        // Check in guild
        Guild guild = discordIRC.getDefaultGuild();
        Member member;
        try {
            member = guild.retrieveMember(source).complete();
        } catch (ErrorResponseException exception) {
            return false;
        }

        return member.getPermissions().contains(Permission.ADMINISTRATOR);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Init variables
        event.deferReply(true).queue();

        User source = event.getUser();
        if (!hasPermission(event)) {
            event.getHook().editOriginal(
                    config.getMessage("no_permission")
            ).queue();
            return;
        }

        OptionMapping option = event.getOption("type");
        String argument = option != null ? option.getAsString() : null;
        execute(argument);

        event.getHook().editOriginal(
                config.getMessage("reload_success")
        ).queue();
    }

    @Override
    public SlashCommandData slashCommandData() {
        return Commands.slash("auralink", "Перезагружает некоторые функции плагина")
                .addOptions(new OptionData(OptionType.STRING, "type", "Тип перезагрузки", false)
                        .addChoice("Перезагрузить конфиг", "config")
                )
                .setDefaultPermissions(
                        DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)
                )
                .setGuildOnly(true);
    }
}