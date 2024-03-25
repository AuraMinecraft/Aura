package net.aniby.aura.core;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import org.bukkit.command.CommandSender;

@CommandAlias("areload")
public class ReloadCommand {
    @Default
    @CommandPermission("aura.core.reload")
    public void execute(CommandSender sender) {
        AuraCore instance = AuraCore.getInstance();

        instance.reloadConfig();

        instance.getDatabase().disconnect();
        instance.connectDatabase();
    }
}
