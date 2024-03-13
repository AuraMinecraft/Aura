package net.aniby.aura.gamemaster.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.aniby.aura.gamemaster.GameMaster;
import net.aniby.aura.gamemaster.MasterMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("emessage|emsg")
@CommandPermission("aura.gamemaster.message")
public class EventMessageCommand extends BaseCommand {
    @Default
    public void execute(CommandSender sender, OnlinePlayer target, String string) {
        Component component = GameMaster.getMiniMessage().deserialize(string);

        Player targetPlayer = target.getPlayer();
        MasterMessage message = new MasterMessage(sender.getName(), targetPlayer.getName(), component);
        MasterMessage.getMessages().add(message);

        targetPlayer.sendMessage(component);
    }
}
