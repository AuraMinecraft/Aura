package net.aniby.aura.gamemaster;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import lombok.SneakyThrows;
import net.aniby.aura.core.CoreConfig;
import net.aniby.aura.gamemaster.common.command.EventCommand;
import net.aniby.aura.gamemaster.common.command.VisibilityCommand;
import net.aniby.aura.gamemaster.event.abyss.AbyssCommand;
import net.aniby.aura.gamemaster.event.abyss.AbyssListener;
import net.aniby.aura.gamemaster.event.abyss.AbyssManager;
import net.aniby.aura.gamemaster.event.fault.FaultCommand;
import net.aniby.aura.gamemaster.event.fault.FaultListener;
import net.aniby.aura.gamemaster.event.fault.FaultRepository;
import net.aniby.aura.gamemaster.event.fault.InputCommand;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuraGameMaster extends JavaPlugin {
    @Getter
    private static AuraGameMaster instance;
    @Getter
    private static AbyssManager abyss;
    @Getter
    private static FaultRepository faultRepository;

    public static String getPlainMessage(String path) {
        return CoreConfig.getPlainMessage(instance.getConfig(), path);
    }

    public static Component getMessage(String path, TagResolver... tags) {
        return CoreConfig.getMessage(instance.getConfig(), path, tags);
    }

    @Override
    @SneakyThrows
    public void onEnable() {
        saveDefaultConfig();

        PluginManager pluginManager = this.getServer().getPluginManager();
        if (pluginManager.getPlugin("AuraCore") == null) {
            getLogger().info("AuraCore is needed to start this plugin!");
            pluginManager.disablePlugin(this);
            return;
        }

        instance = this;

        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new EventCommand());
        commandManager.registerCommand(new VisibilityCommand());

        // =========== Events
        // Abyss
        CustomConfig abyssConfig = new CustomConfig("events/abyss.yml", this);
        abyss = new AbyssManager(this, abyssConfig);
        getServer().getPluginManager().registerEvents(new AbyssListener(), this);
        commandManager.registerCommand(new AbyssCommand());

        // Faults
        CustomConfig faultsConfig = new CustomConfig("events/faults.yml", this);
        faultRepository = new FaultRepository(this, faultsConfig);
        faultRepository.loadAll();
        getServer().getPluginManager().registerEvents(new FaultListener(), this);
        commandManager.registerCommand(new FaultCommand());
        commandManager.registerCommand(new InputCommand());
    }


    public CoreProtectAPI getCoreProtect() {
        Plugin plugin = getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (!(plugin instanceof CoreProtect)) {
            return null;
        }

        // Check that the API is enabled
        CoreProtectAPI CoreProtect = ((CoreProtect) plugin).getAPI();
        if (!CoreProtect.isEnabled()) {
            return null;
        }

        // Check that a compatible version of the API is loaded
        if (CoreProtect.APIVersion() < 9) {
            return null;
        }

        return CoreProtect;
    }

    @Override
    public void onDisable() {
        faultRepository.getTimer().cancel();
    }
}
