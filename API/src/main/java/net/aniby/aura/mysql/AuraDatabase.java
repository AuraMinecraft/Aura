package net.aniby.aura.mysql;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.dependencies.DatabaseLibrary;
import net.aniby.aura.entity.AuraDonate;
import net.aniby.aura.entity.AuraUser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.sql.SQLException;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuraDatabase {
    final ConnectionSource connectionSource;
    @Getter
    final Dao<AuraUser, Integer> users;
    @Getter
    final Dao<AuraDonate, Integer> donates;

    public AuraDatabase(String hostname, String database, String user, String password, String connectionParameters) throws SQLException, ReflectiveOperationException, IOException, URISyntaxException {
        this.connectionSource = DatabaseLibrary.MYSQL.connectToORM(
                hostname,
                database + connectionParameters,
                user,
                password
        );
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
            try {
                TableUtils.createTableIfNotExists(this.connectionSource, AuraUser.class);
            } catch (Exception ignored) {}
        }
        try {
            this.donates.queryForFirst();
        } catch (Exception e) {
            try {
                TableUtils.createTableIfNotExists(this.connectionSource, AuraDonate.class);
            } catch (Exception ignored) {}
        }
    }
}
