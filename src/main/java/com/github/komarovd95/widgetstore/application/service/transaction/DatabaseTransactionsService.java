package com.github.komarovd95.widgetstore.application.service.transaction;

import com.github.komarovd95.widgetstore.application.service.transaction.locks.ExclusiveLock;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An implementation of the {@link TransactionsService} that uses database transactions.
 * <p>
 * For write transactions it uses standard database transaction with exclusive access to WIDGET table in the database.
 * We achieve exclusive access to the given table via table locks (this implementation uses explicit locks stored in
 * WIDGET_LOCK table and acquired by SELECT ... FOR UPDATE statement). Finally, it gives us serialized access to the
 * WIDGET table.
 * <p>
 * For read operations it doesn't use any explicit transactions.
 */
public class DatabaseTransactionsService implements TransactionsService {

    private final TransactionTemplate transactionTemplate;

    private final ExclusiveLock lock;

    public DatabaseTransactionsService(TransactionTemplate transactionTemplate, ExclusiveLock lock) {
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
        this.lock = Objects.requireNonNull(lock, "lock");
    }

    /**
     * @inheritDocs
     */
    @Override
    public <T> T writeTransaction(Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        return transactionTemplate.execute(status -> {
            try (AutoCloseable ignored = lock.acquire()) {
                return action.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * @inheritDocs
     */
    @Override
    public <T> T readTransaction(Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        return action.get();
    }
}
