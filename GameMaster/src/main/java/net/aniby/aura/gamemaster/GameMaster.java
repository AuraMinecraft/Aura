package net.aniby.aura.gamemaster;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import lombok.SneakyThrows;
import net.aniby.aura.gamemaster.commands.EventCommand;
import net.aniby.aura.gamemaster.commands.HighlightCommand;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.repository.UserRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class GameMaster extends JavaPlugin {
    @Getter
    private static GameMaster instance;
    @Getter
    private static AuraDatabase database;
    @Getter
    private static UserRepository userRepository;

    public static Component getMessage(String path, TagResolver... tags) {
        return miniMessage.deserialize(
                instance.getConfig().getConfigurationSection("messages")
                        .getString(path),
                tags
        );
    }

    @Getter
    private static final MiniMessage miniMessage = MiniMessage.builder()
            .tags(TagResolver.standard())
            .build();

    @Override
    @SneakyThrows
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        FileConfiguration config = getConfig();

        ConfigurationSection dbSection = config.getConfigurationSection("mysql");
        database = new AuraDatabase(
                dbSection.getString("url"),
                dbSection.getString("login"),
                dbSection.getString("password")
        );
        userRepository = new UserRepository(database);

        PaperCommandManager manager = new PaperCommandManager(this);
        manager.registerCommand(new EventCommand());
        if (this.getServer().getPluginManager().getPlugin("GlowAPI") != null)
            manager.registerCommand(new HighlightCommand());
    }

    @Override
    public void onDisable() {
        database.disconnect();
    }
}
