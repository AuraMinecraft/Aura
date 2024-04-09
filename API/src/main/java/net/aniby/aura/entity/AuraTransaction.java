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
@DatabaseTable(tableName = "transactions")
public class AuraTransaction {
    @DatabaseField(generatedId = true)
    int id;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = "sender_id", canBeNull = false)
    AuraUser sender;

    @DatabaseField(foreign = true, foreignAutoRefresh = true, columnName = "receiver_id", canBeNull = false)
    AuraUser receiver;

    @DatabaseField(canBeNull = false)
    double amount;

    @DatabaseField
    String comment;

    @DatabaseField(unique = true, canBeNull = false)
    long timestamp;
}
