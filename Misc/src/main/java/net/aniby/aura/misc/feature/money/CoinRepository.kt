package net.aniby.aura.misc.feature.money

import net.aniby.aura.misc.AuraMisc
import org.bukkit.Material
import org.bukkit.inventory.MerchantRecipe
import kotlin.random.Random

data class CoinRepository(val plugin: AuraMisc) {
    companion object {
        val materialMap: HashMap<String, List<Material>> = hashMapOf(
            "emerald" to listOf(Material.DEEPSLATE_EMERALD_ORE, Material.EMERALD_ORE),
            "diamond" to listOf(Material.DEEPSLATE_DIAMOND_ORE, Material.DIAMOND_ORE),
            "iron" to listOf(Material.DEEPSLATE_IRON_ORE, Material.IRON_ORE),
            "gold" to listOf(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_ORE),
            "copper" to listOf(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_ORE),
            "lapis" to listOf(Material.DEEPSLATE_LAPIS_ORE, Material.LAPIS_ORE)
        )
    }

    fun getCoins(): List<Coin> {
        val coins = ArrayList<Coin>()
        val money = plugin.config.getConfigurationSection("money")!!

        for (key in money.getKeys(false)) {
            val section = money.getConfigurationSection(key)!!
            val customModelData = section.getInt("custom_model_data")
            val holders = ArrayList<Coin.Holder>()

            for (holderKey in section.getKeys(false)) {
                if (holderKey == "custom_model_data" || holderKey == "aura")
                    continue

                val holderSection = section.getConfigurationSection(holderKey)!!
                holders.add(Coin.Holder(holderKey, holderSection.getInt("base"), holderSection.getInt("range")))
            }

            val coin = Coin(key, customModelData, holders)
            coins.add(coin)
        }
        return coins
    }

    private fun upgradeRecipe(ingredientCoin: Coin, resultCoin: Coin): MerchantRecipe {
        val ingredient = ingredientCoin.getItemStack()
        ingredient.amount = 8

        val recipe = MerchantRecipe(resultCoin.getItemStack(), Random.nextInt(1, 15))
        recipe.addIngredient(ingredient)

        return recipe
    }

    private fun downgradeRecipe(ingredientCoin: Coin, resultCoin: Coin): MerchantRecipe {
        val result = resultCoin.getItemStack()
        result.amount = 8

        val recipe = MerchantRecipe(result, Random.nextInt(1, 15))
        recipe.addIngredient(ingredientCoin.getItemStack())

        return recipe
    }

    private fun coinWithAura(coin: Coin, cost: Int): MerchantRecipe {
        val result = coin.getItemStack()
        val recipe = MerchantRecipe(result, Random.nextInt(1, 5))

        val aura = plugin.itemRepository!!.get("aura")!!.getItemStack(cost)
        recipe.addIngredient(aura)

        return recipe
    }

    fun getAllRecipes(): List<MerchantRecipe> {
        val repository = plugin.itemRepository!!
        val gold = repository.get("gold_coin")!! as Coin
        val silver = repository.get("silver_coin")!! as Coin
        val copper = repository.get("copper_coin")!! as Coin

        return listOf(
            gold.getRecipe(),
            silver.getRecipe(),
            copper.getRecipe(),
            upgradeRecipe(copper, silver),
            upgradeRecipe(silver, gold),
            downgradeRecipe(gold, silver),
            downgradeRecipe(silver, copper),
            coinWithAura(gold, plugin.config.getInt("money.gold_coin.aura"))
        ).shuffled()
    }
}