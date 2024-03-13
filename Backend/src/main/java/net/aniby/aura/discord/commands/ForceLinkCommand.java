package net.aniby.aura.discord.commands;

import net.aniby.aura.AuraBackend;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.BackendTools;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.modules.AuraUser;
import net.aniby.aura.tool.Replacer;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;
import java.util.Locale;

public class ForceLinkCommand implements ACommand {
    public boolean hasPermission(SlashCommandInteractionEvent event) {
        // Init variables
        User source = event.getUser();

        // Is bot
        if (source.isBot()) {
            return false;
        }

        // Check in guild
        Guild guild = AuraBackend.getDiscord().getDefaultGuild();
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
        event.deferReply(true).queue();

        User source = event.getUser();
        AuraConfig config = AuraBackend.getConfig();

        if (!hasPermission(event)) {
            event.getHook().editOriginal(
                    config.getMessage("no_permission")
            ).queue();
            return;
        }

        String identifier = event.getOption("identifier").getAsString(),
                social = event.getOption("social").getAsString(),
                value = event.getOption("value").getAsString();

        AuraUser user = BackendTools.extractBySocialSelector(identifier);

        if (user == null) {
            event.getHook().editOriginal(config.getMessage("user_not_found"))
                    .queue();
            return;
        }

        switch (social) {
            case "minecraft" -> {
                user.setPlayerName(value);
                user.save();
            }
            case "discord" -> {
                user.setDiscordId(value);
                user.save();
            }
        }

        social = social.toLowerCase(Locale.ROOT);
        String formattedSocial = social.substring(0, 1).toUpperCase() + social.substring(1);

        List<Replacer> tags = user.getReplacers();
        tags.addAll(List.of(
                Replacer.r("social", formattedSocial),
                Replacer.r("admin_name", source.getName())
        ));
        event.getHook().editOriginal(config.getMessage("forcelink_success", tags))
                .queue();
    }


    @Override
    public SlashCommandData slashCommandData() {
        return Commands.slash("forcelink", "Привязывает сеть в AuraLink")
                .addOption(OptionType.STRING, "identifier", "Индентификатор игрока", true, false)
                .addOption(OptionType.STRING, "value", "Параметр, который нужно привязать", true, false)
                .addOptions(new OptionData(OptionType.STRING, "social", "Тип параметра", true)
                        .addChoice("Discord ID", "discord")
                        .addChoice("Никнейм Minecraft", "minecraft")
                )
                .setDefaultPermissions(
                        DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)
                )
                .setGuildOnly(true);
    }
}