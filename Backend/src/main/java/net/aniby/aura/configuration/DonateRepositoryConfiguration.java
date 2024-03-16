package net.aniby.aura.configuration;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.repository.DonateRepository;
import net.aniby.aura.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DonateRepositoryConfiguration {
    @Autowired
    AuraDatabase database;

    @Autowired
    UserRepository userRepository;

    @Bean
    @Scope("singleton")
    public DonateRepository createDonateRepository() {
        return new DonateRepository(userRepository, database);
    }
}
