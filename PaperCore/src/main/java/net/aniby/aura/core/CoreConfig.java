package net.aniby.aura.core;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.configuration.file.FileConfiguration;

public class CoreConfig {
    public static String getPlainMessage(FileConfiguration configuration, String path) {
        return configuration.getConfigurationSection("messages")
                .getString(path);
    }

    public static Component getMessage(FileConfiguration configuration, String path, TagResolver... tags) {
        return miniMessage.deserialize(
                configuration.getConfigurationSection("messages")
                        .getString(path),
                tags
        );
    }

    @Getter
    private static final MiniMessage miniMessage = MiniMessage.builder()
            .tags(StandardTags.defaults())
            .build();

    public static String getPlainMessage(String path) {
        return CoreConfig.getPlainMessage(AuraCore.getInstance().getConfig(), path);
    }

    public static Component getMessage(String path, TagResolver... tags) {
        return CoreConfig.getMessage(AuraCore.getInstance().getConfig(), path, tags);
    }
}
