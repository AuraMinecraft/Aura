package net.aniby.aura.discord.commands;

import net.aniby.aura.AuraBackend;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.modules.AuraUser;
import net.aniby.aura.AuraConfig;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class LinkCommand implements ACommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();
        // Init variables
        User user = event.getUser();
        AuraConfig config = AuraBackend.getConfig();

        // Is bot
        if (user.isBot()) {
            event.getHook().editOriginal(config.getMessage("invalid_executor")).queue();
            return;
        }

        // Check in guild
        try {
            AuraBackend.getDiscord().getDefaultGuild().retrieveMember(user).complete();
        } catch (ErrorResponseException exception) {
            event.getHook().editOriginal(
                    config.getMessage("not_in_guild")
            ).queue();
            return;
        }

        String userId = user.getId();
        AuraUser CAuraUser = AuraUser.getByWith("discord_id", userId);

        if (CAuraUser != null && CAuraUser.getTwitchId() != null) { // if already linked twitch
            event.getHook().editOriginal(
                    config.getMessage("lc_twitch_already_linked")
            ).queue();
            return;
        }

        String url = AuraBackend.getTwitch().generateTwitchLink(userId);
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