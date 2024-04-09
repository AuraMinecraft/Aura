package net.aniby.aura.misc

import co.aikar.commands.PaperCommandManager
import net.aniby.aura.misc.common.commands.GiveItemCommand
import net.aniby.aura.misc.common.item.AuraItem
import net.aniby.aura.misc.common.item.ItemRepository
import net.aniby.aura.misc.common.item.StandardItem
import net.aniby.aura.misc.feature.aura.AuraListener
import net.aniby.aura.misc.feature.end.AllowEnd
import net.aniby.aura.misc.feature.end.DisableElytra
import net.aniby.aura.misc.feature.end.DragonEgg
import net.aniby.aura.misc.feature.money.CoinListener
import net.aniby.aura.misc.feature.money.CoinRepository
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import org.bukkit.plugin.java.JavaPlugin

class AuraMisc : JavaPlugin() {
    var coinRepository: CoinRepository? = null
    var itemRepository: ItemRepository? = null

    private val miniMessage = MiniMessage.builder()
        .tags(StandardTags.defaults())
        .build()

    fun getMessage(path: String, vararg tags: TagResolver): Component {
        return miniMessage.deserialize(
            config.getConfigurationSection("messages")!!.getString(path)!!,
            *tags
        );
    }

    override fun onEnable() {
        saveDefaultConfig()
        val manager = server.pluginManager

        // Coins
        coinRepository = CoinRepository(this)
        manager.registerEvents(CoinListener(this), this)

        // End features
        val disableElytra = getConfig().getBoolean("disable_elytra.active", true)
        if (disableElytra) manager.registerEvents(
            DisableElytra(
                getConfig().getDouble("disable_elytra.allowed_end_radius"),
                getConfig().getBoolean("disable_elytra.reset_velocity")
            ), this
        )
        val allowEnd = AllowEnd(this)
        manager.registerEvents(allowEnd, this)
        manager.registerEvents(
            DragonEgg(
                getConfig().getBoolean("dragon_egg.can_leave_with"),
                getConfig().getBoolean("dragon_egg.in_container")
            ), this
        )
        manager.registerEvents(AuraListener(this), this)

        // Items
        val itemList = ArrayList<AuraItem>()
        itemList.addAll(coinRepository!!.getCoins())
        itemList.add(StandardItem("aura", 800, NamedTextColor.LIGHT_PURPLE))

        itemRepository = ItemRepository(itemList)

        // Commands
        val commandManager = PaperCommandManager(this)
        commandManager.registerCommand(allowEnd)
        commandManager.registerCommand(GiveItemCommand(this))
    }

    override fun onDisable() {}
}