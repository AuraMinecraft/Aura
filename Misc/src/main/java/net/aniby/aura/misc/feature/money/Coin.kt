package net.aniby.aura.misc.feature.money

import net.aniby.aura.misc.common.item.AuraItem
import net.aniby.aura.misc.util.KotlinUtil.Companion.toItemComponent
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MerchantRecipe
import kotlin.random.Random

data class Coin(override val name: String, override val customModelData: Int, val holders: List<Holder>) : AuraItem {
    override val component: Component = name.toItemComponent()

    fun getRecipe(): MerchantRecipe {
        val recipe = MerchantRecipe(getItemStack(), Random.nextInt(1, 3))
        recipe.addIngredient(holders[Random.nextInt(holders.size)].getItemStack())
        return recipe
    }

    data class Holder(val name: String, val base: Int, val range: Int) {
        private fun getMaterial(): Material {
            return CoinRepository.materialMap[name]!![Random.nextInt(2)]
        }
        private fun getCost(): Int {
            return Random.nextInt(-range, range + 1) + base
        }

        fun getItemStack(): ItemStack {
            return ItemStack(getMaterial(), getCost())
        }
    }
}