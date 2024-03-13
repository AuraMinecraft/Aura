package net.aniby.aura.module;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import net.aniby.aura.AuraAPI;
import net.aniby.aura.tool.Replacer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static net.aniby.aura.tool.Replacer.r;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@DatabaseTable(tableName = "donates")
public class AuraDonate {
    @DatabaseField(generatedId = true, canBeNull = false)
    private int id;
    @DatabaseField(columnName = "discord_id", index = true)
    private @NotNull String discordId = "";
    @DatabaseField
    private double amount = 0;
    @DatabaseField(columnName = "earned_aura")
    private double earnedAura = 0;
    @DatabaseField(uniqueIndex = true)
    private long timestamp = 0;

    public List<Replacer> getReplacers() {
        return new ArrayList<>(List.of(
                r("donate_amount", amount)
        ));
    }

    @SneakyThrows
    public void save() {
        Dao<AuraDonate, Integer> dao = AuraAPI.getDatabase().getDonates();
        if (this.id > 0)
            dao.update(this);
        else
            dao.create(this);
    }

    @SneakyThrows
    public void delete() {
        AuraAPI.getDatabase().getDonates().delete(this);

        CAuraUser user = AuraAPI.getDatabase().getUsers()
                .queryBuilder()
                .where()
                .eq("discord_id", this.discordId)
                .queryForFirst();
        if (user != null) {
            user.setAura(user.getAura() - this.earnedAura);
            user.save();
        }
    }
}
