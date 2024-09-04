package ru.asynchronizer.util.concurrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.asynchronizer.util.concurrent.AsyncPipeline.run;
import static ru.asynchronizer.util.concurrent.AsyncPipeline.supply;

public class AsyncPipelineTest {

    @Test
    public void shouldExecuteAllTasksInGivenExecutor() throws Exception {

        // Given

        AtomicInteger count = new AtomicInteger(0);

        Executor executor = action -> {
            count.incrementAndGet();
            action.run();
        };

        Collection<Integer> sequence = new ArrayList<>();

        // When
        await(run(f -> sequence.add(1), executor)
                .run((f, r) -> sequence.add(2))
                .run((f, r) -> sequence.add(3))
                .run((f, r) -> sequence.add(4))
                .run((f, r) -> sequence.add(5)));

        // Then
        assertThat(count.get()).isGreaterThanOrEqualTo(5);
        assertThat(sequence).contains(1, 2, 3, 4, 5);
    }

    @Test
    public void shouldRunTasksInGivenOrder() throws Exception {

        // Given
        Collection<Integer> sequence = new ArrayList<>();

        // When
        await(run(f -> sequence.add(1))
                .run((f, r) -> sequence.add(2))
                .run((f, r) -> sequence.add(3))
                .run((f, r) -> sequence.add(4))
                .run((f, r) -> sequence.add(5)));

        // Then
        assertThat(sequence).contains(1, 2, 3, 4, 5);
    }

    @Test
    public void shouldAwaitTasksInGivenOrder() throws Exception {

        // Given
        Collection<Integer> sequence = new ArrayList<>();

        // When
        await(AsyncPipeline.await(f -> CompletableFuture.supplyAsync(() -> 1))
                .run((f, r) -> sequence.add(r))
                .await((f, r) -> CompletableFuture.supplyAsync(() -> 2))
                .run((f, r) -> sequence.add(r))
                .await((f, r) -> CompletableFuture.supplyAsync(() -> 3))
                .run((f, r) -> sequence.add(r))
                .await((f, r) -> CompletableFuture.supplyAsync(() -> 4))
                .run((f, r) -> sequence.add(r))
                .await((f, r) -> CompletableFuture.supplyAsync(() -> 5))
                .run((f, r) -> sequence.add(r)));

        // Then
        assertThat(sequence).contains(1, 2, 3, 4, 5);
    }

    @Test
    public void shouldInterruptFlow() throws Exception {

        // Given
        Collection<Integer> sequence = new ArrayList<>();

        // When
        await(run(f -> sequence.add(1))
                .run((f, r) -> sequence.add(2))
                .run((f, r) -> f.interrupt())
                .run((f, r) -> sequence.add(4))
                .run((f, r) -> sequence.add(5)));

        // Then
        assertThat(sequence).contains(1, 2);
    }

    @Test
    public void shouldInterruptFlowIfConditionIsTrue() throws Exception {

        // Given
        Collection<Integer> sequence = new ArrayList<>();

        // When
        await(supply(f -> sequence.add(1))
                .supply((f, r) -> sequence.add(2))
                .interruptIf(r -> true)
                .supply((f, r) -> sequence.add(3))
                .supply((f, r) -> sequence.add(4))
                .supply((f, r) -> sequence.add(5)));

        // Then
        assertThat(sequence).contains(1, 2);
    }

    @Test
    public void shouldNotInterruptFlowIfConditionIsFalse() throws Exception {

        // Given
        Collection<Integer> sequence = new ArrayList<>();

        // When
        await(supply(f -> sequence.add(1))
                .supply((f, r) -> sequence.add(2))
                .interruptIf(r -> false)
                .supply((f, r) -> sequence.add(3))
                .supply((f, r) -> sequence.add(4))
                .supply((f, r) -> sequence.add(5)));

        // Then
        assertThat(sequence).contains(1, 2, 3, 4, 5);
    }

