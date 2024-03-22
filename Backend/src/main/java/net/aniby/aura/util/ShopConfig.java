package net.aniby.aura.util;

import com.google.common.reflect.TypeToken;
import lombok.Getter;
import lombok.SneakyThrows;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ShopConfig {
    private final YAMLConfigurationLoader loader;

    private ConfigurationNode root;
    private final ArrayList<ShopGood> goods = new ArrayList<>();

    public ShopConfig(File file) throws IOException {
        this.loader = YAMLConfigurationLoader.builder().setPath(file.toPath()).build();
        load();
    }

    public void load() throws IOException {
        this.goods.clear();
        this.root = this.loader.load();

        for (String nodeKey : AuraConfig.getNodeKeys(this.root)) {
            ConfigurationNode node = this.root.getNode(nodeKey);
            this.goods.add(fromConfig(node));
        }
    }

    @SneakyThrows
    public ShopGood fromConfig(ConfigurationNode node) {
        return new ShopGood(
                node.getNode("name").getString(),
                node.getNode("value").getString(),
                node.getNode("cost").getInt(),
                node.getNode("minecraft", "commands").getList(TypeToken.of(String.class)),
                node.getNode("discord", "roles").getList(TypeToken.of(String.class))
        );
    }
}
