package net.aniby.aura.repository;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.mysql.AuraDatabase;

import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserRepository {
    AuraDatabase database;

    @SneakyThrows
    public AuraUser findByPlayerName(String playerName) {
        return database.getUsers().queryBuilder()
                .where()
                .eq("player_name", playerName)
                .queryForFirst();
    }

    @SneakyThrows
    public AuraUser findByWhitelistedPlayerName(String playerName) {
        return database.getUsers().queryBuilder()
                .where()
                .eq("player_name", playerName)
                .and()
                .eq("whitelisted", true)
                .queryForFirst();
    }

    @SneakyThrows
    public List<AuraUser> findWhitelistedPlayers() {
        return database.getUsers().queryBuilder()
                .where()
                .eq("whitelisted", true)
                .query();
    }

    @SneakyThrows
    public int delete(AuraUser user) {
        return database.getUsers().delete(user);
    }

    @SneakyThrows
    public void update(AuraUser user) {
        if (user.getId() > 0)
            database.getUsers().update(user);
        else
            database.getUsers().create(user);
    }
}
