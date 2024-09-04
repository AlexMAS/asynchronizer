package ru.asynchronizer.util.concurrent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AsyncContextTest {

    @Test
    public void shouldIsolateThreads() throws Exception {

        // Given
        var context = new AsyncContext();

        // When

        var parentState = new HashMap<String, Object>();
        var thread1State = new HashMap<String, Object>();
        var thread2State = new HashMap<String, Object>();

        context.setProperty("p1", "v10");
        context.setProperty("p2", "v20");
        context.setProperty("p3", "v30");
        context.setProperty("p4", "v40");

        await(
                async(() -> {
                    context.setProperty("p1", "v11");
                    context.setProperty("p2", "v21");
                    context.setProperty("p3", "v31");
                    context.capture().copyTo(thread1State);
                }),
                async(() -> {
                    context.setProperty("p2", "v22");
                    context.setProperty("p3", "v32");
                    context.setProperty("p4", "v42");
                    context.capture().copyTo(thread2State);
                })
        );

        context.capture().copyTo(parentState);

        // Then

        assertThat(parentState.size()).isEqualTo(4);
        assertThat(parentState.get("p1")).isEqualTo("v10");
        assertThat(parentState.get("p2")).isEqualTo("v20");
        assertThat(parentState.get("p3")).isEqualTo("v30");
        assertThat(parentState.get("p4")).isEqualTo("v40");

        assertThat(thread1State.size()).isEqualTo(3);
        assertThat(thread1State.get("p1")).isEqualTo("v11");
        assertThat(thread1State.get("p2")).isEqualTo("v21");
        assertThat(thread1State.get("p3")).isEqualTo("v31");

        assertThat(thread2State.size()).isEqualTo(3);
        assertThat(thread2State.get("p2")).isEqualTo("v22");
        assertThat(thread2State.get("p3")).isEqualTo("v32");
        assertThat(thread2State.get("p4")).isEqualTo("v42");
    }

    @Test
    public void shouldCaptureState() throws Exception {

        // Given
        var context = new AsyncContext();

        // When

        var parentState = new HashMap<String, Object>();
        var thread1State = new HashMap<String, Object>();
        var thread2State = new HashMap<String, Object>();

        context.setProperty("p1", "v11");
        context.setProperty("p2", "v21");
        context.setProperty("p3", "v31");
        var capture1 = context.capture();

        context.setProperty("p2", "v22");
        context.setProperty("p3", "v32");
        context.setProperty("p4", "v42");
        var capture2 = context.capture();

        await(
                async(() -> {
                    capture1.use(); // apply capture
                    context.capture().copyTo(thread1State);
                }),
                async(() -> {
                    capture2.use(); // apply capture
                    context.capture().copyTo(thread2State);
                })
        );

        context.capture().copyTo(parentState);

        // Then

        assertThat(parentState.size()).isEqualTo(4);
        assertThat(parentState.get("p1")).isEqualTo("v11");
        assertThat(parentState.get("p2")).isEqualTo("v22");
        assertThat(parentState.get("p3")).isEqualTo("v32");
        assertThat(parentState.get("p4")).isEqualTo("v42");

        assertThat(thread1State.size()).isEqualTo(3);
        assertThat(thread1State.get("p1")).isEqualTo("v11");
        assertThat(thread1State.get("p2")).isEqualTo("v21");
        assertThat(thread1State.get("p3")).isEqualTo("v31");

        assertThat(thread2State.size()).isEqualTo(4);
        assertThat(thread2State.get("p1")).isEqualTo("v11");
        assertThat(thread2State.get("p2")).isEqualTo("v22");
        assertThat(thread2State.get("p3")).isEqualTo("v32");
        assertThat(thread2State.get("p4")).isEqualTo("v42");
    }

    @Test
    public void shouldRestoreState() throws Exception {

        // Given
        var context = new AsyncContext();

        // When

        var parentState = new HashMap<String, Object>();
        var thread1State = new HashMap<String, Object>();
        var thread2State = new HashMap<String, Object>();

        context.setProperty("p1", "v1");
        context.setProperty("p2", "v2");
        var capture = context.capture();

        await(
                async(() -> {
                    context.setProperty("p1", "v11");
                    context.setProperty("p3", "v31");

                    var captureUsage = capture.use(); // apply capture
                    captureUsage.dispose(); // restore own state

                    context.capture().copyTo(thread1State);
                }),
                async(() -> {
                    context.setProperty("p2", "v22");
                    context.setProperty("p4", "v42");

                    var captureUsage = capture.use(); // apply capture
                    captureUsage.dispose(); // restore own state

                    context.capture().copyTo(thread2State);
                })
        );

        context.capture().copyTo(parentState);

        // Then

        assertThat(parentState.size()).isEqualTo(2);
        assertThat(parentState.get("p1")).isEqualTo("v1");
        assertThat(parentState.get("p2")).isEqualTo("v2");

        assertThat(thread1State.size()).isEqualTo(2);
        assertThat(thread1State.get("p1")).isEqualTo("v11");
        assertThat(thread1State.get("p3")).isEqualTo("v31");

        assertThat(thread2State.size()).isEqualTo(2);
        assertThat(thread2State.get("p2")).isEqualTo("v22");
        assertThat(thread2State.get("p4")).isEqualTo("v42");
    }

    @Test
    public void shouldNotifyAboutChanges() throws Exception {

        // Given

        var context = new AsyncContext();
        var contextChanges = new ArrayList<String>();
        var contextSwitches = new AtomicInteger(0);

        var changeObserver = new IAsyncContextObserver() {

            @Override
            public void contextChange(String name, Object value) {
                contextChanges.add(String.format("%s-%s", name, value));
            }

            @Override
            public void contextSwitch(Map<String, Object> context) {
                contextSwitches.incrementAndGet();
            }
        };

        // When

        context.setProperty("p1", "v11"); // no observers

        var subscription = context.subscribeToChange(changeObserver); // subscribe

        context.setProperty("p1", "v12");
        context.setProperty("p2", "v22");

        var capture = context.capture();

        await(async(() -> {
            context.setProperty("p2", "v23");
            context.setProperty("p3", "v33");

            var captureUsage = capture.use(); // apply capture, the first switch
            captureUsage.dispose(); // restore own state, the second switch
        }));

        context.setProperty("p3", "v34");

        subscription.dispose(); // unsubscribe

        context.setProperty("p4", "v44"); // no observers

        // Then
        assertThat(contextChanges).contains("p1-v12", "p2-v22", "p2-v23", "p3-v33", "p3-v34");
        assertThat(contextSwitches.get()).isEqualTo(2);
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

    private static void await(CompletableFuture<?>... task) throws Exception {
        CompletableFuture.allOf(task).get(2, TimeUnit.SECONDS); // prevent an infinite execution of the test
    }
}
