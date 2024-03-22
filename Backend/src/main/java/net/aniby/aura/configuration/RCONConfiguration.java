package net.aniby.aura.configuration;

import io.graversen.minecraft.rcon.service.ConnectOptions;
import io.graversen.minecraft.rcon.service.MinecraftRconService;
import io.graversen.minecraft.rcon.service.RconDetails;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.util.AuraConfig;
import ninja.leaping.configurate.ConfigurationNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.time.Duration;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RCONConfiguration {
    @Autowired
    AuraConfig config;

    @Bean
    @Scope("singleton")
    public MinecraftRconService createMinecraftRcon() {
        ConfigurationNode node = config.getRoot().getNode("rcon");
        MinecraftRconService rconService = new MinecraftRconService(
                new RconDetails(
                        node.getNode("host").getString(),
                        node.getNode("port").getInt(),
                        node.getNode("password").getString()
                ),
                ConnectOptions.neverStopTrying()
        );
        rconService.connectBlocking(Duration.ofSeconds(3));
        return rconService;
    }
}
