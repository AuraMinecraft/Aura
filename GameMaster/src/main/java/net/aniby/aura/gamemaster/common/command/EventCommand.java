package net.aniby.aura.gamemaster.common.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.aniby.aura.core.CoreConfig;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.gamemaster.EventQueue;
import net.aniby.aura.gamemaster.AuraGameMaster;
import net.aniby.aura.gamemaster.MasterMessage;
import net.aniby.aura.core.AuraCore;
import net.aniby.aura.repository.UserRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

@CommandAlias("event")
public class EventCommand extends BaseCommand {
    @Subcommand("answer")
    @Description("Answer to Game Master's event invite")
    public void answer(CommandSender sender, String answer) {
        String name = sender.getName();
        EventQueue queue = EventQueue.getQueue().stream().filter(
                q -> Objects.equals(q.getReceiver(), name)
        ).findFirst().orElse(null);
        if (queue == null) {
            sender.sendMessage(AuraGameMaster.getMessage("no_event"));
            return;
        }

        EventQueue.getQueue().remove(queue);

        switch (answer) {
            case "accept" -> {
                UserRepository repository = AuraCore.getInstance().getUserRepository();
                AuraUser user = repository.findByPlayerName(name);
                if (user == null) {
                    sender.sendMessage(
                            CoreConfig.getMessage("database_error")
                    );
                    return;
                }

                sender.sendMessage(AuraGameMaster.getMessage("answer_accepted",
                        Placeholder.unparsed("event_aura", String.valueOf(queue.getAura()))
                ));

                Player master = Bukkit.getPlayer(queue.getSender());
                if (master != null)
                    master.sendMessage(AuraGameMaster.getMessage("answer_accepted_master"));

                user.setAura(user.getAura() - queue.getAura());
                repository.update(user);
                return;
            }
            case "decline" -> {
                sender.sendMessage(AuraGameMaster.getMessage("answer_declined"));

                Player master = Bukkit.getPlayer(queue.getSender());
                if (master != null)
                    master.sendMessage(AuraGameMaster.getMessage("answer_declined_master"));
                return;
            }
        }
        sender.sendMessage(AuraGameMaster.getMessage("invalid_arguments"));
    }

    @Subcommand("suggest")
    @CommandPermission("aura.gamemaster.suggest")
    @Description("Suggest event to player")
    public void suggest(CommandSender sender, OnlinePlayer target, double aura, String string) {
        Component message = CoreConfig.getMiniMessage().deserialize(string);
        Player player = target.getPlayer();
        String playerName = player.getName();

        EventQueue queue = EventQueue.getQueue().stream().filter(
                q -> Objects.equals(q.getReceiver(), playerName)
        ).findFirst().orElse(null);
        if (queue != null) {
            EventQueue.getQueue().remove(queue);
        }
        EventQueue.getQueue().add(new EventQueue(sender.getName(), playerName, aura));

        player.sendMessage(message);
        player.sendMessage(AuraGameMaster.getMessage("answer_response"));
    }

    @Subcommand("message")
    @CommandPermission("aura.gamemaster.message")
    @Description("Send MiniMessage message to user")
    public void message(CommandSender sender, OnlinePlayer target, String string) {
        Component component = CoreConfig.getMiniMessage().deserialize(string);

        Player targetPlayer = target.getPlayer();
        MasterMessage message = new MasterMessage(new Date(), sender.getName(), targetPlayer.getName(), component);
        MasterMessage.addMessage(message);

        targetPlayer.sendMessage(component);
        sender.sendMessage(AuraGameMaster.getMessage("event_message_sent"));
    }

    @Subcommand("list")
    @CommandCompletion("@players")
    @Description("Get list of received messages by player from gamemaster")
    public void list(CommandSender sender, @Optional Integer page, @Optional String targetName) {
        if (page == null || page <= 0)
            page = 1;
        if (targetName != null) {
            if (!sender.hasPermission("aura.gamemaster.list.other")) {
                sender.sendMessage(CoreConfig.getMessage("no_permission"));
                return;
            }
        } else if (sender instanceof Player player) {
            targetName = player.getName();
        }

        if (targetName == null) {
            sender.sendMessage(CoreConfig.getMessage("no_player"));
            return;
        }

        sender.sendMessage(AuraGameMaster.getMessage("list_title", Placeholder.unparsed("name", targetName)));
        for (MasterMessage message : MasterMessage.getMessages(targetName, page)) {
            Component component = AuraGameMaster.getMessage("list_prefix",
                    Placeholder.unparsed("date", formatter.format(message.date())),
                    Placeholder.unparsed("sender", message.sender())
            ).append(
                    message.component()
            );
            sender.sendMessage(component);
        }
    }

    DateFormat formatter = new SimpleDateFormat("HH:mm");
}
