package com.github.komarovd95.widgetstore.application.service.transaction;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * An implementation of the {@link TransactionsService}.
 * <p>
 * For read/write transactions it uses Read-Write lock. So, read transactions give a shared access to the resource and
 * write transactions give an exclusive access to the resource.
 */
public class InMemoryTransactionsService implements TransactionsService {

    /**
     * A lock.
     */
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * @inheritDocs
     */
    @Override
    public <T> T writeTransaction(Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        return executeWithLock(lock.writeLock(), action);
    }

    /**
     * @inheritDocs
     */
    @Override
    public <T> T readTransaction(Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        return executeWithLock(lock.readLock(), action);
    }

    private <T> T executeWithLock(Lock lock, Supplier<T> action) {
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
