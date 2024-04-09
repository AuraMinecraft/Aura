package net.aniby.aura.misc.common.item

import de.tr7zw.nbtapi.NBT
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

interface AuraItem {
    val name: String
    val customModelData: Int
    val component: Component

    fun getItemStack(): ItemStack {
        return getItemStack(1)
    }
    fun getItemStack(amount: Int): ItemStack {
        val item = ItemStack(Material.STRUCTURE_BLOCK)
        val itemMeta = item.itemMeta
        itemMeta.displayName(this.component)
        itemMeta.setCustomModelData(this.customModelData)
        item.itemMeta = itemMeta
        item.amount = amount

        NBT.modify(item) { nbt ->
            nbt.setString("aura_item", this.name)
        }
        return item
    }
}