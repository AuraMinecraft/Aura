package net.aniby.aura.configuration;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.File;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfigConfiguration {
    @Bean
    @SneakyThrows
    @Scope("singleton")
    public AuraConfig createConfig() {
        return new AuraConfig(new File("config.yml"));
    }
}
