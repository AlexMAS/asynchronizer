package ru.asynchronizer.util.concurrent;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PriorityExecutorServiceTest {

    @Test
    public void shouldExecuteRunnableWithPriority() {

        // Given

        var taskCount = 100;
        var randomRange = getRandomRange(taskCount);
        var testStarted = new CompletableFuture<Boolean>();
        var testCompleted = new CountDownLatch(taskCount);

        var target = Asynchronizer.executorFactory().newPrioritySingleThreadExecutor(getClass());
        target.submit(() -> await(testStarted));

        // When

        var taskSequence = new ArrayList<>();

        randomRange.stream()
                .map(i -> IPriorityRunnable.withPriority(i, () -> {
                    taskSequence.add(i);
                    testCompleted.countDown();
                }))
                .forEach(target::submit);

        testStarted.complete(true);
        await(testCompleted);

        // Then
        assertThat(taskSequence).isEqualTo(getSortedRange(randomRange));
    }

    @Test
    public void shouldExecuteCallableWithPriority() {

        // Given

        var taskCount = 100;
        var randomRange = getRandomRange(taskCount);
        var testStarted = new CompletableFuture<Boolean>();
        var testCompleted = new CountDownLatch(taskCount);

        var target = Asynchronizer.executorFactory().newPrioritySingleThreadExecutor(getClass());
        target.submit(() -> await(testStarted));

        // When

        var taskSequence = new ArrayList<>();

        randomRange.stream()
                .map(i -> IPriorityCallable.withPriority(i, () -> {
                    taskSequence.add(i);
                    testCompleted.countDown();
                    return i;
                }))
                .forEach(target::submit);

        testStarted.complete(true);
        await(testCompleted);

        // Then
        assertThat(taskSequence).isEqualTo(getSortedRange(randomRange));
    }

    @Test
    public void shouldNotExecuteCancelledTask() {

        // Given

        var taskCount = 100;
        var randomRange = getRandomRange(taskCount);
        var testStarted = new CompletableFuture<Boolean>();

        var target = Asynchronizer.executorFactory().newPrioritySingleThreadExecutor(getClass());
        var testCompleted = target.submit(() -> await(testStarted));

        // When

        var taskSequence = new ArrayList<>();

        randomRange.stream()
                .map(i -> IPriorityRunnable.withPriority(i, () -> taskSequence.add(i)))
                .map(target::submit)
                .forEach(i -> i.cancel(true));

        testStarted.complete(true);
        await(testCompleted);
        await(Duration.ofSeconds(1));

        // Then
        assertThat(taskSequence).isEmpty();
    }

    @Test
    public void shouldExecuteEqualPriorityTasksInHistoricalOrder() {

        // Given

        var taskCount = 100;
        var randomRange = getRandomRange(taskCount);
        var testStarted = new CompletableFuture<Boolean>();
        var testCompleted = new CountDownLatch(taskCount);

        var target = Asynchronizer.executorFactory().newPrioritySingleThreadExecutor(getClass());
        target.submit(() -> await(testStarted));

        // When

        var taskSequence = new ArrayList<>();

        randomRange.stream()
                .map(i -> IPriorityRunnable.withPriority(123, () -> {
                    taskSequence.add(i);
                    testCompleted.countDown();
                }))
                .forEach(target::submit);

        testStarted.complete(true);
        await(testCompleted);

        // Then
        assertThat(taskSequence).isEqualTo(randomRange);
    }

    @Test
    public void shouldProvideAsyncContext() {

        // Given

        var propertyName = UUID.randomUUID().toString();
        var propertyValue = UUID.randomUUID().toString();

        var target = Asynchronizer.executorFactory().newPrioritySingleThreadExecutor(getClass());

        // When

        Asynchronizer.context().setProperty(propertyName, propertyValue);

        var actualPropertyValue = await(
                target.submit(IPriorityCallable.withHighPriority(() ->
                        Asynchronizer.context().getProperty(propertyName)))
        );

        // Then
        assertThat(actualPropertyValue).isEqualTo(propertyValue);
    }

    @Test
    public void shouldProvidePriorityViews() {

        // Given

        var testStarted = new CompletableFuture<Boolean>();
        var testCompleted = new CountDownLatch(4);

        var target = Asynchronizer.executorFactory().newPrioritySingleThreadExecutor(getClass());
        target.submit(() -> await(testStarted));

        var lowPriorityView = target.withLowPriority();
        var highPriorityView = target.withHighPriority();

        // When

        var taskSequence = new ArrayList<>();

        lowPriorityView.submit(() -> {
            taskSequence.add(11);
            testCompleted.countDown();
        });

        lowPriorityView.submit(() -> {
            taskSequence.add(12);
            testCompleted.countDown();
        });

        highPriorityView.submit(() -> {
            taskSequence.add(21);
            testCompleted.countDown();
        });

        highPriorityView.submit(() -> {
            taskSequence.add(22);
            testCompleted.countDown();
        });

        testStarted.complete(true);
        await(testCompleted);

        // Then
        assertThat(taskSequence).contains(21, 22, 11, 12);
    }

    @Test
    public void shouldWorkWithCompletableFuture() {

        // Given

        var taskCount = 100;
        var testStarted = new CompletableFuture<Boolean>();
        var testCompleted = new CountDownLatch(taskCount);

        var target = Asynchronizer.executorFactory().newPrioritySingleThreadExecutor(getClass());

        // When

        for (var i = 0; i < taskCount; ++i) {
            testStarted.thenRunAsync(() -> testCompleted.countDown(), target);
        }

        testStarted.complete(true);

        // Then
        await(testCompleted);
    }


    private static Collection<Integer> getRandomRange(int size) {
        return new Random().ints(size, 0, size).boxed().collect(Collectors.toList());
    }

    private static Collection<Integer> getSortedRange(Collection<Integer> randomRange) {
        return randomRange.stream().sorted(Collections.reverseOrder()).collect(Collectors.toList());
    }

    private static <T> T await(Future<T> event) {
        try {
            return event.get(5, TimeUnit.SECONDS); // prevent an infinite execution of the test
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void await(CountDownLatch event) {
        try {
            event.await(5, TimeUnit.SECONDS); // prevent an infinite execution of the test
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static void await(Duration delay) {
        try {
            TimeUnit.SECONDS.sleep(delay.toSeconds());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
