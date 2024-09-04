package ru.asynchronizer.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import ru.asynchronizer.util.function.ThrowableRunnable;
import ru.asynchronizer.util.function.ThrowableSupplier;

public final class InterruptibleCompletableFuture<T> extends CompletableFuture<T> {

    private final Future<?> interruptibleFuture;


    private InterruptibleCompletableFuture(ThrowableSupplier<T> supplier, ExecutorService executor) {
        this.interruptibleFuture = executor.submit(() -> {
            try {
                super.complete(supplier.tryGet());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                super.completeExceptionally(exception);
            } catch (Throwable exception) {
                super.completeExceptionally(exception);
            }
        });
    }


    public static <T> InterruptibleCompletableFuture<T> interruptibleSupplyAsync(ThrowableSupplier<T> supplier) {
        return interruptibleSupplyAsync(supplier, Asynchronizer.commonPool());
    }

    public static <T> InterruptibleCompletableFuture<T> interruptibleSupplyAsync(ThrowableSupplier<T> supplier, ExecutorService executor) {
        return new InterruptibleCompletableFuture<>(supplier, executor);
    }


    public static InterruptibleCompletableFuture<Void> interruptibleRunAsync(ThrowableRunnable runnable) {
        return interruptibleRunAsync(runnable, Asynchronizer.commonPool());
    }

    public static InterruptibleCompletableFuture<Void> interruptibleRunAsync(ThrowableRunnable runnable, ExecutorService executor) {
        return new InterruptibleCompletableFuture<>(() -> {
            runnable.tryRun();
            return null;
        }, executor);
    }


    @Override
    public boolean complete(T value) {
        if (isDone()) {
            return false;
        }

        var completed = super.complete(value);
        interruptibleFuture.cancel(true);

        return completed;
    }

    @Override
    public boolean completeExceptionally(Throwable exception) {
        if (isDone()) {
            return false;
        }

        var completed = super.completeExceptionally(exception);
        interruptibleFuture.cancel(true);

        return completed;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        var cancelled = interruptibleFuture.cancel(mayInterruptIfRunning);

        if (cancelled) {
            super.obtrudeException(new CancellationException());
        }

        return cancelled;
    }
}
