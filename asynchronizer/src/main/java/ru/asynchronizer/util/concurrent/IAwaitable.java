package ru.asynchronizer.util.concurrent;

import java.util.concurrent.*;

/**
 * Represents an asynchronous operation which can be awaited for the execution result.
 *
 * @param <T> the result type
 *
 * @see IAwaiter
 */
public interface IAwaitable<T> {

    /**
     * Gets the result awaiter.
     */
    IAwaiter<T> getAwaiter();


    static <T> IAwaitable<T> of(Future<T> future) {
        return () -> new IAwaiter<>() {

            @Override
            public boolean await(long timeout, TimeUnit unit) throws InterruptedException, CancellationException, ExecutionException {
                try {
                    future.get(timeout, unit);
                } catch (TimeoutException e) {
                    return false;
                }
                return future.isDone();
            }

            @Override
            public T getResult() throws InterruptedException, CancellationException, ExecutionException {
                return future.get();
            }

            @Override
            public void dispose() {
                future.cancel(true);
            }
        };
    }
}
