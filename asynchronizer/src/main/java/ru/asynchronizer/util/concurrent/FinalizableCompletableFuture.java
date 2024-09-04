package ru.asynchronizer.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ru.asynchronizer.util.IDisposable;

/**
 * Extends {@link CompletableFuture} and provides ability to handle any kind of the future completion.
 */
public class FinalizableCompletableFuture<T> extends CompletableFuture<T> implements IDisposable {

    private static final Runnable NONE = () -> { };

    private final Runnable finalizer;


    public FinalizableCompletableFuture() {
        this(NONE);
    }

    public FinalizableCompletableFuture(Runnable finalizer) {
        this.finalizer = finalizer;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        try {
            return super.cancel(mayInterruptIfRunning);
        } finally {
            dispose();
        }
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        try {
            return super.get();
        } finally {
            dispose();
        }
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return super.get(timeout, unit);
        } finally {
            dispose();
        }
    }

    @Override
    public boolean complete(T value) {
        try {
            return super.complete(value);
        } finally {
            dispose();
        }
    }

    @Override
    public boolean completeExceptionally(Throwable ex) {
        try {
            return super.completeExceptionally(ex);
        } finally {
            dispose();
        }
    }

    @Override
    public void dispose() {
        finalizer.run();
    }
}
