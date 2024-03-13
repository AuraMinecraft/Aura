package net.aniby.aura.gamemaster;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import net.aniby.aura.AuraAPI;
import net.aniby.aura.gamemaster.commands.EventMessageCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class GameMaster extends JavaPlugin {
    @Getter
    private static GameMaster instance;

    @Getter
    private static final MiniMessage miniMessage = MiniMessage.builder()
            .tags(TagResolver.standard())
            .build();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        FileConfiguration config = getConfig();

        ConfigurationSection dbSection = config.getConfigurationSection("mysql");
        AuraAPI.init(
                dbSection.getString("url"),
                dbSection.getString("login"),
                dbSection.getString("password")
        );

        PaperCommandManager manager = new PaperCommandManager(this);
        manager.registerCommand(new EventMessageCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
