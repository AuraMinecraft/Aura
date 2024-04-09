package net.aniby.aura.entity;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@DatabaseTable(tableName = "donates")
public class AuraDonate {
    @DatabaseField(generatedId = true)
    int id;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = "user_id", canBeNull = false)
    AuraUser user;

    @DatabaseField(canBeNull = false)
    double amount;

    @DatabaseField(columnName = "earned_aura", canBeNull = false, defaultValue = "0.0")
    double earnedAura;

    @DatabaseField(uniqueIndex = true, canBeNull = false)
    long timestamp;
}
