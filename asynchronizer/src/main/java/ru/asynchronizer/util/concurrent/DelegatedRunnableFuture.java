package ru.asynchronizer.util.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The wrapper class that exposes only the {@link RunnableFuture} methods based on a given instance of the {@link RunnableFuture}.
 */
public abstract class DelegatedRunnableFuture<T> implements RunnableFuture<T> {

    protected final RunnableFuture<T> target;


    protected DelegatedRunnableFuture(RunnableFuture<T> target) {
        this.target = target;
    }


    @Override
    public void run() {
        target.run();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return target.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return target.isCancelled();
    }

    @Override
    public boolean isDone() {
        return target.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return target.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return target.get(timeout, unit);
    }
}
