package net.aniby.aura.core;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import lombok.SneakyThrows;
import net.aniby.aura.core.command.AuraCommand;
import net.aniby.aura.core.command.AuraReload;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.repository.TransactionRepository;
import net.aniby.aura.repository.UserRepository;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class AuraCore extends JavaPlugin {
    @Getter
    private static AuraCore instance;
    private AuraDatabase database;
    private UserRepository userRepository;
    private TransactionRepository transactionRepository;

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
//        commandManager.registerCommand(new PayCommand());
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
        this.transactionRepository = new TransactionRepository(database);
    }

    public void loadWhitelist() {
        this.userRepository.findWhitelistedPlayers()
                .stream().map(AuraUser::getPlayerName)
                .forEach(o -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "simplewhitelist add " + o));
    }

    @Override
    public void onDisable() {
        this.database.disconnect();
    }
}
