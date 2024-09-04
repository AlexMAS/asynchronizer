package ru.asynchronizer.util.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.*;

/**
 * Provides methods to define a sequence of async tasks.
 *
 * <p>
 * This abstraction implements the pipeline pattern and organizes
 * computation as a sequence of async tasks where the result of one task
 * is the input data to the next.
 *
 * <p>
 * All computations are performed in the context of {@link IAsyncFlow} which
 * is common for all tasks of the pipeline. It allows to share the execution
 * context and {@linkplain IAsyncFlow#interrupt(Object) interrupt} the pipeline
 * execution at any moment depending on specified conditions. This fills
 * the lack of such functionality in {@link CompletableFuture}.
 *
 * <p>
 * To handle errors of a specific step use {@link #onError(Consumer)} handler
 * right after this step. To handle the final stage of the pipeline execution
 * use {@link #onFinally(BiConsumer)} handler. It is invoked with the result
 * (or null if none) and the exception (or null if none) as arguments.
 *
 * <p>
 * If a pipeline completes exceptionally both {@link #onError(Consumer)} and
 * {@link #onFinally(BiConsumer)} are invoked with the original exception as
 * the appropriate argument. So that it is not necessary to check tha the given
 * exception is wrapped in {@link CompletionException} as in case of direct
 * work with {@link CompletableFuture}.
 *
 * @param <T> the result type
 */
public interface IAsyncPipeline<T> {

    /**
     * Adds the given action to the pipeline to execute it when the current stage completes
     * normally and without {@linkplain IAsyncFlow#interrupt(Object) interruption}.
     *
     * @param action the action to execute
     *
     * @return the new state of the pipeline
     */
    IAsyncPipeline<Void> run(IAsyncThenRunnable<T> action);

    /**
     * Adds the given action to the pipeline to execute it when the current stage completes
     * normally and without {@linkplain IAsyncFlow#interrupt(Object) interruption}.
     *
     * @param action the action to execute
     * @param <R> the result type of the action
     *
     * @return the new state of the pipeline
     */
    <R> IAsyncPipeline<R> supply(IAsyncThenSupplier<T, R> action);

    /**
     * Adds the given asynchronous computation to the pipeline to execute it when the current
     * stage completes normally and without {@linkplain IAsyncFlow#interrupt(Object) interruption}.
     *
     * @param futureSupplier the asynchronous computation to await
     * @param <R> the result type of the asynchronous computation
     *
     * @return the new state of the pipeline
     */
    <R> IAsyncPipeline<R> await(BiFunction<IAsyncFlow, T, CompletionStage<R>> futureSupplier);

    /**
     * Interrupts the execution of the current pipeline with providing the specified result if the current state satisfy the given condition.
     *
     * @param condition the condition
     * @param resultSupplier the result supplier
     *
     * @return the new state of the pipeline
     */
    IAsyncPipeline<T> interruptIf(Predicate<T> condition, Function<T, Object> resultSupplier);

    /**
     * Interrupts the execution of the current pipeline without providing any result if the current state satisfy the given condition.
     *
     * @param condition the condition
     *
     * @return the new state of the pipeline
     */
    default IAsyncPipeline<T> interruptIf(Predicate<T> condition) {
        return interruptIf(condition, r -> null);
    }

    /**
     * Adds the given handler to the pipeline to execute it if the current stage completes exceptionally.
     *
     * @param handler the error handler
     *
     * @return the new state of the pipeline
     */
    IAsyncPipeline<T> onError(Consumer<? super Throwable> handler);

    /**
     * Adds the given handler to the pipeline to execute it at the end of the pipeline computation regardless of its result.
     *
     * @param handler the final handler
     *
     * @return the new state of the pipeline
     */
    IAsyncPipeline<T> onFinally(BiConsumer<? super T, ? super Throwable> handler);

    /**
     * Returns the instance of {@link CompletionStage} represented this pipeline.
     */
    CompletionStage<T> toFuture();

    /**
     * Returns the instance of {@link CompletableFuture} represented this pipeline.
     */
    default CompletableFuture<T> toCompletableFuture() {
        return toFuture().toCompletableFuture();
    }
}
