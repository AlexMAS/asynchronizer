package ru.asynchronizer.util.concurrent;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DelayedCompletableFutureTest {

    @Test
    public void shouldHandleCancellation() {

        // Given
        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable onComplete = () -> completed.set(true);
        DelayedCompletableFuture<Object> future = new DelayedCompletableFuture<>(onComplete);

        // When
        future.cancel(true);

        // Then
        assertThat(future.isDone()).isEqualTo(true);
        assertThat(future.isCancelled()).isEqualTo(true);
        assertThat(completed.get()).isEqualTo(true);
    }

    @Test
    public void shouldHandleNormalCompletion() {

        // Given
        Object result = new Object();
        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable onComplete = () -> completed.set(true);
        DelayedCompletableFuture<Object> future = new DelayedCompletableFuture<>(onComplete);

        // When
        future.complete(result);

        // Then
        assertThat(future.isDone()).isEqualTo(true);
        assertThat(future.isCancelled()).isEqualTo(false);
        assertThat(future.getNow(null)).isEqualTo(result);
        assertThat(completed.get()).isEqualTo(true);
    }

    @Test
    public void shouldHandleErrorCompletion() {

        // Given
        Throwable error = new Exception();
        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable onComplete = () -> completed.set(true);
        DelayedCompletableFuture<Object> future = new DelayedCompletableFuture<>(onComplete);

        // When

        future.completeExceptionally(error);

        Throwable actualError = null;

        try {
            future.getNow(null);
        } catch (CompletionException e) {
            actualError = e.getCause();
        }

        // Then
        assertThat(future.isDone()).isEqualTo(true);
        assertThat(future.isCancelled()).isEqualTo(false);
        assertThat(completed.get()).isEqualTo(true);
        assertThat(actualError).isEqualTo(error);
    }

    @Test
    public void shouldHandleTimeouts() {

        // Given
        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable onComplete = () -> completed.set(true);
        DelayedCompletableFuture<Object> neverCompleted = new DelayedCompletableFuture<>(onComplete);

        // When

        neverCompleted.orTimeout(1, TimeUnit.MILLISECONDS);

        Throwable actualError = null;

        try {
            neverCompleted.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            actualError = e.getCause();
        }

        // Then
        assertThat(neverCompleted.isDone()).isEqualTo(true);
        assertThat(neverCompleted.isCancelled()).isEqualTo(false);
        assertThat(completed.get()).isEqualTo(true);
        assertThat(actualError).isInstanceOf(TimeoutException.class);
    }

    @Test
    public void shouldHandleDelayedCompletion() {

        // Given
        Object result = new Object();
        AtomicBoolean completed = new AtomicBoolean(false);
        Runnable onComplete = () -> completed.set(true);
        DelayedCompletableFuture<Object> neverCompleted = new DelayedCompletableFuture<>(onComplete);

        // When

        neverCompleted.completeOnTimeout(result, 1, TimeUnit.MILLISECONDS);

        Object actualResult = null;

        try {
            actualResult = neverCompleted.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Ignore
        }

        // Then
        assertThat(neverCompleted.isDone()).isEqualTo(true);
        assertThat(neverCompleted.isCancelled()).isEqualTo(false);
        assertThat(completed.get()).isEqualTo(true);
        assertThat(actualResult).isEqualTo(result);
    }
}
