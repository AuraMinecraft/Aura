package net.aniby.aura.misc.common.item

import net.aniby.aura.misc.util.KotlinUtil.Companion.toItemComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

data class StandardItem(override val name: String, override val customModelData: Int, val color: TextColor = NamedTextColor.WHITE) : AuraItem {
    override val component: Component = name.toItemComponent(color)
}