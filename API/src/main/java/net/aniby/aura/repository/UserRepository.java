package net.aniby.aura.repository;

import lombok.SneakyThrows;
import net.aniby.aura.AuraAPI;
import net.aniby.aura.module.CAuraUser;

public class UserRepository {
    // Methods
    @SneakyThrows
    public int delete(CAuraUser user) {
        return AuraAPI.getDatabase().getUsers().delete(user);
    }

    @SneakyThrows
    public void update(CAuraUser user) {
        if (user.getId() > 0)
            AuraAPI.getDatabase().getUsers().update(user);
        else
            AuraAPI.getDatabase().getUsers().create(user);
    }
}
