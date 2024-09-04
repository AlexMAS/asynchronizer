package ru.asynchronizer.util.concurrent;

import java.util.UUID;
import java.util.concurrent.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.asynchronizer.util.concurrent.Asynchronizer.commonPool;
import static ru.asynchronizer.util.concurrent.Asynchronizer.context;

public class CompletableFutureUtilTest {

    // allOf


    @Test
    public void shouldAwaitAllTasks() throws Exception {

        // Given
        var subTask1 = new CompletableFuture<Integer>();
        var subTask2 = new CompletableFuture<Integer>();
        var subTask3 = new CompletableFuture<Integer>();

        // When
        var result = CompletableFutureUtil.allOf(subTask1, subTask2, subTask3);
        subTask1.complete(11);
        subTask2.complete(22);
        subTask3.complete(33);
        awaitSuccess(result);

        // Then
        assertThat(result.get()).containsExactlyInAnyOrder(11, 22, 33);
    }

    @Test
    public void shouldNotAwaitAllTasksIfOneFailed() {

        // Given
        var subTask1 = new CompletableFuture<Integer>();
        var subTask2 = new CompletableFuture<Integer>();
        var subTask3 = new CompletableFuture<Integer>();
        var subTaskError = new Exception();

        // When
        var result = CompletableFutureUtil.allOf(subTask1, subTask2, subTask3);
        subTask2.completeExceptionally(subTaskError);
        var resultError = awaitError(result);

        // Then
        assertThat(result.isDone()).isEqualTo(true);
        assertThat(result.isCompletedExceptionally()).isEqualTo(true);
        assertThat(resultError).isInstanceOf(ExecutionException.class);
        assertThat(resultError.getCause()).isEqualTo(subTaskError);
    }

    @Test
    public void shouldNotAwaitAllTasksIfOneCancelled() {

        // Given
        var subTask1 = new CompletableFuture<Integer>();
        var subTask2 = new CompletableFuture<Integer>();
        var subTask3 = new CompletableFuture<Integer>();

        // When
        var result = CompletableFutureUtil.allOf(subTask1, subTask2, subTask3);
        subTask2.cancel(true);
        var resultError = awaitError(result);

        // Then
        assertThat(result.isDone()).isEqualTo(true);
        assertThat(result.isCompletedExceptionally()).isEqualTo(true);
        assertThat(resultError).isInstanceOf(CancellationException.class);
    }

    @Test
    public void shouldAwaitAllTasksWithParentContext() throws Exception {

        // Given

        var parentValue = UUID.randomUUID().toString();
        var childValue = new CompletableFuture<>();

        var subTask1 = new CompletableFuture<Boolean>();
        var subTask2 = new CompletableFuture<Boolean>();
        var subTask3 = new CompletableFuture<Boolean>();

        // When

        // Run the parent thread
        commonPool().submit(() -> {

            // Set the parent context
            context().setProperty("p", parentValue);

            // Await all subtasks
            CompletableFutureUtil.allOf(subTask1, subTask2, subTask3)
                    .thenRunAsync(() -> {

                        // Retrieve the context after executing all subtasks
                        childValue.complete(context().getProperty("p"));
                    });
        });

        // Complete all subtasks
        subTask1.complete(true);
        subTask2.complete(true);
        subTask3.complete(true);

        // Await the test completed
        awaitSuccess(childValue);

        // Then
        assertThat(childValue.get()).isEqualTo(parentValue);
    }


    // anyOf


    @Test
    public void shouldAwaitAnyTasks() throws Exception {

        // Given
        var subTask1 = new CompletableFuture<Integer>();
        var subTask2 = new CompletableFuture<Integer>();
        var subTask3 = new CompletableFuture<Integer>();

        // When
        var result = CompletableFutureUtil.anyOf(subTask1, subTask2, subTask3);
        subTask2.complete(22);
        awaitSuccess(result);

        // Then
        assertThat(result.get()).isEqualTo(22);
    }

    @Test
    public void shouldNotAwaitAnyTasksIfOneFailed() {

        // Given
        var subTask1 = new CompletableFuture<Integer>();
        var subTask2 = new CompletableFuture<Integer>();
        var subTask3 = new CompletableFuture<Integer>();
        var subTaskError = new Exception();

        // When
        var result = CompletableFutureUtil.anyOf(subTask1, subTask2, subTask3);
        subTask2.completeExceptionally(subTaskError);
        var resultError = awaitError(result);

        // Then
        assertThat(result.isDone()).isEqualTo(true);
        assertThat(result.isCompletedExceptionally()).isEqualTo(true);
        assertThat(resultError).isInstanceOf(ExecutionException.class);
        assertThat(resultError.getCause()).isEqualTo(subTaskError);
    }

    @Test
    public void shouldNotAwaitAnyTasksIfOneCancelled() {

        // Given
        var subTask1 = new CompletableFuture<Integer>();
        var subTask2 = new CompletableFuture<Integer>();
        var subTask3 = new CompletableFuture<Integer>();

        // When
        var result = CompletableFutureUtil.anyOf(subTask1, subTask2, subTask3);
        subTask2.cancel(true);
        var resultError = awaitError(result);

        // Then
        assertThat(result.isDone()).isEqualTo(true);
        assertThat(result.isCompletedExceptionally()).isEqualTo(true);
        assertThat(resultError).isInstanceOf(CancellationException.class);
    }

