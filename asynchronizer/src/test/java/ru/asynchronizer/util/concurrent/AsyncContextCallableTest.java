package ru.asynchronizer.util.concurrent;

import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AsyncContextCallableTest {

    @Test
    public void shouldUseParentThreadState() throws Exception {

        // Given

        var context = new AsyncContext();

        // The parent thread state
        context.setProperty("p1", "v1");
        context.setProperty("p2", "v2");

        // An action to perform asynchronously

        var actionState = new HashMap<String, Object>();

        Callable<Integer> action = () -> {
            context.capture().copyTo(actionState);
            return 123;
        };

        // When

        var asyncAction = new AsyncContextCallable<>(action, context, e -> { });

        var result = await(async(() -> {

            // The child thread state
            context.setProperty("p1", "v11");
            context.setProperty("p2", "v21");
            context.setProperty("p3", "v31");

            return asyncAction.call();
        }));

        // Then
        Assertions.assertThat(result).isEqualTo(123);
        assertThat(actionState.size()).isEqualTo(2);
        assertThat(actionState.get("p1")).isEqualTo("v1");
        assertThat(actionState.get("p2")).isEqualTo("v2");
    }

    @Test
    public void shouldRestoreChildThreadState() throws Exception {

        // Given

        var context = new AsyncContext();

        // The parent thread state
        context.setProperty("p1", "v1");
        context.setProperty("p2", "v2");

        // An action to perform asynchronously
        Callable<Integer> action = () -> 123;

        // When

        var childThreadState = new HashMap<String, Object>();
        var asyncAction = new AsyncContextCallable<>(action, context, e -> { });

        Integer result = await(async(() -> {

            // The child thread state
            context.setProperty("p1", "v11");
            context.setProperty("p2", "v21");
            context.setProperty("p3", "v31");

            try {
                return asyncAction.call();
            } finally {
                context.capture().copyTo(childThreadState);
            }
        }));

        // Then
        assertThat(result).isEqualTo(123);
        assertThat(childThreadState.size()).isEqualTo(3);
        assertThat(childThreadState.get("p1")).isEqualTo("v11");
        assertThat(childThreadState.get("p2")).isEqualTo("v21");
        assertThat(childThreadState.get("p3")).isEqualTo("v31");
    }

    @Test
    public void shouldHandleUncaughtException() {

        // Given

        var context = new AsyncContext();
        var expectedException = new RuntimeException();
        var actualException = new AtomicReference<Throwable>();

        Callable<Object> action = () -> { throw expectedException; };
        IUncaughtExceptionHandler exceptionHandler = e -> actualException.set(e);

        // When

        var asyncAction = new AsyncContextCallable<>(action, context, exceptionHandler);

        try {
            asyncAction.call();
        } catch (Throwable ignore) {
            // Ignore
        }

        // Then
        assertThat(actualException.get()).isEqualTo(expectedException);
    }


    private static <T> CompletableFuture<T> async(Callable<T> action) {

        // Run the task in a really separate thread

        var task = new CompletableFuture<T>();

        var thread = new Thread(() -> {
            try {
                var r = action.call();
                task.complete(r);
            } catch (Exception e) {
                task.completeExceptionally(e);
            }
        });

        thread.start();

        return task;
    }

    private static <T> T await(CompletableFuture<T> task) throws Exception {
        return task.get(2, TimeUnit.SECONDS); // prevent an infinite execution of the test
    }
}
