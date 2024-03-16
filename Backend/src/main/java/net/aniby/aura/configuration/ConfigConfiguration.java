package net.aniby.aura.configuration;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.tool.AuraUtils;
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
        File file = new File("config.yml");
        AuraUtils.saveDefaultFile(file, "config.yml", AuraBackend.class);
        return new AuraConfig(file);
    }
}
