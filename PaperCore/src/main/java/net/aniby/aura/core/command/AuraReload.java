package net.aniby.aura.core.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import net.aniby.aura.core.AuraCore;
import net.aniby.aura.core.CoreConfig;
import org.bukkit.command.CommandSender;

@CommandAlias("areload")
@CommandPermission("aura.core.reload")
public class AuraReload extends BaseCommand {
    @Default
    public void execute(CommandSender sender) {
        AuraCore instance = AuraCore.getInstance();
        instance.getDatabase().disconnect();
        instance.connectDatabase();

        instance.loadWhitelist();

        sender.sendMessage(CoreConfig.getMessage("reload"));
    }
}
