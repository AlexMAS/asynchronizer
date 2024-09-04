package ru.asynchronizer.util.concurrent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CompletableFutureUtil {

    private static final Runnable NONE = () -> { };
    private static final CompletableFuture<Boolean> TRUE = AsyncCompletableFuture.completedFuture(true);
    private static final CompletableFuture<Boolean> FALSE = AsyncCompletableFuture.completedFuture(false);
    private static final CompletableFuture<Object> COMPLETED = AsyncCompletableFuture.completedFuture(null);


    private CompletableFutureUtil() {

    }


    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> completed() {
        return (CompletableFuture<T>) COMPLETED;
    }

    public static <T> CompletableFuture<T> completed(T value) {
        return AsyncCompletableFuture.completedFuture(value);
    }


    public static CompletableFuture<Boolean> completedTrue() {
        return TRUE;
    }

    public static CompletableFuture<Boolean> completedFalse() {
        return FALSE;
    }

    public static CompletableFuture<Boolean> completedBoolean(boolean value) {
        return value ? TRUE : FALSE;
    }


    public static <R> CompletableFuture<R> failed(Throwable e) {
        return AsyncCompletableFuture.failedFuture(e);
    }


    public static CompletableFuture<Void> delay(Duration timeout) {
        return delay(timeout.toNanos(), TimeUnit.NANOSECONDS);
    }

    public static CompletableFuture<Void> delay(long timeout, TimeUnit unit) {
        return new AsyncCompletableFuture<Void>().completeOnTimeout(null, timeout, unit);
    }


    public static CompletableFuture<Void> toVoid(CompletableFuture<?> source) {
        return source.thenRun(NONE);
    }

    @SuppressWarnings("unchecked")
    public static CompletableFuture<Object> toObject(CompletableFuture<?> source) {
        return (CompletableFuture<Object>) source;
    }


    @SafeVarargs
    public static <T> CompletableFuture<T> anyOf(CompletionStage<T>... futures) {
        return anyOf(Asynchronizer.commonPool(), futures);
    }

    @SafeVarargs
    public static <T> CompletableFuture<T> anyOf(Executor executor, CompletionStage<T>... futures) {
        return anyOf(executor, Arrays.asList(futures));
    }

    public static <T> CompletableFuture<T> anyOf(Stream<? extends CompletionStage<T>> futures) {
        return anyOf(Asynchronizer.commonPool(), futures);
    }

    public static <T> CompletableFuture<T> anyOf(Executor executor, Stream<? extends CompletionStage<T>> futures) {
        return anyOf(executor, futures.collect(Collectors.toList()));
    }

    public static <T> CompletableFuture<T> anyOf(Collection<? extends CompletionStage<T>> futures) {
        return anyOf(Asynchronizer.commonPool(), futures);
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> anyOf(Executor executor, Collection<? extends CompletionStage<T>> futures) {
        if (futures.isEmpty()) {
            return completed();
        }

        var result = new AsyncCompletableFuture<T>(executor);

        var arrayOfFutures = futures.stream().map(i -> i.toCompletableFuture()).toArray(CompletableFuture[]::new);

        executor.execute(() -> {
            try {
                var value = CompletableFuture.anyOf(arrayOfFutures).get();
                result.complete((T) value);
            } catch (ExecutionException e) {
                result.completeExceptionally(e.getCause());
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });

        return result;
    }


    @SafeVarargs
    public static <T> CompletableFuture<Collection<T>> allOf(CompletionStage<T>... futures) {
        return allOf(Asynchronizer.commonPool(), futures);
    }

    @SafeVarargs
    public static <T> CompletableFuture<Collection<T>> allOf(Executor executor, CompletionStage<T>... futures) {
        return allOf(executor, Arrays.asList(futures));
    }

    public static <T> CompletableFuture<Collection<T>> allOf(Stream<? extends CompletionStage<T>> futures) {
        return allOf(Asynchronizer.commonPool(), futures);
    }

    public static <T> CompletableFuture<Collection<T>> allOf(Executor executor, Stream<? extends CompletionStage<T>> futures) {
        return allOf(executor, futures.collect(Collectors.toList()));
    }

    public static <T> CompletableFuture<Collection<T>> allOf(Collection<? extends CompletionStage<T>> futures) {
        return allOf(Asynchronizer.commonPool(), futures);
    }

    public static <T> CompletableFuture<Collection<T>> allOf(Executor executor, Collection<? extends CompletionStage<T>> futures) {
        if (futures.isEmpty()) {
            return AsyncCompletableFuture.completedFuture(List.of());
        }

        var result = new AsyncCompletableFuture<Collection<T>>(executor);

        for (var f : futures) {
            f.whenCompleteAsync((r, e) -> {
                if (e != null) {
                    result.completeExceptionally(e);
                }
            }, executor);
        }

        executor.execute(() -> {
            var list = new ArrayList<T>();
            for (var future : futures) {
                list.add(future.toCompletableFuture().join());
            }
            result.complete(list);
        });

        return result;
    }


    public static TimeoutException getTimeoutException(Throwable source) {
        return getNestedException(source, TimeoutException.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getNestedException(Throwable source, Class<? extends T> nested) {
        var cause = source;

        while (cause != null && cause != cause.getCause()) {
            if (nested.isInstance(cause)) {
                return (T) cause;
            }
            cause = cause.getCause();
        }

        return null;
    }
}
