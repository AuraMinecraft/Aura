package net.aniby.aura.misc.feature.end

import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryMoveItemEvent
import org.bukkit.event.inventory.InventoryPickupItemEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

data class DragonEgg(val canLeaveWith: Boolean, val inContainer: Boolean) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onQuit(event: PlayerQuitEvent) {
        if (canLeaveWith)
            return

        val player = event.player
        if (player.inventory.contains(Material.DRAGON_EGG)) {
            player.inventory.remove(Material.DRAGON_EGG)
            player.location.world.dropItemNaturally(player.location, ItemStack(Material.DRAGON_EGG))
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onClick(event: InventoryClickEvent) {
        if (inContainer)
            return

        if (event.currentItem?.type == Material.DRAGON_EGG)
            if (event.clickedInventory !is PlayerInventory)
                event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDrag(event: InventoryDragEvent) {
        if (inContainer)
            return

        if (event.cursor?.type == Material.DRAGON_EGG)
            if (event.inventory !is PlayerInventory)
                event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDrag(event: InventoryMoveItemEvent) {
        if (inContainer)
            return

        if (event.item.type == Material.DRAGON_EGG)
            if (event.destination !is PlayerInventory)
                event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onHopper(event: InventoryPickupItemEvent) {
        if (inContainer)
            return

        if (event.item.itemStack.type == Material.DRAGON_EGG)
            if (event.inventory !is PlayerInventory)
                return
    }
}