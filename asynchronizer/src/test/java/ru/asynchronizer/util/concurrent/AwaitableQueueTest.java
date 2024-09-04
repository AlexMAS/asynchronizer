package ru.asynchronizer.util.concurrent;

import java.time.Duration;
import java.util.concurrent.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AwaitableQueueTest {

    private AwaitableQueue target;

    @BeforeEach
    public void beforeEach() {
        target = new AwaitableQueue(Executors.newSingleThreadExecutor());
    }

    @AfterEach
    public void afterEach() {
        target.dispose();
    }

    @Test
    public void shouldReturnFutureThatCompletesInTime() throws Exception {

        // Given
        var task = new SomeTask<>();

        // When
        var taskFuture = target.enqueue(task);
        task.complete(123);
        await(taskFuture);

        // Then
        assertThat(taskFuture.isDone()).isEqualTo(true);
        assertThat(taskFuture.isCancelled()).isEqualTo(false);
        assertThat(taskFuture.isCompletedExceptionally()).isEqualTo(false);
        assertThat(taskFuture.get()).isEqualTo(123);
    }

    @Test
    public void shouldCompleteLongRunningTasksByTimeout() throws Exception {

        // Given
        var infiniteTask = new SomeTask<>();

        // When
        var taskFuture = target.enqueue(infiniteTask, Duration.ofMillis(1));
        awaitTimeout(taskFuture);

        // Then
        assertThat(taskFuture.isDone()).isEqualTo(true);
        assertThat(taskFuture.isCancelled()).isEqualTo(false);
        assertThat(taskFuture.isCompletedExceptionally()).isEqualTo(true);
    }

    @Test
    public void shouldProcessBothLongAndFastRunningTasks() throws Exception {

        // Given
        var longTask = new SomeTask<>();
        var fastTask = new SomeTask<>();
        fastTask.complete(123);

        // When
        var longTaskFuture = target.enqueue(longTask);
        var fastTaskFuture = target.enqueue(fastTask);
        await(fastTaskFuture);

        // Then
        assertThat(longTaskFuture.isDone()).isEqualTo(false);
        assertThat(fastTaskFuture.isDone()).isEqualTo(true);
        assertThat(fastTaskFuture.isCancelled()).isEqualTo(false);
        assertThat(fastTaskFuture.isCompletedExceptionally()).isEqualTo(false);
        assertThat(fastTaskFuture.get()).isEqualTo(123);
    }

    @Test
    public void shouldProcessMultipleEnqueuedTasks() throws Exception {

        // Given
        var task1 = new SomeTask<>();
        var task2 = new SomeTask<>();
        var task3 = new SomeTask<>();

        // When
        var task1Future = target.enqueue(task1);
        var task2Future = target.enqueue(task2);
        var task3Future = target.enqueue(task3);
        task1.complete(1);
        var task1Result = await(task1Future);
        task2.complete(2);
        var task2Result = await(task2Future);
        task3.complete(3);
        var task3Result = await(task3Future);

        // Then
        assertThat(task1Result).isEqualTo(1);
        assertThat(task2Result).isEqualTo(2);
        assertThat(task3Result).isEqualTo(3);
    }

    @Test
    public void shouldProcessTaskCancellation() throws Exception {

        // Given
        var task = new SomeTask<>();

        // When
        var taskFuture = target.enqueue(task);
        taskFuture.cancel(true);
        awaitCancellation(taskFuture);

        // Then
        assertThat(taskFuture.isDone()).isEqualTo(true);
        assertThat(taskFuture.isCancelled()).isEqualTo(true);
        assertThat(taskFuture.isCompletedExceptionally()).isEqualTo(true);
    }

    @Test
    public void shouldProcessFailedTasks() throws Exception {

        // Given
        var task = new SomeTask<>();
        var exception = new Exception("Some error.");

        // When
        var taskFuture = target.enqueue(task);
        task.completeExceptionally(exception);
        awaitException(taskFuture, exception);

        // Then
        assertThat(taskFuture.isDone()).isEqualTo(true);
        assertThat(taskFuture.isCancelled()).isEqualTo(false);
        assertThat(taskFuture.isCompletedExceptionally()).isEqualTo(true);
    }

    @Test
    public void shouldHandleRuntimeException() throws Exception {

        // Given
        var failedTask = new SomeTask<>();
        var normalTask1 = new SomeTask<>();
        var normalTask2 = new SomeTask<>();
        var failedTaskError = new RuntimeException("Some error.");

        // When

        var failedTaskFuture = target.enqueue(failedTask);
        var normalTask1Future = target.enqueue(normalTask1);
        var normalTask2Future = target.enqueue(normalTask2);

        failedTask.completeExceptionally(failedTaskError);
        awaitException(failedTaskFuture, failedTaskError);

        normalTask1.complete(123);
        var normalTask1Result = await(normalTask1Future);

        normalTask2.complete(456);
        var normalTask2Result = await(normalTask2Future);

        // Then
        assertThat(normalTask1Result).isEqualTo(123);
        assertThat(normalTask2Result).isEqualTo(456);
    }

    @Test
    public void shouldProcessThreadInterruption() throws Exception {

        // Given
        var task1 = new SomeTask<>();
        var task2 = new SomeTask<>();
        var task3 = new SomeTask<>();

        // When
        var task1Future = target.enqueue(task1);
        var task2Future = target.enqueue(task2);
        var task3Future = target.enqueue(task3);
        task1.interrupt();
        awaitCancellation(task1Future);
        awaitCancellation(task2Future);
        awaitCancellation(task3Future);

        // Then
        assertThat(task1Future.isDone()).isEqualTo(true);
        assertThat(task1Future.isCancelled()).isEqualTo(true);
        assertThat(task1Future.isCompletedExceptionally()).isEqualTo(true);
        assertThat(task2Future.isDone()).isEqualTo(true);
        assertThat(task2Future.isCancelled()).isEqualTo(true);
        assertThat(task2Future.isCompletedExceptionally()).isEqualTo(true);
        assertThat(task3Future.isDone()).isEqualTo(true);
        assertThat(task3Future.isCancelled()).isEqualTo(true);
        assertThat(task3Future.isCompletedExceptionally()).isEqualTo(true);
    }

    @Test
    public void shouldCancelAllTasksWhenDisposing() throws Exception {

        // Given
        var task1 = new SomeTask<>();
        var task2 = new SomeTask<>();
        var task3 = new SomeTask<>();

        // When
        var task1Future = target.enqueue(task1);
        var task2Future = target.enqueue(task2);
        var task3Future = target.enqueue(task3);
        target.dispose();
        awaitCancellation(task1Future);
        awaitCancellation(task2Future);
        awaitCancellation(task3Future);

        // Then
        assertThat(task1Future.isDone()).isEqualTo(true);
        assertThat(task1Future.isCancelled()).isEqualTo(true);
        assertThat(task1Future.isCompletedExceptionally()).isEqualTo(true);
        assertThat(task2Future.isDone()).isEqualTo(true);
        assertThat(task2Future.isCancelled()).isEqualTo(true);
        assertThat(task2Future.isCompletedExceptionally()).isEqualTo(true);
        assertThat(task3Future.isDone()).isEqualTo(true);
        assertThat(task3Future.isCancelled()).isEqualTo(true);
        assertThat(task3Future.isCompletedExceptionally()).isEqualTo(true);
    }

    @Test
    public void shouldProcessEarlyTaskCompletion() throws Exception {

        // Given
        var task = new SomeTask<>();

        // When
        var taskFuture = target.enqueue(task);
        taskFuture.complete(456);
        task.complete(123);
        await(taskFuture);

        // Then
        assertThat(taskFuture.isDone()).isEqualTo(true);
        assertThat(taskFuture.isCancelled()).isEqualTo(false);
        assertThat(taskFuture.isCompletedExceptionally()).isEqualTo(false);
        assertThat(taskFuture.get()).isEqualTo(456);
    }

    @Test
    public void shouldDisposeCompletedTask() throws Exception {

        // Given
        var task = new SomeTask<>();

        // When
        target.enqueue(task);
        task.complete(123);
        await(task.disposeEvent());
    }


    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(5, TimeUnit.SECONDS); // prevent an infinite execution of the test
    }

    private static void awaitTimeout(CompletableFuture<?> future) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS); // prevent an infinite execution of the test
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isInstanceOf(TimeoutException.class);
        }
    }

    private static void awaitCancellation(CompletableFuture<?> future) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS); // prevent an infinite execution of the test
        } catch (CancellationException e) {
            // Expected
        }
    }

    private static void awaitException(CompletableFuture<?> future, Throwable exception) throws Exception {
        try {
            future.get(5, TimeUnit.SECONDS); // prevent an infinite execution of the test
        } catch (ExecutionException e) {
            assertThat(e.getCause()).isEqualTo(exception);
        }
    }


    public static class SomeTask<T> implements IAwaitable<T> {

        private final CountDownLatch completeEvent;
        private final CompletableFuture<?> disposeEvent;
        private T result;
        private Throwable exception;
        private boolean interrupt;

        public SomeTask() {
            this.completeEvent = new CountDownLatch(1);
            this.disposeEvent = new CompletableFuture<>();
        }

        public void complete(T result) {
            this.result = result;
            this.completeEvent.countDown();
        }

        public void completeExceptionally(Throwable exception) {
            this.exception = exception;
        }

        public void interrupt() {
            this.interrupt = true;
        }

        public CompletableFuture<?> disposeEvent() {
            return disposeEvent;
        }

        @Override
        public IAwaiter<T> getAwaiter() {
            return new IAwaiter<>() {

                @Override
                public boolean await(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException {
                    if (exception != null) {
                        if (exception instanceof RuntimeException) {
                            throw (RuntimeException) exception;
                        }
                        throw new ExecutionException(exception);
                    }

                    if (interrupt) {
                        throw new InterruptedException();
                    }

                    return completeEvent.await(timeout, unit);
                }

                @Override
                public T getResult() {
                    return result;
                }

                @Override
                public void dispose() {
                    disposeEvent.complete(null);
                }
            };
        }

    }
}
