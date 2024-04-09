package net.aniby.aura.misc.feature.end

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.util.Vector

data class DisableElytra(val allowedEndRadius : Double, val resetVelocity: Boolean) : Listener {
    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val from = event.from
        val to = event.to
        if (from.blockX == to.blockX && from.blockY == to.blockY && from.blockZ == to.blockZ) {
            return
        }
        if (isOutOfRadius(to)) {
            val player = event.player
            if (!player.isGliding) return
            player.isGliding = false
            if (resetVelocity) player.velocity = Vector(0, 0, 0)
        }
    }

    @EventHandler
    fun onElytra(event: EntityToggleGlideEvent) {
        val entity = event.entity as? Player ?: return
        if (event.isGliding && isOutOfRadius(entity.location)) {
            entity.isGliding = false
            event.isCancelled = true
            if (resetVelocity) entity.velocity = Vector(0, 0, 0)
        }
    }

    private fun isOutOfRadius(to: Location): Boolean {
        val end = Bukkit.getWorld("world_the_end")
        if (end != null && to.getWorld() === end) {
            val location = Location(end, 0.0, 90.0, 0.0)
            return location.distance(to) >= allowedEndRadius
        }
        return false
    }
}