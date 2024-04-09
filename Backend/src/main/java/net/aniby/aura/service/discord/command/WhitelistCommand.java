package net.aniby.aura.service.discord.command;

import io.graversen.minecraft.rcon.service.MinecraftRconService;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.service.discord.DiscordIRC;
import net.aniby.aura.service.user.UserService;
import net.aniby.aura.tool.Replacer;
import net.aniby.aura.util.AuraConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@FieldDefaults(makeFinal = true)
public class WhitelistCommand implements ACommand {
    AuraConfig config;
    UserService userService;
    UserRepository userRepository;
    DiscordIRC discordIRC;

    public WhitelistCommand(AuraConfig config, @Lazy UserService userService, UserRepository userRepository, @Lazy DiscordIRC discordIRC) {
        this.config = config;
        this.userService = userService;
        this.userRepository = userRepository;
        this.discordIRC = discordIRC;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        if (!discordIRC.hasDefaultPermission(event))
            return;

        String identifier = event.getOption("identifier").getAsString();
        AuraUser user = userService.extractBySocialSelector(identifier);
        if (user == null) {
            event.getHook().editOriginal(config.getMessage("user_not_found"))
                    .queue();
            return;
        }

        String selectorName = userService.extractNameBySocialSelector(identifier);
        assert selectorName != null;
        List<Replacer> resolvers = userService.getReplacers(user);
        resolvers.add(Replacer.r("selector_name", selectorName));

        if (user.getPlayerName() == null) {
            OptionMapping optionMapping = event.getOption("player_name");
            if (optionMapping != null)
                user.setPlayerName(optionMapping.getAsString());
            else {
                event.getHook().editOriginal(config.getMessage("player_name_not_found"))
                        .queue();
                return;
            }
        }

        // Database
        boolean whitelist = !user.isWhitelisted();
        userService.setWhitelist(user, whitelist);
        userRepository.update(user);

        // Discord
        try {
            Role addRole = discordIRC.getRoles().get("player");
            User discordUser = userService.getDiscordUser(user);
            assert discordUser != null;
            discordIRC.getDefaultGuild().addRoleToMember(discordUser, addRole).queue();
        } catch (Exception ignored) {}



        event.getHook().editOriginal(
                        config.getMessage(
                                whitelist ? "profile_whitelist_added" : "profile_whitelist_removed",
                                userService.getReplacers(user)
                        )
                )
                .queue();
        return;
    }

    @Override
    public SlashCommandData slashCommandData() {
        return Commands.slash("whitelist", "Изменение состояния в белом списке")
                .addOption(OptionType.STRING, "identifier", "Идентификатор пользователя", true)
                .addOption(OptionType.STRING, "player_name", "Никнейм Minecraft", false)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(
                        Permission.BAN_MEMBERS
                )).setGuildOnly(true);
    }
}