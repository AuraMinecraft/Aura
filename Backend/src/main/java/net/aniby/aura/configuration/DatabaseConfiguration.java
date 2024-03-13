package net.aniby.aura.configuration;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.repository.DonateRepository;
import net.aniby.aura.repository.UserRepository;
import ninja.leaping.configurate.ConfigurationNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.File;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DatabaseConfiguration {
    @Autowired
    AuraDatabase database;

    @Autowired
    AuraConfig config;

    @Autowired
    UserRepository userRepository;

    @Bean
    @SneakyThrows
    @Scope("singleton")
    public AuraDatabase createDatabase() {
        ConfigurationNode node = config.getRoot().getNode("mysql");
        return new AuraDatabase(
                node.getNode("url").getString(),
                node.getNode("login").getString(),
                node.getNode("password").getString()
        );
    }


    @Bean
    @SneakyThrows
    @Scope("singleton")
    public AuraConfig createConfig() {
        return new AuraConfig(new File("config.yml"));
    }

    @Bean
    @Scope("singleton")
    public UserRepository createUserRepository() {
        return new UserRepository(database);
    }

    @Bean
    @Scope("singleton")
    public DonateRepository createDonateRepository() {
        return new DonateRepository(userRepository, database);
    }
}
