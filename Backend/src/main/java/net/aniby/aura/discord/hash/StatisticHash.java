package net.aniby.aura.discord.hash;

import lombok.*;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.mysql.AuraDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class StatisticHash {
    ArrayList<Object> list = new ArrayList<>();
    AuraDatabase database;
    int limit;
    long lastUpdate;

    @SneakyThrows
    public boolean updateIfNeed() {
        return true;
    }
}
