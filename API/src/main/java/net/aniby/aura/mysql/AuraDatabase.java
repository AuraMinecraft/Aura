package net.aniby.aura.mysql;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.entity.AuraDonate;

import javax.annotation.Nonnull;
import java.sql.SQLException;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuraDatabase {
    final @Nonnull DatabaseConnectionData connectionData;
    ConnectionSource connectionSource;
    @Getter
    Dao<AuraUser, Integer> users;
    @Getter
    Dao<AuraDonate, Integer> donates;

    public AuraDatabase(String url, String username, String password) throws SQLException {
        this.connectionData = new DatabaseConnectionData(url, username, password);
        this.connect();
    }

    public void connect() throws SQLException {
        this.connectionSource = this.connectionData.connectionSource();
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
