package net.aniby.aura.misc.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.Reset
import java.lang.StringBuilder

class KotlinUtil {
    companion object {
        fun <T> List<T>.update(index: Int, item: T): List<T> = toMutableList().apply { this[index] = item }

        fun String.toItemComponent(color: TextColor = NamedTextColor.WHITE): Component {
            val builder = StringBuilder()
            val split = this.split("_")
            for (i in split.indices) {
                val part = split[i]
                builder.append(part[0].uppercase() + part.substring(1, part.length))
                if (i != split.size - 1)
                    builder.append(" ")
            }
            val displayName: String = builder.toString()
            val translatableKey: String = String.format("aura:item.%s", this)
            val style = Style.style(TextDecoration.ITALIC.withState(false)).color(color)

            return Component.translatable(
                translatableKey, displayName, style
            )
        }
    }
}