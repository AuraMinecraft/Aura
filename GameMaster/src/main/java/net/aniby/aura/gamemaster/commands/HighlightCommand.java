package net.aniby.aura.gamemaster.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.aniby.aura.gamemaster.GameMaster;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.inventivetalent.glow.GlowAPI;

@CommandAlias("highlight")
public class HighlightCommand extends BaseCommand {
    @CommandPermission("aura.gamemaster.highlight")
    @Description("Switching target's highlight")
    public void execute(CommandSender sender, OnlinePlayer onlinePlayer, @Optional String colorName) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GameMaster.getMessage("invalid_executor"));
            return;
        }
        Player target = onlinePlayer.getPlayer();

        GlowAPI.Color color = GlowAPI.Color.NONE;
        try {
            if (colorName != null) {
                color = GlowAPI.Color.valueOf(colorName);
            }
        } catch (Exception ignored) {
            sender.sendMessage(GameMaster.getMessage("invalid_arguments"));
            return;
        }
        GlowAPI.setGlowing(target, color, player);
        sender.sendMessage(
                GameMaster.getMessage(
                        color == GlowAPI.Color.NONE ? "highlighting_disabled" : "highlighted",
                        Placeholder.unparsed("target", target.getName())
                )
        );
        return;
    }
}
