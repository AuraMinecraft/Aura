package net.aniby.aura.discord.hash;

import lombok.SneakyThrows;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.mysql.AuraDatabase;

import java.time.Duration;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class AuraHash extends StatisticHash {

    public AuraHash(AuraDatabase database, int limit) {
        super(database, limit, new Date().getTime());
    }

    @SneakyThrows
    public boolean updateIfNeed() {
        if (new Date().getTime() - this.getLastUpdate() >= Duration.ofHours(1).toMillis()) {
            List<AuraUser> users = this.getDatabase().getUsers().queryForAll()
                    .stream().sorted(Comparator.comparingDouble(AuraUser::getAura))
                    .toList();
            if (!users.isEmpty()) {
                int maxIndex = Math.min(10, users.size());
                this.getList().clear();
                this.getList().addAll(users.subList(0, maxIndex));
                return true;
            }
        }
        return false;
    }
}
