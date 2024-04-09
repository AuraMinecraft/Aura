package net.aniby.aura.misc.common.item

data class ItemRepository(val list: List<AuraItem>) {
    fun get(name: String): AuraItem? {
        return list.stream().filter { i -> i.name == name }.findFirst().orElse(null)
    }
}