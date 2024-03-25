package net.aniby.aura.core;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import lombok.SneakyThrows;
import net.aniby.aura.core.command.AuraCommand;
import net.aniby.aura.core.command.AuraReload;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.repository.UserRepository;
import net.kyori.adventure.text.EntityNBTComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
public final class AuraCore extends JavaPlugin {
    @Getter
    private static AuraCore instance;
    private AuraDatabase database;
    private UserRepository userRepository;

    @Override
    public void onEnable() {
        // Config
        instance = this;
        saveDefaultConfig();

        // Database
        connectDatabase();

        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new AuraCommand());
        commandManager.registerCommand(new AuraReload());
    }

    @SneakyThrows
    public void connectDatabase() {
        ConfigurationSection dbSection = this.getConfig().getConfigurationSection("mysql");
        this.database = new AuraDatabase(
                dbSection.getString("host"),
                dbSection.getString("database"),
                dbSection.getString("user"),
                dbSection.getString("password"),
                dbSection.getString("parameters")
        );
        this.userRepository = new UserRepository(database);
    }

    public void loadWhitelist() {
        Bukkit.setWhitelist(true);
        Bukkit.getWhitelistedPlayers().clear();
        this.userRepository.findWhitelistedPlayers()
                .stream().map(u -> Bukkit.getOfflinePlayer(u.getPlayerName()))
                .forEach(o -> o.setWhitelisted(true));
        Bukkit.reloadWhitelist();
    }

    @Override
    public void onDisable() {
        this.database.disconnect();
    }
}