    @Test
    public void shouldInterruptByCondition() throws Exception {

        // When
        String result = await(supply(f -> 1)
                .supply((f, r) -> r + 2)
                .supply((f, r) -> r + 3)
                .interruptIf(r -> true, r -> "Interrupted: " + r)
                .supply((f, r) -> r + 4)
                .supply((f, r) -> "Completed: " + r));

        // Then
        assertThat(result).isEqualTo("Interrupted: " + (1 + 2 + 3));
    }

    @Test
    public void shouldInterruptWithResult() throws Exception {

        // When
        String result = await(supply(f -> 1)
                .supply((f, r) -> r + 2)
                .supply((f, r) -> r + 3)
                .supply((f, r) -> {
                    f.interrupt("Interrupted: " + r);
                    return r;
                })
                .supply((f, r) -> r + 4)
                .supply((f, r) -> "Completed: " + r));

        // Then
        assertThat(result).isEqualTo("Interrupted: " + (1 + 2 + 3));
    }

    @Test
    public void shouldPassResultThroughFlow() throws Exception {

        // Given
        int result;

        // When
        result = await(supply(f -> 0)
                .supply((f, r) -> r + 1)
                .supply((f, r) -> r + 2)
                .supply((f, r) -> r + 3)
                .supply((f, r) -> r + 4)
                .supply((f, r) -> r + 5));

        // Then
        assertThat(result).isEqualTo(1 + 2 + 3 + 4 + 5);
    }

    @Test
    public void shouldHandleErrors() throws Exception {

        // Given
        Throwable error = new Exception("Some error!");
        Collection<Throwable> errors = new ArrayList<>();
        Collection<Integer> sequence = new ArrayList<>();

        // When
        try {
            await(run(f -> sequence.add(1))
                    .run((f, r) -> sequence.add(2))
                    .run((f, r) -> { throw error; })
                    .onError(e -> errors.add(e))
                    .onError(e -> errors.add(e))
                    .run((f, r) -> sequence.add(4))
                    .run((f, r) -> sequence.add(5)));
        } catch (ExecutionException ignore) {

        }

        // Then
        assertThat(errors).hasSize(1);
        assertThat(errors).contains(error);
        assertThat(sequence).contains(1, 2);
    }

    @Test
    public void shouldInterruptFlowOnError() throws Exception {

        // Given
        Collection<Integer> sequence = new ArrayList<>();

        // When
        try {
            await(run(f -> sequence.add(1))
                    .onError(e -> sequence.add(11))
                    .run((f, r) -> sequence.add(2))
                    .onError(e -> sequence.add(22))
                    .run((f, r) -> { throw new Exception("Some error!"); })
                    .onError(e -> sequence.add(33))
                    .run((f, r) -> sequence.add(4))
                    .onError(e -> sequence.add(44))
                    .run((f, r) -> sequence.add(5))
                    .onError(e -> sequence.add(55)));
        } catch (ExecutionException ignore) {

        }

        // Then
        assertThat(sequence).contains(1, 2, 33);
    }

    @Test
    public void shouldInvokeFinallyOnSuccess() throws Exception {

        // Given
        Integer result = null;
        Collection<Integer> refResult = new ArrayList<>();
        Collection<Throwable> refError = new ArrayList<>();

        // When
        try {
            result = await(supply(f -> 0)
                    .supply((f, r) -> r + 1)
                    .supply((f, r) -> r + 2)
                    .supply((f, r) -> r + 3)
                    .supply((f, r) -> r + 4)
                    .supply((f, r) -> r + 5)
                    .onFinally((r, e) -> {
                        refResult.add(r);
                        refError.add(e);
                    }));
        } catch (CompletionException ignore) {

        }

        // Then
        assertThat(result).isEqualTo(1 + 2 + 3 + 4 + 5);
        assertThat(refResult).contains(result);
        assertThat(refError).contains((Throwable) null);
    }


    @Test
    public void shouldInvokeFinallyOnError() throws Exception {

        // Given
        Integer result = null;
        Throwable error = new Exception("Some error!");
        Collection<Integer> refResult = new ArrayList<>();
        Collection<Throwable> refError = new ArrayList<>();

        // When
        try {
            result = await(supply(f -> 0)
                    .supply((f, r) -> r + 1)
                    .supply((f, r) -> r + 2)
                    .<Integer>supply((f, r) -> { throw error; })
                    .supply((f, r) -> r + 4)
                    .supply((f, r) -> r + 5)
                    .onFinally((r, e) -> {
                        refResult.add(r);
                        refError.add(e);
                    }));
        } catch (ExecutionException ignore) {

        }

        // Then
        assertThat(result).isNull();
        assertThat(refResult).contains((Integer) null);
        assertThat(refError).contains(error);
    }

