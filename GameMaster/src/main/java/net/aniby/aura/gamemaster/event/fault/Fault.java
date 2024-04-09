package net.aniby.aura.gamemaster.event.fault;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.core.CoreConfig;
import net.aniby.aura.gamemaster.AuraGameMaster;
import net.aniby.aura.gamemaster.util.BlockFrequencyMap;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.EndGateway;
import org.bukkit.block.data.type.RespawnAnchor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Fault {
    static final Random random = new Random();

    final List<Block> blocks = new ArrayList<>();
    final Location exitLocation;
    final Block centerBlock;
    @Setter
    double aura;

    @Getter
    boolean processing = false;

    //////////////////
    public Fault(String string) {
        String[] split = string.split("\\|");

        Location center = FaultRepository.locatifyString(split[0]);
        this.exitLocation = FaultRepository.locatifyString(split[1]);
        this.aura = Double.parseDouble(split[2]);

        // Getting block
        this.centerBlock = center.getBlock();
        this.addPart(this.centerBlock, null);
        center = centerBlock.getLocation();

        // Finding all around
        findAround(center);
    }

    public Fault(Location center, Location exit, double aura, String creator) {
        this.aura = aura;
        this.exitLocation = exit;

        // Getting block
        this.centerBlock = center.getBlock();
        this.addPart(this.centerBlock, creator);
    }

    void findAround(Location findLocation) {
        for (Location location : getAround(findLocation)) {
            Block block = location.getBlock();
            if (block.getType() == Material.END_GATEWAY) {
                if (!this.blocks.contains(block)) {
                    this.blocks.add(block);
                    findAround(location);
                }
            }
        }
    }

    void setPart(Block block, @Nullable String creator) {
        String user = "#fault-" + creator;
        CoreProtectAPI protect = AuraGameMaster.getInstance().getCoreProtect();
        Location location = block.getLocation();

        if (creator != null && protect != null)
            protect.logRemoval(user, location, block.getType(), block.getBlockData());

        block.setType(Material.END_GATEWAY);

        EndGateway gateway = (EndGateway) block.getState();
        Location realExit = exitLocation.clone();
        realExit.setWorld(centerBlock.getWorld());
        gateway.setExitLocation(realExit);
        gateway.setExactTeleport(true);
        gateway.update(true);

        if (creator != null && protect != null)
            protect.logPlacement(user, location, block.getType(), block.getBlockData());

    }

    void addPart(Block block, @Nullable String creator) {
        setPart(block, creator);
        this.blocks.add(block);
    }

    void clearPart(Block block) {
        this.blocks.remove(block);

        CoreProtectAPI protect = AuraGameMaster.getInstance().getCoreProtect();
        if (protect != null)
            protect.logRemoval("#fault-abyss", block.getLocation(), block.getType(), block.getBlockData());
        block.setType(Material.AIR);
    }

    void restore() {
        for (Block block : this.getBlocks()) {
            if (block.getType() != Material.END_GATEWAY)
                this.setPart(block, null);
        }
    }

    static boolean isReplaceableForFault(Material material) {
        switch (material) {
            case BARRIER, BEDROCK, DRAGON_EGG -> {
                return false;
            }
        }
        return true;
    }

    List<Block> blocksToExtend() {
        List<Block> blockList = new ArrayList<>();

        boolean single = this.blocks.size() == 1;
        for (Block blockFromList : this.blocks) {
            for (Location location : getNearest(blockFromList.getLocation())) {
                World world = location.getWorld();
                int y = location.getBlockY();
                if (y > world.getMaxHeight() || y < world.getMinHeight())
                    continue;

                Block block = location.getBlock();
                if (!isReplaceableForFault(block.getType()))
                    continue;

                if (!this.blocks.contains(block)) {
                    if (random.nextInt(6) == 0 || single) {
                        single = false;
                        blockList.add(block);
                    }
                }
            }
        }
        return blockList;
    }

    List<Block> blocksToCompress() {
        if (this.blocks.size() == 1) {
            return this.blocks;
        }

        Location center = this.centerBlock.getLocation();
        Map<Block, Integer> blocksByDistance = this.blocks.stream().collect(Collectors.toMap(
                e -> e,
                e -> (int) Math.round(e.getLocation().distance(center))
        ));
        return new BlockFrequencyMap(blocksByDistance).getMostCommon();
    }

    int getMaximumRadius() {
        if (this.blocks.size() == 1) {
            return 1;
        }

        Location center = this.centerBlock.getLocation();
        return this.blocks.stream()
                .mapToInt(e -> (int) Math.round(e.getLocation().distance(center)))
                .max().orElse(1);
    }

    public static Location getRandomExitLocation() {
        // Getting maximum X, Z
//        List<World> worlds = Bukkit.getWorlds();
//        World to = from;
//        while (to == from)
//            to = worlds.get(random.nextInt(worlds.size()));

        World to = Bukkit.getWorld("world_the_end");

        WorldBorder border = to.getWorldBorder();
        double maximum = border.getSize() / 2;

        // Randomize coordinates
        int x = (int) Math.round(random.nextDouble(-maximum, maximum));
        int z = (int) Math.round(random.nextDouble(-maximum, maximum));

        // Getting Y
        int y = to.getHighestBlockYAt(x, z) + 1;

        return new Location(to, x, y, z);
    }

    static Location[] getNearest(Location center) {
        return new Location[]{
                center.clone().add(BlockFace.NORTH.getDirection()),
                center.clone().add(BlockFace.EAST.getDirection()),
                center.clone().add(BlockFace.SOUTH.getDirection()),
                center.clone().add(BlockFace.WEST.getDirection()),
                center.clone().add(BlockFace.UP.getDirection()),
                center.clone().add(BlockFace.DOWN.getDirection())
        };
    }

    static Location[] getAround(Location center) {
        List<Location> locations = new ArrayList<>();
        for (int x = -1; x <= 1; x++)
            for (int y = -1; y <= 1; y++)
                for (int z = -1; z <= 1; z++)
                    if (!(x == 0 && y == 0 && z == 0))
                        locations.add(center.clone().add(x, y, z));
        return locations.toArray(new Location[0]);
    }

    public static class Narrower {
        final Fault fault;
        final List<Block> narrowList;

        public Narrower(Fault fault) {
            this.fault = fault;
            this.fault.restore();
            this.fault.processing = true;

            this.narrowList = this.fault.blocksToCompress();
        }

        public Narrower(Fault fault, List<Block> narrowList) {
            this.fault = fault;
            this.fault.processing = true;

            this.narrowList = narrowList;
        }

        public void run() {
            if (!narrowList.isEmpty()) {
                Bukkit.getScheduler().runTaskLater(AuraGameMaster.getInstance(), () -> {
                    Block block = narrowList.remove(0);
                    World world = block.getWorld();
                    Location location = block.getLocation().add(0.5, 0.5, 0.5);

                    world.spawnParticle(
                            Particle.BLOCK_CRACK, location, 50,
                            0.15, 0.15, 0.15,
                            block.getBlockData()
                    );
                    world.playSound(location, Sound.BLOCK_AMETHYST_BLOCK_BREAK, SoundCategory.BLOCKS, 1, 1);

                    fault.clearPart(block);

                    run();
                }, 20L);
            } else {
                fault.processing = false;

                if (fault.blocks.isEmpty()) {
                    fault.clearPart(fault.centerBlock);

                    FaultRepository repository = AuraGameMaster.getFaultRepository();
                    repository.remove(fault);

                    FileConfiguration config = repository.getConfig().getConfiguration();
                    for (Player player : fault.centerBlock.getLocation().getNearbyPlayers(
                            config.getDouble("find_range")
                    )) {
                        player.sendMessage(CoreConfig.getMessage(
                                config, "fault_closed"
                        ));
                    }
                }
            }
        }
    }

    public static class Extender {
        final Fault fault;
        final List<Block> extendList;

        public Extender(Fault fault) {
            this.fault = fault;
            this.fault.restore();
            this.fault.processing = true;

            this.extendList = this.fault.blocksToExtend();
        }

        @SneakyThrows
        public void run() {
            if (!extendList.isEmpty()) {
                Bukkit.getScheduler().runTaskLater(AuraGameMaster.getInstance(), () -> {
                    Block block = extendList.remove(0);
                    World world = block.getWorld();
                    Location location = block.getLocation().add(0.5, 0.5, 0.5);

                    world.spawnParticle(
                            Particle.ASH, location, 50,
                            0.5, 0.5, 0.5
                    );
                    world.playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1, 1);

                    fault.addPart(block, "abyss");

                    run();
                }, 20L);
            } else {
                fault.processing = false;
            }
        }
    }

    public String toString() {
        return toFindString() + aura;
    }

    public String toFindString() {
        return FaultRepository.stringfyLocation(centerBlock.getLocation()) + "|" + FaultRepository.stringfyLocation(exitLocation) + "|";
    }
}
