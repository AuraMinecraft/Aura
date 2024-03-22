package net.aniby.aura.paper.votes.logic;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.core.AuraCore;
import net.aniby.aura.paper.votes.AuraVotes;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.tool.AuraUtils;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Getter
@AllArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuraCollection {
    Action action;
    double cost;
    long expiresAt;
    HashMap<String, Double> collected = new HashMap<>();

    public ArrayList<TagResolver> getResolvers() {
        int minutes = (int) Math.floor(
                (expiresAt - new Date().getTime()) / 1000.0 / 60
        );
        return new ArrayList<>(List.of(
                Placeholder.unparsed("action_id", action.name()),
                Placeholder.unparsed("collection_cost", String.valueOf(
                        AuraUtils.roundDouble(cost)
                )),
                Placeholder.unparsed("collection_left_minutes", String.valueOf(minutes)),
                Placeholder.unparsed("collected_aura", String.valueOf(AuraUtils.roundDouble(getSummaryCollected()))),
                Placeholder.unparsed("action_name", AuraVotes.getInstance().getConfig().getString("actions." + action.name()))
        ));
    }

    public double getSummaryCollected() {
        return collected.values().stream().mapToDouble(s -> s).sum();
    }

    public void collect(String name, double deposit) {
        UserRepository repository = AuraCore.getInstance().getUserRepository();
        AuraUser user = repository.findByPlayerName(name);
        assert user != null;
        user.setAura(user.getAura() - deposit);
        repository.update(user);

        if (collected.containsKey(name))
            collected.replace(name, collected.get(name) + deposit);
        else collected.put(name, deposit);

        if (isCollected())
            end(null);
    }

    public boolean isExpired() {
        return new Date().getTime() >= this.expiresAt;
    }

    public boolean isCollected() {
        return getSummaryCollected() >= cost;
    }

    public boolean end(String forceAdmin) {
        AuraCore core = AuraCore.getInstance();
        UserRepository repository = core.getUserRepository();

        // Calculating
        double summary = getSummaryCollected();
        double leaved = summary - cost;
        boolean isCollected = leaved >= 0;

        // On success executing
        if (isCollected) {
            action.getOnSuccess().run();
        }

        // Message broadcasting
        ArrayList<TagResolver> resolvers = getResolvers();
        Bukkit.broadcast(AuraVotes.getMessage(
                "collection_end_" + (isCollected ? "success" : "fail"),
                resolvers
        ));
        if (forceAdmin != null) {
            ArrayList<TagResolver> forceResolvers = new ArrayList<>(resolvers);
            forceResolvers.add(Placeholder.unparsed("admin", forceAdmin));
            Bukkit.broadcast(
                    AuraVotes.getMessage("collection_end_force", forceResolvers)
            );
        }

        // Deposit return
        for (String depositor : collected.keySet()) {
            double depositValue = collected.get(depositor);

            AuraUser depositUser = repository.findByPlayerName(depositor);
            assert depositUser != null;
            double depositReturn = isCollected
                    ? leaved * depositValue / summary
                    : depositValue;

            depositUser.setAura(depositUser.getAura() + depositReturn);
            try {
                repository.update(depositUser);
            } catch (Exception e) {
                try {
                    core.getDatabase().disconnect();
                    core.getDatabase().connect();

                    repository.update(depositUser);
                } catch (SQLException ignored) {

                }
            }

            Player player = Bukkit.getPlayer(depositor);
            if (player != null) {
                ArrayList<TagResolver> individualResolvers = new ArrayList<>(resolvers);
                individualResolvers.add(Placeholder.unparsed("deposit_return_aura", String.valueOf(depositReturn)));
                player.sendMessage(
                        AuraVotes.getMessage("collection_deposit_return", individualResolvers)
                );
            }
        }

        // Nulling active collection
        AuraVotes.getInstance().setActiveCollection(null);
        return isCollected;
    }

    @Getter
    @AllArgsConstructor
    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    public enum Action {
        EXTEND_OVERWORLD_BARRIER_100(() -> {
            World overworld = Bukkit.getWorld("world");
            WorldBorder border = overworld.getWorldBorder();
            double size = border.getSize() + 200;
            long time = 20L;
            border.setSize(size, time);
        }),
        EXTEND_OVERWORLD_BARRIER_300(() -> {
            World overworld = Bukkit.getWorld("world");
            WorldBorder border = overworld.getWorldBorder();
            double size = border.getSize() + 600;
            long time = 60L;
            border.setSize(size, time);
        }),
        EXTEND_OVERWORLD_BARRIER_500(() -> {
            World overworld = Bukkit.getWorld("world");
            WorldBorder border = overworld.getWorldBorder();
            double size = border.getSize() + 1000;
            long time = 100L;
            border.setSize(size, time);
        }),
        EXTEND_OVERWORLD_BARRIER_1000(() -> {
            World overworld = Bukkit.getWorld("world");
            WorldBorder border = overworld.getWorldBorder();
            double size = border.getSize() + 2000;
            long time = 200L;
            border.setSize(size, time);
        }),

        EXTEND_NETHER_BARRIER_100(() -> {
            World overworld = Bukkit.getWorld("world_nether");
            WorldBorder border = overworld.getWorldBorder();
            double size = border.getSize() + 200;
            long time = 20L;
            border.setSize(size, time);
        }),
        EXTEND_NETHER_BARRIER_300(() -> {
            World overworld = Bukkit.getWorld("world_nether");
            WorldBorder border = overworld.getWorldBorder();
            double size = border.getSize() + 600;
            long time = 60L;
            border.setSize(size, time);
        }),
        EXTEND_NETHER_BARRIER_500(() -> {
            World overworld = Bukkit.getWorld("world_nether");
            WorldBorder border = overworld.getWorldBorder();
            double size = border.getSize() + 1000;
            long time = 100L;
            border.setSize(size, time);
        }),
        EXTEND_NETHER_BARRIER_1000(() -> {
            World overworld = Bukkit.getWorld("world_nether");
            WorldBorder border = overworld.getWorldBorder();
            double size = border.getSize() + 2000;
            long time = 200L;
            border.setSize(size, time);
        }),

        EXTEND_END_BARRIER_100(() -> {
            World overworld = Bukkit.getWorld("world_the_end");
            WorldBorder border = overworld.getWorldBorder();
            double size = border.getSize() + 200;
            long time = 20L;
            border.setSize(size, time);
        }),
        EXTEND_END_BARRIER_300(() -> {
            World overworld = Bukkit.getWorld("world_the_end");
            WorldBorder border = overworld.getWorldBorder();
            double size = border.getSize() + 600;
            long time = 60L;
            border.setSize(size, time);
        }),
        EXTEND_END_BARRIER_500(() -> {
            World overworld = Bukkit.getWorld("world_the_end");
            WorldBorder border = overworld.getWorldBorder();
            double size = border.getSize() + 1000;
            long time = 100L;
            border.setSize(size, time);
        }),
        EXTEND_END_BARRIER_1000(() -> {
            World overworld = Bukkit.getWorld("world_the_end");
            WorldBorder border = overworld.getWorldBorder();
            double size = border.getSize() + 2000;
            long time = 200L;
            border.setSize(size, time);
        }),

        ADMIN_LIGHTNING_BOLT(() -> {
            Player player = Bukkit.getPlayer("An1by");
            if (player != null) {
                Location location = player.getLocation();
                location.getWorld().spawn(location, LightningStrike.class);
            }
        });

        private final @NotNull Runnable onSuccess;
    }
}
