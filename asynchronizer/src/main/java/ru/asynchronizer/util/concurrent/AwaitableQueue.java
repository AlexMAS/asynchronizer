package ru.asynchronizer.util.concurrent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.*;

import ru.asynchronizer.util.IDisposable;

/**
 * Organizes a queue to await provided tasks.
 *
 * <p>
 * Each task in substance represents a synchronous routine. This class provides
 * an ability to await completion of a synchronous task asynchronously.
 *
 * <p>
 * To be able to await a task asynchronously it must implement the {@link IAwaitable}
 * interface and be enqueued with either the {@link #enqueue(IAwaitable)} or
 * {@link #enqueue(IAwaitable, Duration)} methods. These methods return an instance of
 * the {@link CompletableFuture} class which can be used for asynchronous awaiting.
 * The provided future can be safely interrupted as well, for example, with
 * the {@link CompletableFuture#cancel(boolean)} method. When a task is completed
 * it is dequeued automatically.
 *
 * <p>
 * The implementation is based on the single thread executor which awaits enqueued tasks
 * sequentially in the infinite loop providing each task a piece of time to complete.
 * If a task completes in time the appropriate future is completed as well and the task
 * is dequeued.
 *
 * <p>
 * Awaiting all tasks in a single thread allows to utilize CPU effectively - only one thread
 * is in active wait. On the other hand all tasks are awaited sequentially thus in case
 * of long queue fast tasks can await completion longer. This negative effect can be minimized
 * by decreasing the task poll timeout.
 */
public class AwaitableQueue implements IDisposable {

    public static final Duration DEFAULT_TASK_QUEUE_TIMEOUT = Duration.ofMillis(5000);
    public static final Duration DEFAULT_TASK_POLL_TIMEOUT = Duration.ofMillis(100);
    public static final Duration DEFAULT_TASK_AWAIT_TIMEOUT = Duration.ofMinutes(5);

    private final long taskQueueTimeout;
    private final long taskPollTimeout;
    private final Duration taskAwaitTimeout;
    private final Object taskQueueSignal;
    private final Map<Long, AwaitableTask<?>> taskQueue;
    private final AtomicLong taskSequence;
    private final AtomicBoolean disposed;
    private final Future<?> taskQueueHandlerFuture;

    /**
     * Creates a new instance of the queue with default settings.
     *
     * @param taskQueueExecutor the executor to await tasks
     */
    public AwaitableQueue(ExecutorService taskQueueExecutor) {
        this(DEFAULT_TASK_QUEUE_TIMEOUT,
                DEFAULT_TASK_POLL_TIMEOUT,
                DEFAULT_TASK_AWAIT_TIMEOUT,
                taskQueueExecutor);
    }

    /**
     * Creates a new instance of the queue with given timeouts.
     *
     * @param taskQueueTimeout defines how long to sleep if the queue is empty
     * @param taskPollTimeout defines how long to wait for a task completion during the poll cycle
     * @param taskAwaitTimeout defines how long to wait for a task completion before it will be canceled by timeout
     * @param taskQueueExecutor the executor to await tasks
     *
     * @see #DEFAULT_TASK_QUEUE_TIMEOUT
     * @see #DEFAULT_TASK_POLL_TIMEOUT
     * @see #DEFAULT_TASK_AWAIT_TIMEOUT
     */
    public AwaitableQueue(Duration taskQueueTimeout, Duration taskPollTimeout, Duration taskAwaitTimeout, ExecutorService taskQueueExecutor) {
        this.taskQueueTimeout = taskQueueTimeout.toMillis();
        this.taskPollTimeout = taskPollTimeout.toMillis();
        this.taskAwaitTimeout = taskAwaitTimeout;
        this.taskQueueSignal = new Object();
        this.taskQueue = new ConcurrentSkipListMap<>();
        this.taskSequence = new AtomicLong(0);
        this.disposed = new AtomicBoolean(false);
        this.taskQueueHandlerFuture = taskQueueExecutor.submit(this::taskQueueHandler);
    }

