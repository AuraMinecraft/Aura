package net.aniby.aura.gamemaster;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import lombok.SneakyThrows;
import net.aniby.aura.gamemaster.commands.EventCommand;
import net.aniby.aura.gamemaster.commands.HighlightCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuraGameMaster extends JavaPlugin {
    @Getter
    private static AuraGameMaster instance;

    public static Component getMessage(String path, TagResolver... tags) {
        return miniMessage.deserialize(
                instance.getConfig().getConfigurationSection("messages")
                        .getString(path),
                tags
        );
    }

    @Getter
    private static final MiniMessage miniMessage = MiniMessage.builder()
            .tags(StandardTags.defaults())
            .build();

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

        PaperCommandManager manager = new PaperCommandManager(this);
        manager.registerCommand(new EventCommand());
        if (pluginManager.getPlugin("GlowAPI") != null)
            manager.registerCommand(new HighlightCommand());
    }
}
