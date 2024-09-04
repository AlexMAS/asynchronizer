package ru.asynchronizer.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Extends {@link CompletableFuture} and provides ability to handle the future completion.
 */
public final class DelayedCompletableFuture<T> extends CompletableFuture<T> {

    private static final Runnable NONE = () -> { };

    private final Runnable onComplete;


    public DelayedCompletableFuture() {
        this(NONE);
    }

    public DelayedCompletableFuture(Runnable onComplete) {
        this.onComplete = onComplete;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        try {
            return super.cancel(mayInterruptIfRunning);
        } finally {
            onComplete.run();
        }
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return super.get();
        } finally {
            onComplete.run();
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } finally {
            onComplete.run();
        }
    }

    @Override
    public boolean complete(T value) {
        try {
            return super.complete(value);
        } finally {
            onComplete.run();
        }
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        try {
            return super.completeExceptionally(ex);
        } finally {
            onComplete.run();
        }
    }
}
