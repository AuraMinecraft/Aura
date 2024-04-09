package net.aniby.aura.misc.feature.money

import net.aniby.aura.misc.AuraMisc
import org.bukkit.entity.WanderingTrader
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.CreatureSpawnEvent

data class CoinListener(val plugin: AuraMisc) : Listener {
    @EventHandler
    fun onSpawn(event: CreatureSpawnEvent) {
        if (event.entity !is WanderingTrader)
            return

        val trader: WanderingTrader = event.entity as WanderingTrader
        trader.recipes = plugin.coinRepository!!.getAllRecipes()
    }
}
