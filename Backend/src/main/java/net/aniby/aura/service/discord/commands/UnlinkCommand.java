package net.aniby.aura.service.discord.commands;

import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.service.discord.DiscordIRC;
import net.aniby.aura.service.user.UserService;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

import static net.aniby.aura.tool.Replacer.r;

@Service
@FieldDefaults(makeFinal = true)
public class UnlinkCommand implements ACommand {
    AuraConfig config;
    UserService userService;
    UserRepository userRepository;
    DiscordIRC discordIRC;

    public UnlinkCommand(AuraConfig config, @Lazy UserService userService, UserRepository userRepository, @Lazy DiscordIRC discordIRC) {
        this.config = config;
        this.userService = userService;
        this.userRepository = userRepository;
        this.discordIRC = discordIRC;
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

        String identifier = event.getOption("identifier").getAsString(),
                social = event.getOption("social").getAsString();

        AuraUser user = userService.extractBySocialSelector(identifier);
        if (user == null) {
            event.getHook().editOriginal(config.getMessage("user_not_found"))
                    .queue();
            return;
        }

        switch (social) {
            case "minecraft" -> user.setPlayerName(null);
            case "discord" -> user.setDiscordId(null);
            case "twitch" -> {
                user.setRefreshToken(null);
                user.setTwitchId(null);
                user.setTwitchName(null);
            }
        }
        userRepository.update(user);

        social = social.toLowerCase(Locale.ROOT);
        String formattedSocial = social.substring(0, 1).toUpperCase() + social.substring(1);

        List<Replacer> tags = userService.getReplacers(user);

        tags.addAll(List.of(
                r("social", formattedSocial),
                r("admin_name", source.getName()),
                r("selector_name", userService.extractNameBySocialSelector(identifier))
        ));
        event.getHook().editOriginal(config.getMessage("unlink_success", tags))
                .queue();

//        Player player = user.getPlayer();
//        if (player != null) {
//            player.sendMessage(
//                    config.getComponentMessage("unlink_success_target", tags)
//            );
//        }
    }

    @Override
    public SlashCommandData slashCommandData() {
        return Commands.slash("unlink", "Отвязывает сеть в AuraLink")
                .addOption(OptionType.STRING, "identifier", "Индентификатор игрока", true, false)
                .addOptions(new OptionData(OptionType.STRING, "social", "Сеть, которую нужно отвязать", true)
                        .addChoice("Twitch", "twitch")
                        .addChoice("Discord", "discord")
                        .addChoice("Minecraft", "minecraft")
                )
                .setDefaultPermissions(
                        DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)
                )
                .setGuildOnly(true);
    }
}