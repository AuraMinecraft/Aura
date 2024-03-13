package net.aniby.aura.configuration;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.repository.DonateRepository;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.twitch.TwitchBot;
import ninja.leaping.configurate.ConfigurationNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.File;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TwitchConfiguration {
    @Autowired
    AuraConfig config;

    @Bean
    @SneakyThrows
    @Scope("singleton")
    public TwitchBot createTwitchBot() {
        return new TwitchBot(config.getRoot().getNode("twitch", "application"));
    }
}
