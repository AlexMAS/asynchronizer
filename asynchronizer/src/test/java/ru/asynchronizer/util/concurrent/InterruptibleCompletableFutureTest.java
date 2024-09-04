package ru.asynchronizer.util.concurrent;

import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

import ru.asynchronizer.util.function.ThrowableSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.asynchronizer.util.concurrent.InterruptibleCompletableFuture.interruptibleSupplyAsync;

public class InterruptibleCompletableFutureTest {

    @Test
    public void shouldSupplyValue() throws Exception {

        // Given
        var expected = 123;

        // When
        var actual = awaitValue(interruptibleSupplyAsync(successfulSupplier(expected)));

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldSupplyException() {

        // Given
        var expected = new Exception("Some Error");

        // When
        var actual = awaitException(interruptibleSupplyAsync(failureSupplier(expected)));

        // Then
        assertThat(actual.getCause()).isEqualTo(expected);
    }

    @Test
    public void shouldInterruptSupplier() {

        // Given
        var longSupplier = longSupplier();

        // When

        var distantFuture = interruptibleSupplyAsync(longSupplier);
        distantFuture.cancel(true);

        Exception exception = awaitException(distantFuture);

        // Then
        assertThat(distantFuture.isDone()).isEqualTo(true);
        assertThat(distantFuture.isCompletedExceptionally()).isEqualTo(true);
        assertThat(distantFuture.isCancelled()).isEqualTo(true);
        assertThat(exception).isInstanceOf(CancellationException.class);
    }

    @Test
    public void shouldInterruptSupplierOnComplete() throws Exception {

        // Given
        var expected = 123;
        var longSupplier = longSupplier();

        // When

        var distantFuture = interruptibleSupplyAsync(longSupplier);
        distantFuture.complete(expected);

        int actual = awaitValue(distantFuture);

        // Then
        assertThat(distantFuture.isDone()).isEqualTo(true);
        assertThat(distantFuture.isCompletedExceptionally()).isEqualTo(false);
        assertThat(distantFuture.isCancelled()).isEqualTo(false);
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldInterruptSupplierOnCompleteExceptionally() {

        // Given
        var expected = new Exception("Some Error");
        var longSupplier = longSupplier();

        // When

        var distantFuture = interruptibleSupplyAsync(longSupplier);
        distantFuture.completeExceptionally(expected);

        var actual = awaitException(distantFuture);

        // Then
        assertThat(distantFuture.isDone()).isEqualTo(true);
        assertThat(distantFuture.isCompletedExceptionally()).isEqualTo(true);
        assertThat(distantFuture.isCancelled()).isEqualTo(false);
        assertThat(actual.getCause()).isEqualTo(expected);
    }

    @Test
    public void shouldInterruptSupplierOnTimeout() {

        // Given
        var longSupplier = longSupplier();

        // When

        var distantFuture = interruptibleSupplyAsync(longSupplier);
        distantFuture.orTimeout(1, TimeUnit.MILLISECONDS);

        var actual = awaitException(distantFuture);

        // Then
        assertThat(distantFuture.isDone()).isEqualTo(true);
        assertThat(distantFuture.isCompletedExceptionally()).isEqualTo(true);
        assertThat(distantFuture.isCancelled()).isEqualTo(false);
        assertThat(actual).isInstanceOf(ExecutionException.class);
        assertThat(actual.getCause()).isInstanceOf(TimeoutException.class);
    }


    private static <T> ThrowableSupplier<T> successfulSupplier(T expectedValue) {
        return () -> expectedValue;
    }

    private static ThrowableSupplier<?> failureSupplier(Exception expectedException) {
        return () -> { throw expectedException; };
    }

    private static ThrowableSupplier<Integer> longSupplier() {
        return () -> {
            try {
                TimeUnit.SECONDS.sleep(60);
                return 42;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Exception(e);
            }
        };
    }


    private static <T> T awaitValue(CompletableFuture<T> future) throws Exception {
        return future.get(5, TimeUnit.SECONDS); // prevent an infinite execution of the test
    }

    private static Exception awaitException(CompletableFuture<?> future) {
        try {
            awaitValue(future);
            return null;
        } catch (Exception e) {
            return e;
        }
    }
}
