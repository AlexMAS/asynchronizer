package ru.asynchronizer.util.concurrent;

import java.util.concurrent.Executor;

/**
 * The execution context of the {@link IAsyncPipeline}.
 */
public interface IAsyncFlow {

    /**
     * Returns the executor to use for this asynchronous execution.
     */
    Executor executor();

    /**
     * Interrupts the execution of the current pipeline with providing the specified result.
     */
    void interrupt(Object result);

    /**
     * Interrupts the execution of the current pipeline without providing any result.
     */
    default void interrupt() {
        interrupt(null);
    }
}
