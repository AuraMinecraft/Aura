package net.aniby.aura.misc.feature.aura

import net.aniby.aura.misc.AuraMisc
import org.bukkit.FluidCollisionMode
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.util.Vector

data class AuraListener(val plugin: AuraMisc) : Listener {
    @EventHandler
    fun prepareAnvil(event: PrepareAnvilEvent) {
        if (event.viewers.isEmpty())
            return

        val location = event.inventory.location ?: return

        val human = event.viewers[0]
        if (human.world.name != "world_the_end")
            return

        val item = event.inventory.getItem(0)
        if (item == null || item.type != Material.DRAGON_BREATH)
            return

        var text = event.inventory.renameText ?: return

        text = text.lowercase()
        if (text == "аура" || text == "aura") {
//            val result = human.world.rayTraceBlocks(
//                location.clone().add(0.0, 1.0, 0.0), Vector(0, 1, 0), 320.0, FluidCollisionMode.NEVER, true
//            ) ?: return
//            if (result.hitBlock?.type == Material.DRAGON_EGG) {
//                val auraItem = plugin.itemRepository!!.get("aura")!!
//                event.result = auraItem.getItemStack(item.amount)
//            }

            if (human.location.add(0.0,1.0,0.0).block.type == Material.DRAGON_EGG) {
                val auraItem = plugin.itemRepository!!.get("aura")!!
                event.result = auraItem.getItemStack(item.amount)
            }
        }
    }
}