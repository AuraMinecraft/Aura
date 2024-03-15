package net.aniby.aura.configuration;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.service.DiscordLoggerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.File;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DiscordLoggerConfiguration {
    @Bean
    @SneakyThrows
    @Scope("singleton")
    public DiscordLoggerService createDiscordLoggerService() {
        return new DiscordLoggerService();
    }
}
