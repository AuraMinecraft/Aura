package net.aniby.aura.mysql;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

public record DatabaseConnectionData(String url, String username, String password) {
    public ConnectionSource connectionSource() throws SQLException {
        return new JdbcConnectionSource(url, username, password);
    }
}
