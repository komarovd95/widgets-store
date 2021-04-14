package com.github.komarovd95.widgetstore.application.service.transaction;

import java.util.function.Supplier;

/**
 * A service for application transaction management.
 * <p>
 * The implementations MUST be thread-safe.
 * <p>
 * At the minimum, transactions returned by methods of this interface must be Atomic, Consisted and Isolated.
 */
public interface TransactionsService {

    /**
     * Executes given action in the scope of the "write transaction". The term "write transaction" refers to
     * the transaction that allows modification (i.e. write operations).
     * <p>
     * Write transaction MAY give an exclusive read-write access to the resource.
     *
     * @param action the action that should be executed in the scope of transaction
     * @param <T> the type of the result
     * @return the result returned by the action
     */
    <T> T writeTransaction(Supplier<T> action);

    /**
     * Executes given action in the scope of the "read transaction". The term "read transaction" refers to
     * the transaction that allows only read operations.
     * <p>
     * Usually, read transaction gives a shared read-only access to the resource.
     *
     * @param action the action that should be executed in the scope of transaction
     * @param <T> the type of the result
     * @return the result returned by the action
     */
    <T> T readTransaction(Supplier<T> action);
}
