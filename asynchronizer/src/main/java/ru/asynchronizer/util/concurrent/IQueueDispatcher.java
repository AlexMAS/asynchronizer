package ru.asynchronizer.util.concurrent;

import java.util.Collection;
import java.util.function.Consumer;

import ru.asynchronizer.util.IDisposable;

/**
 * Provides functionality for processing a queue.
 *
 * <p>
 * This abstraction assumes that an implementation has a queue and processes
 * incoming items consequently, asynchronously, and probably by using batching.
 *
 * <p>
 * Additionally, the interface provides methods to subscribes to the processing status.
 *
 * @param <T> the type of processed items
 *
 * @see IQueueHandler
 */
public interface IQueueDispatcher<T> extends IDisposable {

    /**
     * Adds the given item to the processing queue.
     */
    void enqueue(T item);

    /**
     * Adds the given items to the processing queue.
     */
    default void enqueue(Collection<T> items) {
        for (var item : items) {
            enqueue(item);
        }
    }


    /**
     * Subscribes to the processing success event.
     *
     * <p>
     * The observer is invoked when the next batch has been processed successfully
     * since {@linkplain #subscribeToFailure(Consumer) the latest failure}.
     *
     * @see #subscribeToFailure(Consumer)
     */
    IDisposable subscribeToSuccess(Runnable observer);

    /**
     * Subscribes to the processing failure event.
     *
     * <p>
     * The observer is invoked when the next batch has not been processed due to a failure.
     *
     * @see #subscribeToSuccess(Runnable)
     */
    IDisposable subscribeToFailure(Consumer<Throwable> observer);
}
