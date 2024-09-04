package ru.asynchronizer.util.concurrent;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecutorFactoryTest {

    private static final Class<?> owner = ExecutorFactoryTest.class;


    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public class DaemonExecutors {

        public Iterable<TestCase<ExecutorService>> testCases() {
            return List.of(
                    new TestCase<>("Cached", f -> f.newCachedThreadPool(owner)),
                    new TestCase<>("Cached, daemon", f -> f.newCachedThreadPool(owner, true)),
                    new TestCase<>("Fixed, 5 threads", f -> f.newFixedThreadPool(owner, 5)),
                    new TestCase<>("Fixed, 5 threads, daemon", f -> f.newFixedThreadPool(owner, 5, true)),
                    new TestCase<>("SingleThread", f -> f.newSingleThreadExecutor(owner)),
                    new TestCase<>("SingleThread, daemon", f -> f.newSingleThreadExecutor(owner, true)),
                    new TestCase<>("Scheduled, 5 threads", f -> f.newScheduledThreadPool(owner, 5)),
                    new TestCase<>("Scheduled, 5 threads, daemon", f -> f.newScheduledThreadPool(owner, 5, true)),
                    new TestCase<>("SingleThreadScheduled", f -> f.newSingleThreadScheduledExecutor(owner)),
                    new TestCase<>("SingleThreadScheduled, daemon", f -> f.newSingleThreadScheduledExecutor(owner, true))
            );
        }


        @ParameterizedTest
        @MethodSource("testCases")
        public void shouldCreateDaemonThread(TestCase<ExecutorService> testCase) throws Exception {

            // Given
            var executor = testCase.getExecutor();

            // When
            var daemonTask = new CompletableFuture<Boolean>();
            executor.execute(() -> daemonTask.complete(Thread.currentThread().isDaemon()));
            var result = await(daemonTask);

            // Then
            assertThat(result).isEqualTo(true);
        }

        @ParameterizedTest
        @MethodSource("testCases")
        public void shouldPassContextThroughAsyncFlow(TestCase<ExecutorService> testCase) throws Exception {

            // Given
            var context = testCase.getContext();
            var executor = testCase.getExecutor();

            // When / Then

            assertAsyncFlow(context, r -> executor.execute(r));

            assertAsyncFlow(context, r -> executor.submit(r));

            assertAsyncFlow(context, r -> executor.submit(() -> {
                r.run();
                return true;
            }));
        }

        @ParameterizedTest
        @MethodSource("testCases")
        public void shouldHandleUncaughtException(TestCase<ExecutorService> testCase) throws Exception {

            // Given

            var expectedException = new RuntimeException();
            var actualException = new CompletableFuture<Throwable>();

            var factory = testCase.getFactory();
            factory.setUncaughtExceptionHandler(e -> actualException.complete(e));

            var executor = testCase.getExecutor();

            // When
            executor.submit(() -> { throw expectedException; });
            await(actualException);

            // Then
            assertThat(actualException.isDone()).isEqualTo(true);
            assertThat(actualException.get()).isEqualTo(expectedException);
        }

    }


    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public class MainExecutors {

        public Iterable<TestCase<ExecutorService>> testCases() {
            return List.of(
                    new TestCase<>("Cached", f -> f.newCachedThreadPool(owner, false)),
                    new TestCase<>("Fixed, 5 threads", f -> f.newFixedThreadPool(owner, 5, false)),
                    new TestCase<>("SingleThread", f -> f.newSingleThreadExecutor(owner, false)),
                    new TestCase<>("Scheduled, 5 threads", f -> f.newScheduledThreadPool(owner, 5, false)),
                    new TestCase<>("SingleThreadScheduled", f -> f.newSingleThreadScheduledExecutor(owner, false))
            );
        }


        @ParameterizedTest
        @MethodSource("testCases")
        public void shouldCreateMainThread(TestCase<ExecutorService> testCase) throws Exception {

            // Given
            var executor = testCase.getExecutor();

            // When
            var daemonTask = new CompletableFuture<Boolean>();
            executor.execute(() -> daemonTask.complete(Thread.currentThread().isDaemon()));
            var result = await(daemonTask);

            // Then
            assertThat(result).isEqualTo(false);
        }

    }


    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    public class ScheduledExecutors {

        public Iterable<TestCase<ScheduledExecutorService>> testCases() {
            return List.of(
                    new TestCase<>("Scheduled, 5 threads", f -> f.newScheduledThreadPool(owner, 5)),
                    new TestCase<>("SingleThreadScheduled", f -> f.newSingleThreadScheduledExecutor(owner))
            );
        }


        @ParameterizedTest
        @MethodSource("testCases")
        public void shouldPassContextThroughScheduledAsyncFlow(TestCase<ScheduledExecutorService> testCase) throws Exception {

            // Given
            var context = testCase.getContext();
            var executor = testCase.getExecutor();

            // When / Then

            assertAsyncFlow(context, r -> executor.schedule(() -> {
                r.run();
                return true;
            }, 10, TimeUnit.MILLISECONDS));

            assertAsyncFlow(context, r -> executor.schedule(r, 10, TimeUnit.MILLISECONDS));

            assertAsyncFlow(context, r -> executor.scheduleAtFixedRate(r, 10, 10, TimeUnit.MILLISECONDS));

            assertAsyncFlow(context, r -> executor.scheduleWithFixedDelay(r, 10, 10, TimeUnit.MILLISECONDS));
        }

    }


    private static void assertAsyncFlow(IAsyncContext context, Consumer<Runnable> executor) throws Exception {

        // Given
        var property = "flowName";
        var flowValue1 = UUID.randomUUID().toString();
        var flowValue2 = UUID.randomUUID().toString();

        // When

        context.setProperty(property, flowValue1);
        var flowTask1 = runSomeAsyncFlow(property, context, executor);

        context.setProperty(property, flowValue2);
        var flowTask2 = runSomeAsyncFlow(property, context, executor);

        var flow1 = await(flowTask1);
        var flow2 = await(flowTask2);

        // Then
        assertThat(flow1).hasSize(1);
        assertThat(flow1).contains(flowValue1);
        assertThat(flow2).hasSize(1);
        assertThat(flow2).contains(flowValue2);
    }

    private static CompletableFuture<Set<Object>> runSomeAsyncFlow(String property, IAsyncContext context, Consumer<Runnable> executor) {

        // Run the next flow and register the property values in each sub-task

        Set<Object> flow = ConcurrentHashMap.newKeySet();

        var completed = new CountDownLatch(7 /* tasks */);

        // Task 1 (1)
        executor.accept(() -> {

            flow.add(context.getProperty(property));

            // Task 11 (2)
            executor.accept(() -> {

                flow.add(context.getProperty(property));

                // Task 111 (3)
                executor.accept(() -> {
                    flow.add(context.getProperty(property));
                    completed.countDown();
                });

                // Task 112 (4)
                executor.accept(() -> {
                    flow.add(context.getProperty(property));
                    completed.countDown();
                });

                completed.countDown();
            });

            // Task 12 (5)
            executor.accept(() -> {

                flow.add(context.getProperty(property));

                // Task 121 (6)
                executor.accept(() -> {
                    flow.add(context.getProperty(property));
                    completed.countDown();
                });

                // Task 122 (7)
                executor.accept(() -> {
                    flow.add(context.getProperty(property));
                    completed.countDown();
                });

                completed.countDown();
            });

            completed.countDown();
        });

        // Wait for the flow termination in a really separate thread

        var flowTask = new CompletableFuture<Set<Object>>();

        var thread = new Thread(() -> {
            try {
                completed.await();
                flowTask.complete(flow);
            } catch (Exception e) {
                flowTask.completeExceptionally(e);
            }
        });

        thread.start();

        return flowTask;
    }


    private static <T> T await(CompletableFuture<T> task) throws Exception {
        return task.get(5, TimeUnit.SECONDS); // prevent an infinite execution of the test
    }


    @Getter
    private static class TestCase<T extends ExecutorService> {

        private final String name;
        private final IAsyncContext context;
        private final IExecutorFactory factory;
        private final T executor;


        public TestCase(String name, Function<IExecutorFactory, T> executorSupplier) {
            this.name = name;
            this.context = new AsyncContext();
            this.factory = new ExecutorFactory(context);
            this.executor = executorSupplier.apply(factory);
        }


        @Override
        public String toString() {
            return name;
        }

    }
}
