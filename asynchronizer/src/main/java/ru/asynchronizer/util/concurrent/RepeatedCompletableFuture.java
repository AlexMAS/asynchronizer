package ru.asynchronizer.util.concurrent;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import ru.asynchronizer.util.IDisposable;
import ru.asynchronizer.util.function.ThrowableSupplier;

/**
 * Provides an ability to await some result until success.
 *
 * @param <T> the result type
 */
public final class RepeatedCompletableFuture<T> extends AsyncCompletableFuture<T> implements IDisposable {

    private final ThrowableSupplier<T> valueSupplier;
    private final Consumer<Exception> errorHandler;
    private final Duration attemptDelay;
    private final ScheduledExecutorService executor;
    private final AtomicBoolean started;
    private final AtomicReference<T> valueRef;
    private final CompletableFuture<T> lastError;

    private Future<?> attemptFuture;


    public RepeatedCompletableFuture(ThrowableSupplier<T> valueSupplier, Consumer<Exception> errorHandler, Duration attemptDelay, ScheduledExecutorService executor) {
        super();
        this.valueSupplier = valueSupplier;
        this.errorHandler = errorHandler;
        this.attemptDelay = attemptDelay;
        this.executor = executor;
        this.started = new AtomicBoolean(false);
        this.valueRef = new AtomicReference<>(null);
        this.lastError = new AsyncCompletableFuture<>(defaultExecutor());
    }


    /**
     * Creates an asynchronous task that tries to retrieve the value by using specified supplier.
     * Attempts will be performed until success. In case of failure the specified error handler is called
     * and the next attempt will be performed after the given delay.
     *
     * @param valueSupplier the value supplier which will be work in a separate thread
     * @param errorHandler the error which will be called after each unsuccessful attempt
     * @param attemptDelay the fixed delay between attempts
     * @param <T> the result type
     *
     * @return a future instance to await the result
     */
    public static <T> RepeatedCompletableFuture<T> startAttempts(ThrowableSupplier<T> valueSupplier, Consumer<Exception> errorHandler, Duration attemptDelay) {
        return startAttempts(valueSupplier, errorHandler, attemptDelay, Asynchronizer.commonScheduledPool());
    }

    /**
     * Creates an asynchronous task that tries to retrieve the value by using specified supplier.
     * Attempts will be performed until success. In case of failure the specified error handler is called
     * and the next attempt will be performed after the given delay.
     *
     * @param valueSupplier the value supplier which will be work in a separate thread
     * @param errorHandler the error which will be called after each unsuccessful attempt
     * @param attemptDelay the fixed delay between attempts
     * @param executor the executor which will be used to perform the attempts
     * @param <T> the result type
     *
     * @return a future instance to await the result
     */
    public static <T> RepeatedCompletableFuture<T> startAttempts(ThrowableSupplier<T> valueSupplier, Consumer<Exception> errorHandler, Duration attemptDelay, ScheduledExecutorService executor) {
        var future = new RepeatedCompletableFuture<>(valueSupplier, errorHandler, attemptDelay, executor);
        future.start();
        return future;
    }


    public void start() {
        if (!started.getAndSet(true)) {
            attemptFuture = executor.submit(this::performAttempt);
        }
    }


    private void performAttempt() {
        try {
            var value = valueSupplier.tryGet();
            attemptFuture = null;
            valueRef.set(value);
            super.complete(value);
        } catch (Exception e) {
            lastError.obtrudeException(e);
            errorHandler.accept(e);
            attemptFuture = executor.schedule(this::performAttempt, attemptDelay.toNanos(), TimeUnit.NANOSECONDS);
        }
    }

    private boolean cancelAttempt(Future<?> attemptFuture, boolean mayInterruptIfRunning) {
        if (attemptFuture != null && !attemptFuture.isDone()) {
            return attemptFuture.cancel(mayInterruptIfRunning);
        }
        return false;
    }


    public CompletableFuture<T> lastAttempt() {
        if (isDone()) {
            return this;
        }

        return CompletableFutureUtil.anyOf(defaultExecutor(), this, lastError);
    }


    @Override
    public boolean complete(T value) {
        if (isDone()) {
            return false;
        }

        var completed = super.complete(value);
        cancelAttempt(attemptFuture, true);

        return completed;
    }

    @Override
    public boolean completeExceptionally(Throwable exception) {
        if (isDone()) {
            return false;
        }

        var completed = super.completeExceptionally(exception);
        cancelAttempt(attemptFuture, true);

        return completed;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        var cancelled = cancelAttempt(attemptFuture, mayInterruptIfRunning);

        if (cancelled) {
            super.obtrudeException(new CancellationException());
        }

        return cancelled;
    }


    @Override
    public void dispose() {
        if (!isDone()) {
            cancel(true);
        }

        var value = valueRef.getAndSet(null);

        if (value != null) {
            if (value instanceof IDisposable) {
                ((IDisposable) value).dispose();
            } else if ((value instanceof AutoCloseable)) {
                try {
                    ((AutoCloseable) value).close();
                } catch (Exception ignore) {
                    // Ignore
                }
            }
        }
    }
}
