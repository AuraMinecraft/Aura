package net.aniby.aura.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import lombok.SneakyThrows;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.tool.AuraUtils;
import net.aniby.aura.velocity.commands.AuraCommand;
import net.aniby.aura.velocity.event.ConnectListener;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import ninja.leaping.configurate.ConfigurationNode;
import org.slf4j.Logger;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

@Plugin(
        id = "auravelocity",
        name = "AuraVelocity",
        version = "0.1",
        description = "Velocity plugin for AuraMC",
        url = "aura.aniby.net",
        authors = {"An1by"}
)
@Getter
public class AuraVelocity {
    @Getter
    private static AuraVelocity instance;

    @Inject
    private final Logger logger;
    private VelocityConfig config;
    private UserRepository userRepository;
    private AuraDatabase database;
    private final ProxyServer server;
    private final HashMap<String, Long> commandCooldown = new HashMap<>();

    public boolean checkCooldown(CommandSource source) {
        String sourceName = source.getOrDefault(Identity.NAME, "UNKNOWN");
        if (commandCooldown.containsKey(sourceName)) {
            long leaved = new Date().getTime() - commandCooldown.get(sourceName);
            if (leaved > 0) {
                commandCooldown.remove(sourceName);
            } else {
                source.sendMessage(config.getMessage(
                        "command_cooldown", Placeholder.unparsed("cooldown",
                                String.valueOf(Math.round((float) leaved / 1000)))
                ));
                return false;
            }
        }
        long commandUsageCooldown =
                1000L * config.getRoot().getNode("command_usage_cooldown").getInt();
        commandCooldown.put(sourceName, commandUsageCooldown + new Date().getTime());
        return true;
    }

    @Inject
    public AuraVelocity(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    @SneakyThrows
    public void onProxyInitialization(ProxyInitializeEvent event) {
        instance = this;

        File file = new File("config.yml");
        AuraUtils.saveDefaultFile(file, "config.yml", AuraVelocity.class);
        config = new VelocityConfig(new File("config.yml"));

        ConfigurationNode node = config.getRoot().getNode("mysql");
        database = new AuraDatabase(
                node.getNode("url").getString(),
                node.getNode("login").getString(),
                node.getNode("password").getString()
        );
        userRepository = new UserRepository(database);

        CommandManager manager = server.getCommandManager();
        CommandMeta auraMeta = manager.metaBuilder("aura")
                .plugin(this)
                .build();
        manager.register(auraMeta, new AuraCommand());

        server.getEventManager().register(this, new ConnectListener());
    }
}
