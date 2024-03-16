package net.aniby.aura.discord;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

public interface ACommand {
    void execute(SlashCommandInteractionEvent event);

    SlashCommandData slashCommandData();
}
