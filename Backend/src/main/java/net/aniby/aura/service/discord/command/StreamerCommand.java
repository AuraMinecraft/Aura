package net.aniby.aura.service.discord.command;

import lombok.experimental.FieldDefaults;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.service.discord.DiscordIRC;
import net.aniby.aura.service.twitch.TwitchIRC;
import net.aniby.aura.service.user.UserService;
import net.aniby.aura.tool.Replacer;
import net.aniby.aura.util.AuraConfig;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@FieldDefaults(makeFinal = true)
public class StreamerCommand implements ACommand {
    AuraConfig config;
    UserService userService;
    UserRepository userRepository;
    DiscordIRC discordIRC;
    TwitchIRC twitchIRC;

    public StreamerCommand(@Lazy TwitchIRC twitchIRC, AuraConfig config, @Lazy UserService userService, UserRepository userRepository, @Lazy DiscordIRC discordIRC) {
        this.config = config;
        this.userService = userService;
        this.userRepository = userRepository;
        this.discordIRC = discordIRC;
        this.twitchIRC = twitchIRC;
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
            event.getHook().editOriginal(config.getMessage("player_name_not_found"))
                    .queue();
        }

        // Discord
        boolean contains;
        try {
            Role addRole = discordIRC.getRoles().get("streamer");
            Member member = userService.getGuildMember(user);

            if (member == null)
                throw new RuntimeException();

            contains = member.getRoles().contains(addRole);
            if (!contains) {
                discordIRC.getDefaultGuild().addRoleToMember(member, addRole).queue();
                twitchIRC.listenAsStreamer(user);
            }
            else {
                discordIRC.getDefaultGuild().removeRoleFromMember(member, addRole).queue();
                twitchIRC.unregisterStreamer(user);
            }
        } catch (Exception ignored) {
            event.getHook().editOriginal(
                    config.getMessage("something_went_wrong")
            ).queue();
            return;
        }

        event.getHook().editOriginal(
                        config.getMessage(
                                contains ? "streamer_removed" : "streamer_added",
                                userService.getReplacers(user)
                        )
                )
                .queue();
        return;
    }

    @Override
    public SlashCommandData slashCommandData() {
        return Commands.slash("streamer", "Изменение состояния в белом списке")
                .addOption(OptionType.STRING, "identifier", "Идентификатор пользователя", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(
                        Permission.ADMINISTRATOR
                )).setGuildOnly(true);
    }
}