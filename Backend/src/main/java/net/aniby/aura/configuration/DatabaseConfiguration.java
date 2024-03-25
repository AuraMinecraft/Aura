package net.aniby.aura.configuration;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.util.AuraConfig;
import ninja.leaping.configurate.ConfigurationNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DatabaseConfiguration {
    @Autowired
    AuraConfig config;

    @Bean
    @SneakyThrows
    @Scope("singleton")
    public AuraDatabase createDatabase() {
        ConfigurationNode node = config.getRoot().getNode("mysql");
        AuraDatabase database = new AuraDatabase(
                node.getNode("host").getString(),
                node.getNode("database").getString(),
                node.getNode("user").getString(),
                node.getNode("password").getString(),
                node.getNode("parameters").getString()
        );
        database.createTables();
        return database;
    }
}
