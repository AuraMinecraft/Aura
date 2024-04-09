package net.aniby.aura.gamemaster;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomConfig {
    JavaPlugin plugin;
    File file;
    FileConfiguration configuration;

    public CustomConfig(String path, JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = createResourceFile(path);
        this.configuration = load(this.file);
    }

    private File createResourceFile(String path) {
        File file = new File(this.plugin.getDataFolder(), path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            this.plugin.saveResource(path, false);
        }
        return file;
    }

    public void save() throws IOException {
        this.configuration.save(this.file);
    }

    private FileConfiguration load(File file) {
        FileConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        return config;
    }
}
