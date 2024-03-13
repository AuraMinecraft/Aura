package net.aniby.aura.discord.commands;

import net.aniby.aura.AuraBackend;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.BackendTools;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.modules.AuraUser;
import net.aniby.aura.tool.Replacer;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.List;

public class ProfileCommand implements ACommand {
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        AuraConfig config = AuraBackend.getConfig();

        if (!hasPermission(event))
            return;

        String identifier = event.getOption("identifier").getAsString();
        AuraUser user = BackendTools.extractBySocialSelector(identifier);
        if (user == null) {
            event.getHook().editOriginal(config.getMessage("user_not_found"))
                    .queue();
            return;
        }

        String selectorName = BackendTools.extractNameBySocialSelector(identifier);

        assert selectorName != null;
        List<Replacer> resolvers = user.getReplacers();
        resolvers.add(Replacer.r("selector_name", selectorName));

        String subcommandName = event.getSubcommandName();
        assert subcommandName != null;

        switch (subcommandName) {
            case "info" -> {
                event.getHook()
                        .editOriginal(config.getMessage("profile", resolvers))
                        .queue();
                return;
            }
            case "whitelist" -> {
                if (user.getPlayerName() == null) {
                    OptionMapping mapping = event.getOption("player_name");
                    if (mapping != null) {
                        String newName = mapping.getAsString();

                        if (newName.isEmpty()) {
                            event.getHook().editOriginal(config.getMessage("player_name_not_found")).queue();
                            return;
                        }

                        if (AuraUser.getByWith("player_name", newName) != null) {
                            event.getHook().editOriginal(config.getMessage("user_already_exists")).queue();
                            return;
                        }

                        user.setPlayerName(newName);
                    } else {
                        event.getHook().editOriginal(config.getMessage("unknown_action"))
                                .queue();
                        return;
                    }
                }

                boolean newValue = !user.isWhitelisted();
                user.setWhitelisted(newValue);
                user.save();

                event.getHook()
                        .editOriginal(config.getMessage(
                                "profile_whitelist_" + (newValue ? "added" : "removed")
                        ))
                        .queue();
                return;
            }
            default -> {
                event.getHook().editOriginal(config.getMessage("unknown_action"))
                        .queue();
                return;
            }
        }

    }

    @Override
    public SlashCommandData slashCommandData() {
        SubcommandData info = new SubcommandData("info", "Информация о профиле")
                .addOption(OptionType.STRING, "identifier", "Индентификатор игрока", true, false);
        SubcommandData whitelist = new SubcommandData("whitelist", "Переключить состояние белого списка")
                .addOption(OptionType.STRING, "identifier", "Индентификатор игрока", true, false)
                .addOption(OptionType.STRING, "player_name", "Никнейм игрока Minecraft", false, false);

        return Commands.slash("profile", "Управление аккаунтом пользователя")
                .addSubcommands(info, whitelist).setDefaultPermissions(DefaultMemberPermissions.enabledFor(
                        Permission.BAN_MEMBERS
                )).setGuildOnly(true);
    }

    public boolean hasPermission(SlashCommandInteractionEvent event) {
        // Is bot
        AuraConfig config = AuraBackend.getConfig();
        User user = event.getUser();
        if (user.isBot()) {
            event.getHook().editOriginal(config.getMessage("invalid_executor")).queue();
            return false;
        }

        // Check in guild
        try {
            AuraBackend.getDiscord().getDefaultGuild().retrieveMember(user).complete();
        } catch (ErrorResponseException exception) {
            event.getHook().editOriginal(
                    config.getMessage("not_in_guild")
            ).queue();
            return false;
        }
        return true;
    }
}