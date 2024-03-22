package net.aniby.aura.configuration;

import lombok.AccessLevel;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.AuraBackend;
import net.aniby.aura.util.AuraConfig;
import net.aniby.aura.tool.AuraUtils;
import net.aniby.aura.util.ShopConfig;
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

    @Bean
    @SneakyThrows
    @Scope("singleton")
    public ShopConfig createShopConfig() {
        File file = new File("shop.yml");
        AuraUtils.saveDefaultFile(file, "shop.yml", AuraBackend.class);
        return new ShopConfig(file);
    }
}
