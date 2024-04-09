package net.aniby.aura.repository;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import net.aniby.aura.entity.AuraTransaction;
import net.aniby.aura.entity.AuraUser;
import net.aniby.aura.mysql.AuraDatabase;

import java.util.Date;
import java.util.List;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionRepository {
    AuraDatabase database;

    @SneakyThrows
    public void create(AuraUser sender, AuraUser receiver, double amount, String comment) {
        AuraTransaction transaction = new AuraTransaction(0, sender, receiver, amount, comment, new Date().getTime());

        database.getTransactions().create(transaction);
    }
}
