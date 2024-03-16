package net.aniby.aura.mysql;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import lombok.Getter;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.entity.AuraDonate;

import javax.annotation.Nonnull;
import java.sql.SQLException;

public class AuraDatabase {
    private final @Nonnull ConnectionSource connectionSource;
    @Getter
    private final @Nonnull Dao<AuraUser, Integer> users;
    @Getter
    private final @Nonnull Dao<AuraDonate, Integer> donates;

    public AuraDatabase(String url, String username, String password) throws SQLException {
        this.connectionSource = new JdbcConnectionSource(url, username, password);
        this.users = DaoManager.createDao(
                this.connectionSource,
                AuraUser.class
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
            TableUtils.createTableIfNotExists(this.connectionSource, AuraUser.class);
        }
        try {
            this.donates.queryForFirst();
        } catch (Exception e) {
            TableUtils.createTableIfNotExists(this.connectionSource, AuraDonate.class);
        }
    }
}
