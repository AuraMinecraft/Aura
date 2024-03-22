package net.aniby.aura.velocity;

import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class VelocityConfig {
    private final YAMLConfigurationLoader loader;

    private ConfigurationNode root;

    private final MiniMessage miniMessage = MiniMessage.builder()
            .tags(StandardTags.defaults())
            .build();

    public VelocityConfig(File file) throws IOException {
        this.loader = YAMLConfigurationLoader.builder().setPath(file.toPath()).build();
        load();
    }

    public void load() throws IOException {
        this.root = this.loader.load();
    }

    public Component getMessage(String name, TagResolver... tags) {
        String message = this.root.getNode("messages", name).getString();
        return miniMessage.deserialize(message, tags);
    }

    public Component getMessage(String message, List<TagResolver> tags) {
        return miniMessage.deserialize(message, tags.toArray(new TagResolver[0]));
    }

    public static Set<String> getNodeKeys(ConfigurationNode node) {
        return node.getChildrenMap().keySet().stream().map(o -> (String) o).collect(Collectors.toSet());
    }
}
