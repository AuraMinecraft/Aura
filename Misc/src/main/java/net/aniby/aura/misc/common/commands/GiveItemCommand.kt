package net.aniby.aura.misc.common.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Optional
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import net.aniby.aura.misc.AuraMisc
import net.aniby.aura.misc.common.item.AuraItem
import org.bukkit.command.CommandSender

@CommandAlias("agive")
@CommandPermission("aura.misc.item.give")
data class GiveItemCommand(val plugin: AuraMisc) : BaseCommand() {
    @Default
    fun execute(sender: CommandSender, onlinePlayer: OnlinePlayer, name: String, @Optional amount: Int?) {
        val repository = plugin.itemRepository!!

        val auraItem: AuraItem? = repository.get(name)
        if (auraItem == null) {
            sender.sendMessage(plugin.getMessage("item_not_found"))
            return
        }

        var finalAmount = 1
        if (amount != null && amount > 0)
            finalAmount = amount

        val player = onlinePlayer.getPlayer()
        player.inventory.addItem(auraItem.getItemStack(
            finalAmount
        ))

        sender.sendMessage(plugin.getMessage("item_give"))
    }
}