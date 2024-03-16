package net.aniby.aura;

import com.google.common.reflect.TypeToken;
import lombok.Getter;
import net.aniby.aura.tool.Replacer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class AuraConfig {
    private final YAMLConfigurationLoader loader;

    private ConfigurationNode root;

    public AuraConfig(File file) throws IOException {
        this.loader = YAMLConfigurationLoader.builder().setPath(file.toPath()).build();
        System.out.println(file.getAbsolutePath());
        load();
    }

    public void load() throws IOException {
        this.root = this.loader.load();
    }

    public String getMessage(String name, List<Replacer> replaces) {
        String message = this.root.getNode("messages", name).getString();
        return replaceMessage(message, replaces);
    }

    public String replaceMessage(String message, List<Replacer> replaces) {
        if (message == null)
            return null;
        if (replaces != null)
            for (Replacer replacer : replaces)
                message = replacer.replace(message);
        return message;
    }

    public String replaceMessage(String message, @NotNull Replacer... replaces) {
        return replaceMessage(message, Arrays.stream(replaces).toList());
    }

    public String getMessage(String name, @NotNull Replacer... replaces) {
        return getMessage(name, Arrays.stream(replaces).toList());
    }

    public MessageEmbed getEmbed(ConfigurationNode node, @NotNull Replacer... replaces) {
        return getEmbed(node, Arrays.stream(replaces).toList());
    }

    public MessageEmbed getEmbed(String name, @NotNull Replacer... replaces) {
        return getEmbed(this.root.getNode("embeds", name), Arrays.stream(replaces).toList());
    }

    public MessageEmbed getEmbed(String name, @NotNull List<Replacer> replaces) {
        return getEmbed(this.root.getNode("embeds", name), replaces);
    }

    public MessageEmbed getEmbed(ConfigurationNode node, @NotNull List<Replacer> replaces) {
        EmbedBuilder builder = new EmbedBuilder();

        builder = builder.setTitle(replaceMessage(node.getNode("title").getString(null), replaces));
        builder = builder.setUrl(replaceMessage(node.getNode("url").getString(null), replaces));
        builder = builder.setDescription(replaceMessage(node.getNode("description").getString(""), replaces));
        String timestamp = node.getNode("timestamp").getString();
        builder = builder.setTimestamp(timestamp == null ? null : OffsetDateTime.parse(timestamp));
        int color = node.getNode("color").getInt(-1);
        builder = builder.setColor(color == -1 ? Role.DEFAULT_COLOR_RAW : color);

        ConfigurationNode thumbnail = node.getNode("thumbnail");
        if (!thumbnail.isEmpty()) {
            builder = builder.setThumbnail(
                    replaceMessage(thumbnail.getNode("url").getString(null), replaces)
            );
        }

        ConfigurationNode author = node.getNode("author");
        if (!author.isEmpty()) {
            builder = builder.setAuthor(
                    replaceMessage(author.getNode("name").getString(""), replaces),
                    replaceMessage(author.getNode("url").getString(null), replaces),
                    replaceMessage(author.getNode("icon_url").getString(null), replaces)
            );
        }

        ConfigurationNode footer = node.getNode("footer");
        if (!footer.isEmpty()) {
            builder = builder.setFooter(
                    replaceMessage(footer.getNode("text").getString(""), replaces),
                    replaceMessage(footer.getNode("icon_url").getString(null), replaces)
            );
        }

        String imageURL = node.getNode("image", "url").getString();
        if (imageURL != null)
            builder = builder.setImage(
                    replaceMessage(imageURL, replaces)
            );

        try {
            List<ConfigurationNode> fields = node.getNode("fields").getList(TypeToken.of(ConfigurationNode.class));
            for (ConfigurationNode field : fields) {
                builder = builder.addField(
                        replaceMessage(field.getNode("name").getString(EmbedBuilder.ZERO_WIDTH_SPACE), replaces),
                        replaceMessage(field.getNode("value").getString(EmbedBuilder.ZERO_WIDTH_SPACE), replaces),
                        field.getNode("inline").getBoolean(false)
                );
            }
        } catch (Exception ignored) {}
        return builder.build();
    }

    public static Set<String> getNodeKeys(ConfigurationNode node) {
        return node.getChildrenMap().keySet().stream().map(o -> (String) o).collect(Collectors.toSet());
    }
}
