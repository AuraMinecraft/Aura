package net.aniby.aura.gamemaster.event.fault;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.core.CoreConfig;
import net.aniby.aura.gamemaster.CustomConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FaultRepository {
    public static final NamespacedKey TELEPORT_WORLD = new NamespacedKey("aura", "teleport_world");

    final CustomConfig config;
    boolean eggInEnd;


    static final Random random = new Random();
    static final List<PotionEffectType> effectTypes = List.of(
            PotionEffectType.CONFUSION,
            PotionEffectType.LEVITATION,
            PotionEffectType.DARKNESS
    );
    final BukkitTask timer;

    @SneakyThrows
    public FaultRepository(JavaPlugin plugin, CustomConfig customConfig) {
        this.config = customConfig;
        this.eggInEnd = customConfig.getConfiguration().getBoolean("egg_in_end");

        timer = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int count = 0;

            @Override
            public void run() {
                boolean change = false;
                count++;
                if (count == 1800) {
                    change = true;
                    count = 0;
                }

                for (Fault fault : list) {
                    if (change) {
                        if (eggInEnd) {
                            new Fault.Narrower(fault).run();
                        } else {
                            new Fault.Extender(fault).run();
                        }
                    }

                    for (Player player : fault.getCenterBlock().getLocation().getNearbyPlayers(
                            fault.getMaximumRadius() * 2
                    )) {
                        if (count % 5 == 0) {
                            player.addPotionEffect(new PotionEffect(
                                    effectTypes.get(random.nextInt(effectTypes.size())), 10 * 20, 1
                            ));
                        }

                        Vector from = player.getLocation().toVector();
                        Vector to = fault.getCenterBlock().getLocation().toVector();
                        Vector velocity = to.subtract(from);

                        player.setVelocity(velocity.normalize().multiply(0.5));
                    }
                }
            }
        }, 200L, 40L);
    }

    final ArrayList<Fault> list = new ArrayList<>();

    @SneakyThrows
    public void setEggInEnd(boolean inEnd) {
        eggInEnd = inEnd;

        config.getConfiguration().set("egg_in_end", inEnd);
        config.save();
    }

    static String stringfyLocation(Location location) {
        return location.getWorld().getName() + ";" + location.getBlockX() + ";" + location.getBlockY() + ";" + location.getBlockZ();
    }

    static Location locatifyString(String string) {
        String[] loc = string.split(";");
        return new Location(
                Bukkit.getWorld(loc[0]),
                Integer.parseInt(loc[1]),
                Integer.parseInt(loc[2]),
                Integer.parseInt(loc[3])
        );
    }

    public @Nullable Fault getNearest(Location location, double radius) {
        return list.stream()
                .filter(f -> f.getCenterBlock().getLocation().distance(location) <= radius
                        &&
                        f.getCenterBlock().getWorld() == location.getWorld())
                .min(Comparator.comparingDouble(c -> c.getCenterBlock().getLocation().distance(location)))
                .orElse(null);
    }

    public @Nullable Fault getNearestBySelf(Location location, double multiplier) {
        return list.stream()
                .filter(f -> f.getCenterBlock().getWorld() == location.getWorld()
                        &&
                        f.getCenterBlock().getLocation().distance(location) <= f.getMaximumRadius() * multiplier)
                .min(Comparator.comparingDouble(c -> c.getCenterBlock().getLocation().distance(location)))
                .orElse(null);
    }

    public @Nullable Fault getNearest(@Nullable Location location) {
        return location != null ? list.stream()
                .filter(f -> f.getCenterBlock().getWorld() == location.getWorld())
                .min(Comparator.comparingDouble(c -> c.getCenterBlock().getLocation().distance(location)))
                .orElse(null) : null;
    }

    @SneakyThrows
    public @Nullable Fault create(Location center, Location exitLocation, String creator) {
        if (creator != null && !creator.equals("abyss")) {
            List<String> creatorList = config.getConfiguration().getStringList("fault_creators");
            if (creatorList.contains(creator))
                return null;
            creatorList.add(creator);
            config.getConfiguration().set("fault_creators", creatorList);
        }

        Fault fault = new Fault(center, exitLocation, 0, creator);
        list.add(fault);
        save(fault);

        new Fault.Extender(fault).run();
        return fault;
    }

    @SneakyThrows
    public void save(Fault fault) {
        List<String> sections = this.config.getConfiguration().getStringList("list");
        sections.stream().filter(s -> s.startsWith(fault.toFindString())).findFirst().ifPresent(sections::remove);
        sections.add(fault.toString());
        this.config.getConfiguration().set("list", sections);
        this.config.save();
    }

    @SneakyThrows
    public void loadAll() {
        list.clear();
        for (String string :  this.config.getConfiguration().getStringList("list")) {
            Fault fault = new Fault(string);
            list.add(fault);
        }
    }

    @SneakyThrows
    public void remove(Fault fault) {
        List<String> sections = this.config.getConfiguration().getStringList("list");
        String string = sections.stream().filter(s -> s.startsWith(fault.toFindString())).findFirst().orElse(null);
        if (string != null) {
            sections.remove(string);
            this.config.getConfiguration().set("list", sections);
            this.config.getConfiguration().save(this.config.getFile());
        }
        list.remove(fault);
    }
}
