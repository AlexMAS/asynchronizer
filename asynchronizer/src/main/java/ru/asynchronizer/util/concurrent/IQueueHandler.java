package ru.asynchronizer.util.concurrent;

import java.util.Collection;

/**
 * The queue handler.
 *
 * @param <T> the type of processed items
 *
 * @see IQueueDispatcher
 */
public interface IQueueHandler<T> {

    /**
     * Processes the given set of items.
     */
    void handle(Collection<T> items) throws Throwable;
}
