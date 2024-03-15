package net.aniby.aura.configuration;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.repository.UserRepository;
import net.aniby.aura.service.DiscordLoggerService;
import net.aniby.aura.service.DiscordService;
import net.aniby.aura.service.UserService;
import net.aniby.aura.twitch.TwitchIRC;
import ninja.leaping.configurate.ConfigurationNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TwitchConfiguration {
    @Autowired
    AuraConfig config;
    @Autowired
    UserService userService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    DiscordLoggerService loggerService;
    @Autowired
    DiscordService discordService;

    @Bean
    @SneakyThrows
    @Scope("singleton")
    public TwitchIRC createTwitchIRC() {
        ConfigurationNode node = config.getRoot().getNode("twitch", "application");
        return new TwitchIRC(
                config,
                userService,
                userRepository,
                loggerService,
                discordService,
                node.getNode("client_id").getString(),
                node.getNode("client_secret").getString(),
                getRedirectURI()
        );
    }

    private String getRedirectURI() {
        String url = config.getRoot().getNode("http_server", "external_url").getString();
        if (!url.endsWith("/"))
            url += "/";
        return url + "link/twitch/";
    }
}