    @Test
    public void shouldAwaitAnyTasksWithParentContext() throws Exception {

        // Given

        var parentValue = UUID.randomUUID().toString();
        var childValue = new CompletableFuture<>();

        var subTask1 = new CompletableFuture<Boolean>();
        var subTask2 = new CompletableFuture<Boolean>();
        var subTask3 = new CompletableFuture<Boolean>();

        // When

        // Run the parent thread
        commonPool().submit(() -> {

            // Set the parent context
            context().setProperty("p", parentValue);

            // Await all subtasks
            CompletableFutureUtil.anyOf(subTask1, subTask2, subTask3)
                    .thenRunAsync(() -> {

                        // Retrieve the context after executing all subtasks
                        childValue.complete(context().getProperty("p"));
                    });
        });

        // Complete one of the subtasks
        subTask2.complete(true);

        // Await the test completed
        awaitSuccess(childValue);

        // Then
        assertThat(childValue.get()).isEqualTo(parentValue);
    }

    @Test
    public void shouldCastToVoid() {

        // Given
        CompletableFuture<Integer> source = CompletableFutureUtil.completed(123);

        // When
        CompletableFuture<Void> result = CompletableFutureUtil.toVoid(source);

        // Then
        Void resultValue = awaitSuccess(result);
        assertThat(resultValue).isNull();
    }


    @Test
    public void shouldCastToObject() {

        // Given
        CompletableFuture<Void> voidSource = CompletableFutureUtil.completed();
        CompletableFuture<Integer> intSource = CompletableFuture.completedFuture(123);

        // When
        var voidResult = CompletableFutureUtil.toObject(voidSource);
        var intResult = CompletableFutureUtil.toObject(intSource);

        // Then
        awaitSuccess(voidResult);
        awaitSuccess(intResult);
    }


    // Exceptions


    @Test
    public void shouldFindFirstOfTwoNestedExceptions() {

        // Given
        var exception =
                new Exception(
                        new NestedException(
                                "e1",
                                new Exception(
                                        new NestedException("e2"))));

        // When
        var actual = CompletableFutureUtil.getNestedException(exception, NestedException.class);

        // Then
        assertThat(actual).isInstanceOf(NestedException.class);
        assertThat(actual.getMessage()).isEqualTo("e1");
    }

    @Test
    public void shouldFindNestedExceptionWithSelfCause() {

        // Given
        var exception =
                new Exception(
                        new Exception(
                                new Exception(
                                        new NestedException("e"))));

        // When
        var actual = CompletableFutureUtil.getNestedException(exception, NestedException.class);

        // Then
        assertThat(actual).isInstanceOf(NestedException.class);
        assertThat(actual.getMessage()).isEqualTo("e");
    }

    @Test
    public void shouldFindNestedExceptionWithNullCause() {

        // Given
        var exception =
                new Exception(
                        new Exception(
                                new Exception(
                                        new NestedException("e", null))));

        // When
        var actual = CompletableFutureUtil.getNestedException(exception, NestedException.class);

        // Then
        assertThat(actual).isInstanceOf(NestedException.class);
        assertThat(actual.getMessage()).isEqualTo("e");
    }

    @Test
    public void shouldFindNestedExceptionWithCause() {

        // Given
        var exception =
                new Exception(
                        new NestedException(
                                "e1",
                                new Exception("e2")));

        // When
        var actual = CompletableFutureUtil.getNestedException(exception, NestedException.class);

        // Then
        assertThat(actual).isInstanceOf(NestedException.class);
        assertThat(actual.getMessage()).isEqualTo("e1");
        assertThat(actual.getCause()).isInstanceOf(Exception.class);
        assertThat(actual.getCause().getMessage()).isEqualTo("e2");
    }

    @Test
    public void shouldFindItselfNestedException() {

        // Given
        var exception =
                new NestedException("e1",
                        new Exception("e2"));

        // When
        var actual = CompletableFutureUtil.getNestedException(exception, NestedException.class);

        // Then
        assertThat(actual).isEqualTo(exception);
    }

    @Test
    public void shouldReturnWhenNoNestedException() {

        // Given
        var exception =
                new Exception(
                        new Exception(
                                new Exception("e")));

        // When
        var actual = CompletableFutureUtil.getNestedException(exception, NestedException.class);

        // Then
        assertThat(actual).isNull();
    }

    @Test
    @SuppressWarnings("ConstantValue")
    public void shouldReturnNullWhenNoException() {

        // Given
        Exception exception = null;

        // When
        var actual = CompletableFutureUtil.getNestedException(exception, NestedException.class);

        // Then
        assertThat(actual).isNull();
    }

    @Test
    public void shouldFindTimeoutException() {

        // Given
        var timeoutException = new TimeoutException("Timeout!");
        var timeoutExceptionWrapper = new CompletionException(timeoutException);

        // When
        var result1 = CompletableFutureUtil.getTimeoutException(timeoutException);
        var result2 = CompletableFutureUtil.getTimeoutException(timeoutExceptionWrapper);

        // Then
        assertThat(result1).isEqualTo(timeoutException);
        assertThat(result2).isEqualTo(timeoutException);
    }


    private static class NestedException extends Exception {

        public NestedException(String message) {
            super(message);
        }

        public NestedException(String message, Throwable cause) {
            super(message, cause);
        }

    }


    // Helpers


    private static <T> T awaitSuccess(Future<T> future) {
        try {
            return future.get(5, TimeUnit.SECONDS); // prevent an infinite execution of the test
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static Exception awaitError(Future<?> future) {
        try {
            future.get(5, TimeUnit.SECONDS); // prevent an infinite execution of the test
            throw new AssertionError();
        } catch (Exception e) {
            return e;
        }
    }
}
