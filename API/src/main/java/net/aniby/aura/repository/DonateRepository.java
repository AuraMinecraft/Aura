package net.aniby.aura.repository;

import com.j256.ormlite.dao.Dao;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.entity.AuraDonate;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.mysql.AuraDatabase;
import net.aniby.aura.tool.DiscordWebhook;
import net.aniby.aura.tool.Replacer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static net.aniby.aura.tool.Replacer.r;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DonateRepository {
    UserRepository userRepository;
    AuraDatabase database;

    public List<Replacer> getReplacers(AuraDonate donate) {
        return new ArrayList<>(List.of(
                r("donate_amount", donate.getAmount())
        ));
    }

    @SneakyThrows
    public void update(AuraDonate donate) {
        Dao<AuraDonate, Integer> dao = database.getDonates();
        if (donate.getId() > 0)
            dao.update(donate);
        else
            dao.create(donate);
    }

    @SneakyThrows
    public void delete(AuraDonate donate) {
        database.getDonates().delete(donate);

        AuraUser user = database.getUsers().queryForId(donate.getUser().getId());
        if (user != null) {
            user.setAura(user.getAura() - donate.getEarnedAura());
            userRepository.update(user);
        }
    }
}
