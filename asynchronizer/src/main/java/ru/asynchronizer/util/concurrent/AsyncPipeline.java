package ru.asynchronizer.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.*;

/**
 * Provides methods to define a sequence of async tasks.
 *
 * <p>
 * This class represents the main implementation of the {@link IAsyncPipeline}.
 *
 * @param <T> the result type
 */
public final class AsyncPipeline<T> implements IAsyncPipeline<T> {

    private final AsyncFlow flow;
    private final CompletionStage<T> future;


    private AsyncPipeline(AsyncFlow flow, CompletionStage<T> future) {
        this.flow = flow;
        this.future = future;
    }


    /**
     * Creates a new instance of the {@link IAsyncPipeline} based on the given action
     * and executes this pipeline in the {@link Asynchronizer#commonPool()}.
     */
    public static IAsyncPipeline<Void> run(IAsyncRunnable action) {
        return await(f -> run(f, action));
    }

    /**
     * Creates a new instance of the {@link IAsyncPipeline} based on the given action
     * and executes this pipeline in the specified executor.
     */
    public static IAsyncPipeline<Void> run(IAsyncRunnable action, Executor executor) {
        return await(f -> run(f, action), executor);
    }

    /**
     * Creates a new instance of the {@link IAsyncPipeline} based on the given action
     * and executes this pipeline in the {@link Asynchronizer#commonPool()}.
     */
    public static <R> IAsyncPipeline<R> supply(IAsyncSupplier<R> action) {
        return await(f -> supply(f, action));
    }

    /**
     * Creates a new instance of the {@link IAsyncPipeline} based on the given action
     * and executes this pipeline in the specified executor.
     */
    public static <R> IAsyncPipeline<R> supply(IAsyncSupplier<R> action, Executor executor) {
        return await(f -> supply(f, action), executor);
    }

    /**
     * Creates a new instance of the {@link IAsyncPipeline} based on the given asynchronous computation
     * and executes this pipeline in the {@link Asynchronizer#commonPool()}.
     */
    public static <R> IAsyncPipeline<R> await(Function<IAsyncFlow, CompletionStage<R>> futureSupplier) {
        return await(futureSupplier, Asynchronizer.commonPool());
    }

    /**
     * Creates a new instance of the {@link IAsyncPipeline} based on the given asynchronous computation
     * and executes this pipeline in the specified executor.
     */
    public static <R> IAsyncPipeline<R> await(Function<IAsyncFlow, CompletionStage<R>> futureSupplier, Executor executor) {
        var flow = new AsyncFlow(executor);
        var future = futureSupplier.apply(flow);
        return new AsyncPipeline<>(flow, future);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public IAsyncPipeline<Void> run(IAsyncThenRunnable<T> action) {
        return await((f, t) -> run(f, action.carry(t)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> IAsyncPipeline<R> supply(IAsyncThenSupplier<T, R> action) {
        return await((f, t) -> supply(f, action.carry(t)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <R> IAsyncPipeline<R> await(BiFunction<IAsyncFlow, T, CompletionStage<R>> futureSupplier) {
        var thenFuture = future.thenComposeAsync(t -> {
            if (flow.isInterrupted()) {
                return CompletableFuture.completedFuture(null);
            } else {
                return futureSupplier.apply(flow, t);
            }
        }, flow.executor());

        return new AsyncPipeline<>(flow, thenFuture);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public IAsyncPipeline<T> interruptIf(Predicate<T> condition, Function<T, Object> resultSupplier) {
        var nextFuture = future.whenCompleteAsync((t, e) -> {
            if (e == null && !flow.isInterrupted() && condition.test(t)) {
                flow.interrupt(resultSupplier.apply(t));
            }
        }, flow.executor());

        return new AsyncPipeline<>(flow, nextFuture);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public IAsyncPipeline<T> onError(Consumer<? super Throwable> handler) {
        var nextFuture = future.whenCompleteAsync((t, e) -> {
            if (e != null && !flow.isInterrupted()) {
                try {
                    handler.accept(unwrapError(e));
                } finally {
                    flow.interrupt(null);
                }
            }
        }, flow.executor());

        return new AsyncPipeline<>(flow, nextFuture);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IAsyncPipeline<T> onFinally(BiConsumer<? super T, ? super Throwable> handler) {
        var nextFuture = future.whenCompleteAsync((t, e) -> handler.accept(t, unwrapError(e)), flow.executor());
        return new AsyncPipeline<>(flow, nextFuture);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public CompletionStage<T> toFuture() {
        return future.thenApplyAsync(t -> flow.isInterrupted() ? flow.getResult() : t);
    }


    private static CompletionStage<Void> run(IAsyncFlow flow, IAsyncRunnable action) {
        var future = new CompletableFuture<Void>();

        CompletableFuture.runAsync(() -> {
            try {
                action.run(flow);
                future.complete(null);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }, flow.executor());

        return future;
    }

    private static <R> CompletionStage<R> supply(IAsyncFlow flow, IAsyncSupplier<R> action) {
        var future = new CompletableFuture<R>();

        CompletableFuture.runAsync(() -> {
            try {
                R result = action.get(flow);
                future.complete(result);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }, flow.executor());

        return future;
    }

    private static Throwable unwrapError(Throwable error) {
        return (error instanceof CompletionException)
                ? error.getCause()
                : error;
    }
}
