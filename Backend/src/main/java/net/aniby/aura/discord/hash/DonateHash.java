package net.aniby.aura.discord.hash;

import lombok.SneakyThrows;
import net.aniby.aura.entity.AuraDonate;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.mysql.AuraDatabase;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

public class DonateHash extends StatisticHash {

    public DonateHash(AuraDatabase database, int limit) {
        super(database, limit, new Date().getTime());
    }

    @SneakyThrows
    public boolean updateIfNeed() {
        if (new Date().getTime() - this.getLastUpdate() >= Duration.ofHours(1).toMillis()) {
            List<AuraDonate> donates = this.getDatabase().getDonates().queryForAll();

            HashMap<AuraUser, Double> map = new HashMap<>();

            Set<AuraUser> donators = donates.stream().map(AuraDonate::getUser).collect(Collectors.toSet());
            for (AuraUser donator : donators) {
                double rubles = donates.stream().filter(d -> d.getUser() == donator)
                        .mapToDouble(AuraDonate::getAmount).sum();
                map.put(donator, rubles);
            }
            if (!map.isEmpty()) {
                int maxIndex = Math.min(10, map.size());
                this.getList().clear();
                this.getList().addAll(
                        map.entrySet()
                                .stream().sorted(Map.Entry.comparingByValue()).toList()
                                .subList(0, maxIndex)
                );

                return true;
            }
        }
        return false;
    }
}
