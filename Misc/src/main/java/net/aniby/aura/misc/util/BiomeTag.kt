package net.aniby.aura.misc.util

import org.bukkit.block.Biome
import java.util.*

enum class BiomeTag(vararg biomes: Biome) {
    WINDSWEPT_HILLS(Biome.WINDSWEPT_HILLS, Biome.WINDSWEPT_FOREST, Biome.WINDSWEPT_GRAVELLY_HILLS),
    MOUNTAINS(Biome.MEADOW, Biome.GROVE, Biome.SNOWY_SLOPES, Biome.JAGGED_PEAKS, Biome.FROZEN_PEAKS, Biome.STONY_PEAKS);


    private var biomes: List<Biome>? = null
    init {
        this.biomes = Arrays.stream(biomes).toList()
    }

    fun isTagged(biome: Biome): Boolean {
        return this.biomes!!.contains(biome)
    }
}