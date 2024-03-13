package net.aniby.aura;

import lombok.Getter;
import lombok.SneakyThrows;
import net.aniby.aura.mysql.AuraDatabase;

public class AuraAPI {
    @Getter
    static AuraDatabase database;

    @SneakyThrows
    public static void init(String databaseURL, String username, String password) {
        database = new AuraDatabase(databaseURL, username, password);
        database.createTables();
    }
}