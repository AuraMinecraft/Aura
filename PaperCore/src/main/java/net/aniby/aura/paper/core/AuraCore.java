package net.aniby.aura.core;

import lombok.Getter;
import lombok.SneakyThrows;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.repository.UserRepository;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class AuraCore extends JavaPlugin {
    @Getter
    private static AuraCore instance;
    private AuraDatabase database;
    private UserRepository userRepository;

    @SneakyThrows
    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        FileConfiguration config = getConfig();

        ConfigurationSection dbSection = config.getConfigurationSection("mysql");
        this.database = new AuraDatabase(
                dbSection.getString("url"),
                dbSection.getString("login"),
                dbSection.getString("password")
        );
        this.userRepository = new UserRepository(database);
    }

    @Override
    public void onDisable() {
        this.database.disconnect();
    }
}