    @Test
    public void shouldSupportPipeline() throws Exception {

        // Given

        Collection<Integer> sequence = new CopyOnWriteArrayList<>();

        Collection<Supplier<CompletionStage<Void>>> chains = Arrays.asList(
                () -> run(f -> sequence.add(1)).toFuture(),
                () -> run(f -> sequence.add(2)).toFuture(),
                () -> run(f -> sequence.add(3)).toFuture());

        IAsyncPipeline<Void> pipeline = null;

        for (Supplier<CompletionStage<Void>> chain : chains) {
            if (pipeline == null) {
                pipeline = AsyncPipeline.await(f -> chain.get());
            } else {
                pipeline = pipeline.await((f, r) -> chain.get());
            }
        }

        pipeline = pipeline.onFinally((r, e) -> {
            sequence.add(999);
        });

        // When
        await(pipeline);

        // Then
        assertThat(sequence).contains(1, 2, 3, 999);
    }

    @Test
    public void shouldAllowRecursion() throws Exception {

        // Given
        int n = 10;

        // When
        int expected = factSync(n);
        int actual = await(factAsync(n));

        // Then
        assertThat(actual).isEqualTo(expected);
    }

    private int factSync(int n) {
        return (n >= 1)
                ? factSync(n - 1) * n
                : 1;
    }

    private CompletableFuture<Integer> factAsync(int n) {
        return AsyncPipeline.await(f -> (n >= 1)
                ? factAsync(n - 1).thenApplyAsync(r -> r * n)
                : completedFuture(1))
                .toCompletableFuture();
    }

    @Test
    public void shouldPassContextWithCommonExecutor() throws Exception {

        // Given

        String property = "flowName";
        String flowValue = UUID.randomUUID().toString();
        Collection<Object> asyncFlow = ConcurrentHashMap.newKeySet();

        IAsyncContext context = Asynchronizer.context();
        ExecutorService executor = Asynchronizer.commonPool();

        // When

        context.setProperty(property, flowValue);

        await(run(f -> asyncFlow.add(context.getProperty(property)), executor)
                .run((f, r) -> asyncFlow.add(context.getProperty(property)))
                .run((f, r) -> asyncFlow.add(context.getProperty(property)))
                .run((f, r) -> asyncFlow.add(context.getProperty(property)))
                .run((f, r) -> asyncFlow.add(context.getProperty(property))));

        // Then
        assertThat(asyncFlow).hasSize(1);
        assertThat(asyncFlow).contains(flowValue);
    }

    @Test
    public void shouldPassContextWithCustomExecutor() throws Exception {

        // Given

        String property = "flowName";
        String flowValue = UUID.randomUUID().toString();
        Collection<Object> asyncFlow = ConcurrentHashMap.newKeySet();

        IAsyncContext context = new AsyncContext();
        IExecutorFactory factory = new ExecutorFactory(context);
        ExecutorService executor = factory.newCachedThreadPool(getClass());

        // When

        context.setProperty(property, flowValue);

        await(run(f -> asyncFlow.add(context.getProperty(property)), executor)
                .run((f, r) -> asyncFlow.add(context.getProperty(property)))
                .run((f, r) -> asyncFlow.add(context.getProperty(property)))
                .run((f, r) -> asyncFlow.add(context.getProperty(property)))
                .run((f, r) -> asyncFlow.add(context.getProperty(property))));

        // Then
        assertThat(asyncFlow).hasSize(1);
        assertThat(asyncFlow).contains(flowValue);
    }


    private static <T> T await(IAsyncPipeline<T> pipeline) throws Exception {
        return await(pipeline.toCompletableFuture());
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(5, TimeUnit.SECONDS); // prevent an infinite execution of the test
    }
}
