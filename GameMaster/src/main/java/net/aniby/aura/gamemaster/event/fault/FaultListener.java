package net.aniby.aura.gamemaster.event.fault;

import lombok.SneakyThrows;
import net.aniby.aura.gamemaster.AuraGameMaster;
import net.aniby.aura.tool.DiscordWebhook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.Random;

public class FaultListener implements Listener {
    final Random random = new Random();

    @SneakyThrows
    @EventHandler
    void onItemDestroy(ItemDespawnEvent event) {
        if (event.getEntity().getItemStack().getType() == Material.DRAGON_EGG) {
            DiscordWebhook webhook = new DiscordWebhook(
                    AuraGameMaster.getFaultRepository().getConfig().getConfiguration()
                            .getString("event_webhook")
            );
            webhook.setContent("@everyone АТАС!!! яйцо задеспавнилось");
            webhook.execute();
        }
    }

    @SneakyThrows
    @EventHandler
    void onItemDestroy(EntityDeathEvent event) {
        if (event.getEntity() instanceof Item item && item.getItemStack().getType() == Material.DRAGON_EGG) {
            DiscordWebhook webhook = new DiscordWebhook(
                    AuraGameMaster.getFaultRepository().getConfig().getConfiguration()
                            .getString("event_webhook")
            );
            webhook.setContent("@everyone АТАС!!! яйцо было уничтожено");
            webhook.execute();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            Fault fault = AuraGameMaster.getFaultRepository().getNearest(event.getFrom());
            if (fault != null) {
                int radius = fault.getMaximumRadius() + 3;

                if (fault.getCenterBlock().getLocation().distance(player.getLocation()) <= radius) {
                    event.setCancelled(true);
                    player.teleportAsync(fault.getExitLocation());
                }
//                event.setCancelled(true);
//
//                Location location = event.getTo().clone();
//                location.setWorld(world);
//                event.getPlayer().teleportAsync(location);
            }
        }
    }

    @EventHandler
    void onDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        if (item.getItemStack().getType() == Material.DRAGON_EGG) {
            World world = item.getWorld();
            if (!world.getName().equals("world_the_end")) {
                if (random.nextInt(4) == 0) {
                    AuraGameMaster.getFaultRepository().create(
                            item.getLocation(),
                            Fault.getRandomExitLocation(),
                            event.getPlayer().getName()
                    );
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onChangeWorld(PlayerChangedWorldEvent event) {
        FaultRepository repository = AuraGameMaster.getFaultRepository();

        Player player = event.getPlayer();
        World to = player.getLocation().getWorld();
        World from = event.getFrom();
        if (to.getName().equals("world_the_end")) {
            repository.setEggInEnd(true);
        } else {
            if (from.getName().equals("world_the_end")
                    && player.getInventory().contains(Material.DRAGON_EGG)) {
                repository.setEggInEnd(false);

                Bukkit.getScheduler().runTaskLater(AuraGameMaster.getInstance(), new Runnable() {
                    final Location center = player.getLocation().clone()
                            .add(Vector.getRandom().multiply(random.nextInt(3, 7)));
                    final String creator = player.getName();
                    final Location exitLocation = Fault.getRandomExitLocation();

                    @Override
                    public void run() {
                        repository.create(center, exitLocation, creator);
                    }
                }, 20L);
            }
        }
    }
}
