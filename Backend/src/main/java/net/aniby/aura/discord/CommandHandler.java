package net.aniby.aura.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class CommandHandler {
    CommandListUpdateAction discordCommands;
    final List<ACommand> commands = new ArrayList<>();
    final HashMap<String, Consumer<ButtonInteractionEvent>> buttons = new HashMap<>();
    final HashMap<String, Consumer<ModalInteractionEvent>> modals = new HashMap<>();

    public CommandHandler(JDA jda) {
        this.discordCommands = jda.updateCommands();
    }

    @SafeVarargs
    public final <T extends ACommand> void registerCommands(T... inputCommands) {
        for (T command : inputCommands) {
            commands.add(command);
            if (command.slashCommandData() != null)
                this.discordCommands = this.discordCommands.addCommands(command.slashCommandData());
        }
    }
    public void registerButton(String name, Consumer<ButtonInteractionEvent> consumer) {
        buttons.put(name, consumer);
    }
    public void registerModal(String name, Consumer<ModalInteractionEvent> consumer) {
        modals.put(name, consumer);
    }
    public void confirm() {
        this.discordCommands.queue();
    }

    public void executeDiscord(SlashCommandInteractionEvent event) {
        String commandName = event.getName();
        commands.stream()
                .filter(c -> c.slashCommandData().getName().equalsIgnoreCase(commandName))
                .findFirst()
                .ifPresent(c -> c.execute(event));
    }

    public void executeButton(@NotNull ButtonInteractionEvent event) {
        String customId = event.getComponentId();
        buttons.keySet().stream()
                .filter(customId::startsWith)
                .findFirst().ifPresent(id -> buttons.get(id).accept(event));
    }

    public void executeModal(@NotNull ModalInteractionEvent event) {
        String customId = event.getModalId();
        modals.keySet().stream()
                .filter(id -> Objects.equals(id, customId))
                .findFirst().ifPresent(key -> modals.get(key).accept(event));
    }
}