    /**
     * Returns the number of tasks in this queue.
     */
    public int size() {
        return taskQueue.size();
    }

    /**
     * Adds the given task to the queue and returns an instance of the {@link CompletableFuture} to await the completion result.
     *
     * @param awaitable the task
     * @param <T> the task result type
     *
     * @return the task future
     */
    public <T> CompletableFuture<T> enqueue(IAwaitable<T> awaitable) {
        return enqueue(awaitable, taskAwaitTimeout);
    }

    /**
     * Adds the given task to the queue and returns an instance of the {@link CompletableFuture} to await the completion result.
     *
     * @param awaitable the task
     * @param timeout the task timeout which defines how long to wait for the task completion before it will be canceled
     * @param <T> the task result type
     *
     * @return the task future
     */
    public <T> CompletableFuture<T> enqueue(IAwaitable<T> awaitable, Duration timeout) {
        if (!disposed.get()) {
            var taskId = taskSequence.getAndIncrement();
            var taskAwaiter = awaitable.getAwaiter();
            var taskFuture = new FinalizableCompletableFuture<T>(() -> removeTask(taskId));
            var task = new AwaitableTask<T>(taskAwaiter, taskFuture);

            if (timeout != null && !timeout.isNegative() && !timeout.isZero()) {
                taskFuture.orTimeout(timeout.toNanos(), TimeUnit.NANOSECONDS);
            }

            taskQueue.put(taskId, task);

            synchronized (taskQueueSignal) {
                taskQueueSignal.notify();
            }

            return taskFuture;
        }

        return CompletableFutureUtil.failed(new IllegalStateException());
    }

    private void taskQueueHandler() {
        while (true) {
            // Wait until the queue is empty
            if (disposed.get()) {
                break;
            } else if (taskQueue.isEmpty()) {
                synchronized (taskQueueSignal) {
                    try {
                        taskQueueSignal.wait(taskQueueTimeout);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (disposed.get()) {
                break;
            } else if (taskQueue.isEmpty()) {
                continue;
            }

            // Await the tasks sequentially
            try {
                for (var entry : taskQueue.entrySet()) {
                    if (disposed.get()) {
                        break;
                    }
                    entry.getValue().await(taskPollTimeout, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        cancelAllTasks();
    }

    private void removeTask(long taskId) {
        @SuppressWarnings("resource")
        var task = taskQueue.remove(taskId);

        if (task != null) {
            task.dispose();
        }
    }

    private void cancelAllTasks() {
        try {
            for (var entry : new ArrayList<>(taskQueue.entrySet())) {
                entry.getValue().cancel();
            }
        } finally {
            taskQueue.clear();
        }
    }

    @Override
    public void dispose() {
        if (!disposed.getAndSet(true)) {
            // Notify the consumer thread to complete
            synchronized (taskQueueSignal) {
                taskQueueSignal.notify();
            }

            // Wait for the consumer thread is completed
            awaitTaskQueueHandlerCompleted();
        }
    }

    protected void awaitTaskQueueHandlerCompleted() {
        if (!taskQueueHandlerFuture.isDone()) {
            try {
                taskQueueHandlerFuture.get();
            } catch (Exception ignore) {
                // Ignore
            }
        }
    }


    @RequiredArgsConstructor
    private static class AwaitableTask<T> implements IDisposable {

        private final IAwaiter<T> awaiter;
        private final CompletableFuture<T> future;

        public void await(long timeout, TimeUnit unit) throws InterruptedException {
            try {
                if (awaiter.await(timeout, unit)) {
                    var taskResult = awaiter.getResult();
                    future.complete(taskResult);
                }
            } catch (CancellationException e) {
                future.cancel(true);
            } catch (ExecutionException e) {
                future.completeExceptionally(e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        }

        public void cancel() {
            future.cancel(true);
        }

        @Override
        public void dispose() {
            awaiter.dispose();
        }
    }
}
