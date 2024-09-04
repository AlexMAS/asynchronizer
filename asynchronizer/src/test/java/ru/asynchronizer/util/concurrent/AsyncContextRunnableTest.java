package ru.asynchronizer.util.concurrent;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AsyncContextRunnableTest {

    @Test
    public void shouldUseParentThreadState() throws Exception {

        // Given

        var context = new AsyncContext();

        // The parent thread state
        context.setProperty("p1", "v1");
        context.setProperty("p2", "v2");

        // An action to perform asynchronously
        var actionState = new HashMap<String, Object>();
        Runnable action = () -> context.capture().copyTo(actionState);

        // When

        var asyncAction = new AsyncContextRunnable(action, context, e -> { });

        await(async(() -> {

            // The child thread state
            context.setProperty("p1", "v11");
            context.setProperty("p2", "v21");
            context.setProperty("p3", "v31");

            asyncAction.run();
        }));

        // Then
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
        Runnable action = () -> { };

        // When

        var childThreadState = new HashMap<String, Object>();
        var asyncAction = new AsyncContextRunnable(action, context, e -> { });

        await(async(() -> {

            // The child thread state
            context.setProperty("p1", "v11");
            context.setProperty("p2", "v21");
            context.setProperty("p3", "v31");

            try {
                asyncAction.run();
            } finally {
                context.capture().copyTo(childThreadState);
            }
        }));

        // Then
        assertThat(childThreadState.size()).isEqualTo(3);
        assertThat(childThreadState.get("p1")).isEqualTo("v11");
        assertThat(childThreadState.get("p2")).isEqualTo("v21");
        assertThat(childThreadState.get("p3")).isEqualTo("v31");
    }

    @Test
    public void shouldHandleUncaughtException() throws Exception {

        // Given

        var context = new AsyncContext();
        var expectedException = new RuntimeException();
        var actualException = new AtomicReference<Throwable>();

        Runnable action = () -> { throw expectedException; };
        IUncaughtExceptionHandler exceptionHandler = e -> actualException.set(e);

        // When

        var asyncAction = new AsyncContextRunnable(action, context, exceptionHandler);

        try {
            asyncAction.run();
        } catch (Throwable ignore) {
            // Ignore
        }

        // Then
        assertThat(actualException.get()).isEqualTo(expectedException);
    }


    private static CompletableFuture<Void> async(Runnable action) {

        // Run the task in a really separate thread

        var task = new CompletableFuture<Void>();

        var thread = new Thread(() -> {
            try {
                action.run();
                task.complete(null);
            } catch (Exception e) {
                task.completeExceptionally(e);
            }
        });

        thread.start();

        return task;
    }

    private static void await(CompletableFuture<?> task) throws Exception {
        task.get(2, TimeUnit.SECONDS); // prevent an infinite execution of the test
    }
}
