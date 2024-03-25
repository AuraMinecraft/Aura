package net.aniby.aura.gamemaster;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import lombok.SneakyThrows;
import net.aniby.aura.core.CoreConfig;
import net.aniby.aura.gamemaster.common.command.EventCommand;
import net.aniby.aura.gamemaster.common.command.HighlightCommand;
import net.aniby.aura.gamemaster.common.command.VisibilityCommand;
import net.aniby.aura.gamemaster.event.abyss.AbyssCommand;
import net.aniby.aura.gamemaster.event.abyss.AbyssManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public final class AuraGameMaster extends JavaPlugin {
    @Getter
    private static AuraGameMaster instance;
    @Getter
    private static AbyssManager abyss;

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
        if (pluginManager.getPlugin("GlowAPI") != null)
            commandManager.registerCommand(new HighlightCommand());
        commandManager.registerCommand(new VisibilityCommand());

        // =========== Events
        // Abyss
        File abyssFile = createResourceFile("events/abyss.yml");
        abyss = new AbyssManager(this, loadCustomConfig(abyssFile), abyssFile);

        commandManager.registerCommand(new AbyssCommand());
    }

    private File createResourceFile(String path) {
        File file = new File(this.getDataFolder(), path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            this.saveResource(path, false);
        }
        return file;
    }

    private FileConfiguration loadCustomConfig(File file) {
        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return config;
    }
}
