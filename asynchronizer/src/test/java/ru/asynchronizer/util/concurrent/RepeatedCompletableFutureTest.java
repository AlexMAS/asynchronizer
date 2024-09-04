package ru.asynchronizer.util.concurrent;

import java.time.Duration;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import ru.asynchronizer.util.IDisposable;
import ru.asynchronizer.util.function.ThrowableSupplier;

import static org.assertj.core.api.Assertions.assertThat;

public class RepeatedCompletableFutureTest {

    @Test
    public void shouldRetrieveValueAfterFirstAttempt() throws Exception {

        // Given
        var valueSupplier = TestValueSupplier.valueImmediately(123);

        // When
        var valueFuture = RepeatedCompletableFuture.startAttempts(valueSupplier, e -> { }, Duration.ofMillis(1));
        awaitSuccess(valueFuture);

        // Then
        assertThat(valueFuture.get()).isEqualTo(123);
    }

    @Test
    public void shouldRetrieveValueAfterSeveralAttempts() throws Exception {

        // Given
        var valueSupplier = TestValueSupplier.valueAfter(123, 3);

        // When
        var valueFuture = RepeatedCompletableFuture.startAttempts(valueSupplier, e -> { }, Duration.ofMillis(1));
        awaitSuccess(valueFuture);

        // Then
        assertThat(valueFuture.get()).isEqualTo(123);
    }

    @Test
    public void shouldProvideLastAttemptWhenUncompleted() {

        // Given
        var attemptError = new Exception("");
        var valueSupplier = TestValueSupplier.errorAlways(attemptError);

        // When
        var valueFuture = RepeatedCompletableFuture.startAttempts(valueSupplier, e -> { }, Duration.ofDays(1));
        var lastAttempt = valueFuture.lastAttempt();
        var actualError = awaitError(lastAttempt);

        // Then
        assertThat(actualError).isEqualTo(attemptError);
    }

    @Test
    public void shouldProvideValueWhenCompleted() throws Exception {

        // Given
        var valueSupplier = TestValueSupplier.valueImmediately(123);

        // When
        var valueFuture = RepeatedCompletableFuture.startAttempts(valueSupplier, e -> { }, Duration.ofMillis(1));
        awaitSuccess(valueFuture);
        var lastAttempt = valueFuture.lastAttempt();
        awaitSuccess(lastAttempt);

        // Then
        assertThat(lastAttempt.get()).isEqualTo(123);
    }

    @Test
    public void shouldCancelAttempts() {

        // Given
        var valueSupplier = TestValueSupplier.errorAlways();

        // When
        var valueFuture = RepeatedCompletableFuture.startAttempts(valueSupplier, e -> { }, Duration.ofMillis(1));
        valueFuture.cancel(true);
        var actualError = awaitError(valueFuture);

        // Then
        assertThat(actualError).isInstanceOf(CancellationException.class);
    }

    @Test
    public void shouldDisposeValue() {

        // Given
        var disposed = new AtomicBoolean(false);
        IDisposable disposableValue = () -> disposed.set(true);
        var valueSupplier = TestValueSupplier.valueImmediately(disposableValue);

        // When
        var valueFuture = RepeatedCompletableFuture.startAttempts(valueSupplier, e -> { }, Duration.ofMillis(1));
        awaitSuccess(valueFuture);
        valueFuture.dispose();

        // Then
        assertThat(disposed.get()).isEqualTo(true);
    }


    // Helpers


    private static void awaitSuccess(Future<?> future) {
        try {
            future.get(5, TimeUnit.SECONDS); // prevent an infinite execution of the test
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Throwable awaitError(Future<?> future) {
        try {
            future.get(5, TimeUnit.SECONDS); // prevent an infinite execution of the test
            throw new AssertionError();
        } catch (ExecutionException e) {
            return e.getCause();
        } catch (Exception e) {
            return e;
        }
    }


    private static class TestValueSupplier<T> implements ThrowableSupplier<T> {

        private final T value;
        private final int minAttempts;
        private final Exception attemptError;

        private int attempt;


        public TestValueSupplier(T value, int minAttempts, Exception attemptError) {
            this.value = value;
            this.minAttempts = minAttempts;
            this.attemptError = attemptError;
        }


        public static <T> TestValueSupplier<T> valueImmediately(T value) {
            return new TestValueSupplier<>(value, 1, null);
        }

        public static <T> TestValueSupplier<T> valueAfter(T value, int attempts) {
            return new TestValueSupplier<>(value, attempts, new Exception(""));
        }

        public static <T> TestValueSupplier<T> errorAlways(Exception error) {
            return new TestValueSupplier<>(null, Integer.MAX_VALUE, error);
        }

        public static <T> TestValueSupplier<T> errorAlways() {
            return errorAlways(new Exception(""));
        }


        @Override
        public T tryGet() throws Exception {
            if (++attempt >= minAttempts) {
                return value;
            }
            throw attemptError;
        }
    }
}
