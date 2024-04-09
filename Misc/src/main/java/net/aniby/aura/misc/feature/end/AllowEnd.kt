package net.aniby.aura.misc.feature.end

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import net.aniby.aura.misc.AuraMisc
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerTeleportEvent

@CommandAlias("end")
@CommandPermission("aura.misc.end")
data class AllowEnd(val plugin: AuraMisc) : Listener, BaseCommand() {
    @Default
    fun default(sender: CommandSender) {
        val config = plugin.config

        val allow = !config.getBoolean("allow_end")
        config.set("allow_end", allow)
        plugin.saveConfig()

        sender.sendMessage(plugin.getMessage("switch_end"))
    }

    @EventHandler
    fun onTeleport(event: PlayerTeleportEvent) {
        if (event.to.world.name != "world_the_end")
            return

        val allow = plugin.config.getBoolean("allow_end")
        if (!allow && !event.player.hasPermission("aura.misc.end")) {
            event.isCancelled = true
        }
    }
}