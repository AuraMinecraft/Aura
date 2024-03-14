package net.aniby.aura.configuration;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraConfig;
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

    @Bean
    @SneakyThrows
    @Scope("singleton")
    public TwitchIRC createTwitchBot() {
        ConfigurationNode node = config.getRoot().getNode("twitch", "application");
        return new TwitchIRC(
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
