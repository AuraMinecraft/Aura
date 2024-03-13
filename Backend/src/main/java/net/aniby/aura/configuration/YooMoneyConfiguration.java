package net.aniby.aura.configuration;

import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.AuraConfig;
import net.aniby.aura.tool.AuraCache;
import net.aniby.aura.tool.AuraUtils;
import net.aniby.yoomoney.client.YooMoneyClient;
import ninja.leaping.configurate.ConfigurationNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.io.File;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class YooMoneyConfiguration {
    @Bean
    @SneakyThrows
    @Scope("singleton")
    public AuraCache createCache() {
        File cacheFile = new File("cache/yoomoney.cache");
        AuraUtils.saveDefaultFile(cacheFile, "cache/yoomoney.cache", AuraBackend.class);
        return new AuraCache(cacheFile);
    }
}
