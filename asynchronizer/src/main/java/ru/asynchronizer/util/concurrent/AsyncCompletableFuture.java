package ru.asynchronizer.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

/**
 * Repeats {@link CompletableFuture} functionality but allows to specify the default executor.
 *
 * <p>
 * If the default executor is not defined the {@link Asynchronizer#commonPool()} is used.
 *
 * <p>
 * This implementation allows to set the default executor that honor {@linkplain IAsyncContext the async context} in continuations
 * for which an executor is not defined explicitly i.e. like {@link CompletableFuture#thenAcceptAsync(Consumer)}.
 * In such cases {@link CompletableFuture} uses the {@link ForkJoinPool#commonPool()}, and it can provoke unexpected behaviour
 * for the side that provides the future instance. At least, {@linkplain IAsyncContext the async context} will not be available
 * for continuations, because they are created with the executor which does not copy the context.
 *
 * @param <T> the result type returned by this future's join and get methods
 *
 * @see IAsyncContext
 */
public class AsyncCompletableFuture<T> extends CompletableFuture<T> {

    private final Executor defaultExecutor;


    public AsyncCompletableFuture() {
        this(Asynchronizer.commonPool());
    }

    public AsyncCompletableFuture(Executor defaultExecutor) {
        this.defaultExecutor = defaultExecutor;
    }


    public static <R> CompletableFuture<R> completedFuture(R value) {
        var result = new AsyncCompletableFuture<R>();
        result.complete(value);
        return result;
    }

    public static <R> CompletionStage<R> completedStage(R value) {
        return completedFuture(value);
    }


    public static <R> CompletableFuture<R> failedFuture(Throwable e) {
        var result = new AsyncCompletableFuture<R>();
        result.completeExceptionally(e);
        return result;
    }

    public static <R> CompletionStage<R> failedStage(Throwable e) {
        return failedFuture(e);
    }


    @Override
    public Executor defaultExecutor() {
        return defaultExecutor;
    }

    @Override
    public <U> CompletableFuture<U> newIncompleteFuture() {
        return new AsyncCompletableFuture<>(defaultExecutor);
    }
}
