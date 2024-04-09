package net.aniby.aura.dependencies;

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.jdbc.db.DatabaseTypeUtils;
import com.j256.ormlite.support.ConnectionSource;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public enum DatabaseLibrary {
    MYSQL(
            BaseLibrary.MYSQL,
            (classLoader, jdbc, user, password)
                    -> fromDriver(classLoader.loadClass("com.mysql.cj.jdbc.NonRegisteringDriver"), jdbc, user, password, true),
            (hostname, database) ->
                    "jdbc:mysql://" + hostname + "/" + database
    ),
    MARIADB(
            BaseLibrary.MARIADB,
            (classLoader, jdbc, user, password)
                    -> fromDriver(classLoader.loadClass("org.mariadb.jdbc.Driver"), jdbc, user, password, true),
            (hostname, database) ->
                    "jdbc:mariadb://" + hostname + "/" + database
    ),
    POSTGRESQL(
            BaseLibrary.POSTGRESQL,
            (classLoader, jdbc, user, password) -> fromDriver(classLoader.loadClass("org.postgresql.Driver"), jdbc, user, password, true),
            (hostname, database) -> "jdbc:postgresql://" + hostname + "/" + database
    );

    private final BaseLibrary baseLibrary;
    private final DatabaseConnector connector;
    private final DatabaseStringGetter stringGetter;

    DatabaseLibrary(BaseLibrary baseLibrary, DatabaseConnector connector, DatabaseStringGetter stringGetter) {
        this.baseLibrary = baseLibrary;
        this.connector = connector;
        this.stringGetter = stringGetter;
    }

    public Connection connect(ClassLoader classLoader, String hostname, String database, String user, String password)
            throws ReflectiveOperationException, SQLException, IOException {
        return this.connect(classLoader, this.stringGetter.getJdbcString(hostname, database), user, password);
    }

    public Connection connect(String hostname, String database, String user, String password)
            throws ReflectiveOperationException, SQLException, IOException {
        return this.connect(this.stringGetter.getJdbcString(hostname, database), user, password);
    }

    public Connection connect(ClassLoader classLoader, String jdbc, String user, String password)
            throws ReflectiveOperationException, SQLException, IOException {
        return this.connector.connect(classLoader, jdbc, user, password);
    }

    public Connection connect(String jdbc, String user, String password) throws IOException, ReflectiveOperationException, SQLException {
        return this.connector.connect(new IsolatedClassLoader(new URL[]{this.baseLibrary.getClassLoaderURL()}), jdbc, user, password);
    }

    public ConnectionSource connectToORM(String hostname, String database, String user, String password)
            throws ReflectiveOperationException, IOException, SQLException {
        String jdbc = this.stringGetter.getJdbcString(hostname, database);
        ClassLoader currentClassLoader = DatabaseLibrary.class.getClassLoader();

        this.connect(currentClassLoader, jdbc, user, password).close(); // Load database driver (Will be rewritten soon)
        boolean h2 = this.baseLibrary == BaseLibrary.H2_V1 || this.baseLibrary == BaseLibrary.H2_V2;
        return new JdbcPooledConnectionSource(jdbc, h2 ? null : user, h2 ? null : password, DatabaseTypeUtils.createDatabaseType(jdbc));
    }

    private static Connection fromDriver(Class<?> connectionClass, String jdbc, String user, String password, boolean register)
            throws ReflectiveOperationException, SQLException {
        Constructor<?> legacyConstructor = connectionClass.getConstructor();

        Properties info = new Properties();
        if (user != null) {
            info.put("user", user);
        }

        if (password != null) {
            info.put("password", password);
        }

        Object driver = legacyConstructor.newInstance();

        DriverManager.deregisterDriver((Driver) driver);
        if (register) {
            DriverManager.registerDriver((Driver) driver);
        }

        Method connect = connectionClass.getDeclaredMethod("connect", String.class, Properties.class);
        connect.setAccessible(true);
        return (Connection) connect.invoke(driver, jdbc, info);
    }

    public interface DatabaseConnector {
        Connection connect(ClassLoader classLoader, String jdbc, String user, String password)
                throws ReflectiveOperationException, SQLException, IOException;
    }

    public interface DatabaseStringGetter {
        String getJdbcString(String hostname, String database);
    }
}