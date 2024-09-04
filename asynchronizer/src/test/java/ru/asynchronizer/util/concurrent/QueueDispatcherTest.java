package ru.asynchronizer.util.concurrent;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("resource")
public class QueueDispatcherTest {

    @Test
    public void shouldWriteOnExceededBufferSize() throws Exception {

        // Given
        var bufferSize = 5;
        var bufferTimeout = Duration.ofHours(1); // too long
        var flushed = new CompletableFuture<Collection<Integer>>();
        var target = new QueueDispatcher<Integer>(items -> flushed.complete(items), bufferSize, bufferTimeout);

        // When

        target.enqueue(1);
        target.enqueue(2);
        target.enqueue(3);
        target.enqueue(4);
        target.enqueue(5);

        var flushedPoints = flushed.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(flushedPoints).hasSize(bufferSize);
        assertThat(flushedPoints).contains(1, 2, 3, 4, 5);
    }

    @Test
    public void shouldWriteOnExceededTimeout() throws Exception {

        // Given
        var bufferSize = 5;
        var bufferTimeout = Duration.ofSeconds(1);
        var flushed = new CompletableFuture<Collection<Integer>>();
        var target = new QueueDispatcher<Integer>(items -> flushed.complete(items), bufferSize, bufferTimeout);

        // When

        target.enqueue(1);

        var flushedPoints = flushed.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(flushedPoints).hasSize(1);
        assertThat(flushedPoints).contains(1);
    }

    @Test
    public void shouldFlushOnDispose() throws Exception {

        // Given
        var bufferSize = 1000; // too big
        var bufferTimeout = Duration.ofHours(1); // too long
        var flushed = new CompletableFuture<Collection<Integer>>();
        var target = new QueueDispatcher<Integer>(items -> flushed.complete(items), bufferSize, bufferTimeout);

        // When

        target.enqueue(1);
        target.enqueue(2);
        target.enqueue(3);
        target.dispose();

        var flushedPoints = flushed.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(flushedPoints).hasSize(3);
        assertThat(flushedPoints).contains(1, 2, 3);
    }

    @Test
    public void shouldActivateAndDeactivateErrorState() throws Exception {

        // Given

        var bufferSize = 1;
        var bufferTimeout = Duration.ofHours(1); // too long
        var flushed = new CompletableFuture<Collection<Integer>>();
        var throwFlag = new AtomicBoolean(false);
        var failure = new RuntimeException();

        var target = new QueueDispatcher<Integer>(
                items -> {
                    if (throwFlag.get()) {
                        throw failure;
                    }
                    flushed.complete(items);
                },
                bufferSize,
                bufferTimeout);

        var onSuccess = new CompletableFuture<Boolean>();
        var onFailure = new CompletableFuture<Throwable>();

        target.subscribeToSuccess(() -> onSuccess.complete(true));
        target.subscribeToFailure(t -> onFailure.complete(t));

        // When 1
        target.enqueue(1);
        flushed.get(5, TimeUnit.SECONDS);

        // Then 1
        assertThat(onSuccess.isDone()).isFalse();
        assertThat(onFailure.isDone()).isFalse();

        // When 2
        throwFlag.set(true);
        target.enqueue(2);
        var actualFailure = onFailure.get(5, TimeUnit.SECONDS);

        // Then 2
        assertThat(actualFailure).isEqualTo(failure);

        // When 3
        throwFlag.set(false);
        target.enqueue(3);
        var actualSuccess = onSuccess.get(5, TimeUnit.SECONDS);

        // Then 3
        assertThat(actualSuccess).isEqualTo(true);
    }

    @Test
    @Disabled("Manual")
    public void performanceTest() {

        // Given
        var count = 1_000_000_000L;
        var counter = new AtomicLong(0L);
        var target = new QueueDispatcher<Long>(items -> counter.getAndAdd(items.size()));

        // When

        var timeBefore = getTime();
        var memoryBefore = getMemory();

        for (var i = 0L; i < count; ++i) {
            target.enqueue(i);
        }

        target.dispose();

        var timeAfter = getTime();
        var memoryAfter = getMemory();

        // Then
        assertThat(counter.get()).isEqualTo(count);
        System.out.format("Total count  : %d\n", counter.get());
        System.out.format("Total time   : %d ms\n", timeAfter - timeBefore);
        System.out.format("Total memory : %.2f KB\n", (memoryAfter - memoryBefore) / 1024.0);
    }


    private static long getTime() {
        return System.currentTimeMillis();
    }

    private static long getMemory() {
        var runtime = Runtime.getRuntime();
        runtime.gc();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
