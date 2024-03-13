package net.aniby.aura.discord.commands;

import net.aniby.aura.AuraBackend;
import net.aniby.aura.discord.ACommand;
import net.aniby.aura.modules.AuraUser;
import net.aniby.aura.tool.Replacer;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public class AuraCommand implements ACommand {
    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue();

        User user = event.getUser();
        if (user.isBot()) {
            event.getHook().editOriginal(AuraBackend.getConfig().getMessage("invalid_executor")).queue();
            return;
        }

        AuraUser CAuraUser = AuraUser.getByWith("discord_id", user.getId());
        double aura = CAuraUser == null ? 0 : CAuraUser.getFormattedAura();

        String message = AuraBackend.getConfig().getMessage("aura_command", Replacer.r("aura", String.valueOf(aura)));
        event.getHook().editOriginal(message).queue();
    }

    @Override
    public SlashCommandData slashCommandData() {
        return Commands.slash("aura", "Отображает количество ауры пользователя");
    }
}