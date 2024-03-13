package net.aniby.aura.mysql;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import lombok.Getter;
import net.aniby.aura.module.CAuraUser;
import net.aniby.aura.module.AuraDonate;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class AuraDatabase {
    private final @NotNull ConnectionSource connectionSource;
    @Getter
    private final @NotNull Dao<? extends CAuraUser, Integer> users;
    @Getter
    private final @NotNull Dao<AuraDonate, Integer> donates;

    public <T extends CAuraUser> AuraDatabase(String url, String username, String password, Class<T> userClass) throws SQLException {
        this.connectionSource = new JdbcConnectionSource(url, username, password);
        this.users = DaoManager.createDao(
                this.connectionSource,
                userClass
        );
        this.donates = DaoManager.createDao(
                this.connectionSource,
                AuraDonate.class
        );
    }

    public void disconnect() {
        this.connectionSource.closeQuietly();
    }

    public void createTables() throws SQLException {
        try {
            this.users.queryForFirst();
        } catch (Exception e) {
            TableUtils.createTableIfNotExists(this.connectionSource, CAuraUser.class);
        }
        try {
            this.donates.queryForFirst();
        } catch (Exception e) {
            TableUtils.createTableIfNotExists(this.connectionSource, AuraDonate.class);
        }
    }
}
