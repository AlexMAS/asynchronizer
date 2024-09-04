package ru.asynchronizer.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import ru.asynchronizer.util.IDisposable;

/**
 * Provides the method to await the result of an asynchronous operation.
 *
 * @param <T> the result type
 *
 * @see IAwaitable
 */
public interface IAwaiter<T> extends IDisposable {

    /**
     * Awaits the result of the asynchronous operation.
     *
     * <p>
     * If returns {@code true} the result is ready and can be read with the {@link #getResult()} method.
     *
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws CancellationException if this asynchronous operation was cancelled
     * @throws ExecutionException if this asynchronous operation completed exceptionally
     */
    boolean await(long timeout, TimeUnit unit) throws InterruptedException, CancellationException, ExecutionException;

    /**
     * Gets the result of the asynchronous operation.
     *
     * <p>
     * Can be invoked only if  the {@link #await(long, TimeUnit)} method returns {@code true}.
     */
    T getResult() throws InterruptedException, CancellationException, ExecutionException;


    @Override
    default void dispose() {

    }
}
