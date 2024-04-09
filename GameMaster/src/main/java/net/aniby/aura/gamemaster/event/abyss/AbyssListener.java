package net.aniby.aura.gamemaster.event.abyss;

import net.aniby.aura.gamemaster.AuraGameMaster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class AbyssListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    void onJoin(PlayerJoinEvent event) {
        AuraGameMaster.getAbyss().updateAttributes(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onQuit(PlayerQuitEvent event) {
        AuraGameMaster.getAbyss().getInvites().remove(event.getPlayer());
    }
}
