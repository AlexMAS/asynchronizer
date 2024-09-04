package ru.asynchronizer.util.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/**
 * Provides factory methods to create {@link ExecutorService} and {@link ScheduledExecutorService}.
 */
public interface IExecutorFactory {

    /**
     * Creates the thread factory.
     *
     * @param owner the class which will use the created threads
     *
     * @return the newly created thread factory
     *
     * @see #newCachedThreadPool(Class, boolean)
     */
    default ThreadFactory newThreadFactory(Class<?> owner) {
        return newThreadFactory(owner, true);
    }

    /**
     * Creates the thread factory.
     *
     * @param owner the class which will use the created threads
     * @param daemon if {@code true}, all threads in the pool will be {@linkplain Thread#isDaemon() daemons}
     *
     * @return the newly created thread factory
     *
     * @see #newThreadFactory(Class)
     */
    ThreadFactory newThreadFactory(Class<?> owner, boolean daemon);


    /**
     * Creates a thread pool that creates new threads as needed, but will reuse previously constructed threads when they are available.
     *
     * <p>
     * All threads in the pool will be {@linkplain Thread#isDaemon() daemons}.
     *
     * @param owner the class which will use the created thread pool
     *
     * @return the newly created thread pool
     *
     * @see #newCachedThreadPool(Class, boolean)
     */
    default ExecutorService newCachedThreadPool(Class<?> owner) {
        return newCachedThreadPool(owner, true);
    }

    /**
     * Creates a thread pool that creates new threads as needed, but will reuse previously constructed threads when they are available.
     *
     * @param owner the class which will use the created thread pool
     * @param daemon if {@code true}, all threads in the pool will be {@linkplain Thread#isDaemon() daemons}
     *
     * @return the newly created thread pool
     *
     * @see #newCachedThreadPool(Class)
     */
    ExecutorService newCachedThreadPool(Class<?> owner, boolean daemon);


    /**
     * Creates a thread pool that reuses a fixed number of threads operating off a shared unbounded queue.
     *
     * <p>
     * At any point, at most {@code poolSize} threads will be active processing tasks. If additional tasks
     * are submitted when all threads are active, they will wait in the queue until a thread is available.
     * All threads in the pool will be {@linkplain Thread#isDaemon() daemons}.
     *
     * @param owner the class which will use the created thread pool
     * @param poolSize the number of threads in the pool
     *
     * @return the newly created thread pool
     *
     * @see #newFixedThreadPool(Class, int, boolean)
     */
    default ExecutorService newFixedThreadPool(Class<?> owner, int poolSize) {
        return newFixedThreadPool(owner, poolSize, true);
    }

    /**
     * Creates a thread pool that reuses a fixed number of threads operating off a shared unbounded queue.
     *
     * <p>
     * At any point, at most {@code poolSize} threads will be active processing tasks. If additional tasks
     * are submitted when all threads are active, they will wait in the queue until a thread is available.
     *
     * @param owner the class which will use the created thread pool
     * @param poolSize the number of threads in the pool
     * @param daemon if {@code true}, all threads in the pool will be {@linkplain Thread#isDaemon() daemons}
     *
     * @return the newly created thread pool
     *
     * @see #newFixedThreadPool(Class, int)
     */
    ExecutorService newFixedThreadPool(Class<?> owner, int poolSize, boolean daemon);


    /**
     * Creates a thread pool that reuses a fixed number of threads operating off a prioritized queue.
     *
     * <p>
     * At any point, at most {@code poolSize} threads will be active processing tasks. If additional tasks
     * are submitted when all threads are active, they will wait in the queue until a thread is available.
     * All added tasks are sorted and processed in accordance with their {@linkplain IPriorityTask priorities}.
     * All threads in the pool will be {@linkplain Thread#isDaemon() daemons}.
     *
     * @param owner the class which will use the created thread pool
     * @param poolSize the number of threads in the pool
     *
     * @return the newly created thread pool
     *
     * @see #newPriorityFixedThreadPool(Class, int, boolean)
     * @see IPriorityTask
     * @see IPriorityRunnable
     * @see IPriorityCallable
     */
    default IPriorityExecutorService newPriorityFixedThreadPool(Class<?> owner, int poolSize) {
        return newPriorityFixedThreadPool(owner, poolSize, true);
    }

    /**
     * Creates a thread pool that reuses a fixed number of threads operating off a prioritized queue.
     *
     * <p>
     * At any point, at most {@code poolSize} threads will be active processing tasks. If additional tasks
     * are submitted when all threads are active, they will wait in the queue until a thread is available.
     * All added tasks are sorted and processed in accordance with their {@linkplain IPriorityTask priorities}.
     *
     * @param owner the class which will use the created thread pool
     * @param poolSize the number of threads in the pool
     * @param daemon if {@code true}, all threads in the pool will be {@linkplain Thread#isDaemon() daemons}
     *
     * @return the newly created thread pool
     *
     * @see #newPriorityFixedThreadPool(Class, int)
     * @see IPriorityTask
     * @see IPriorityRunnable
     * @see IPriorityCallable
     */
    IPriorityExecutorService newPriorityFixedThreadPool(Class<?> owner, int poolSize, boolean daemon);


    /**
     * Creates an executor that uses a single worker thread operating off an unbounded queue.
     *
     * <p>
     * Tasks are guaranteed to execute sequentially, and no more than one task will be active at any given time.
     * The thread in the pool will be a {@linkplain Thread#isDaemon() daemon}.
     *
     * @param owner the class which will use the created thread pool
     *
     * @return the newly created thread pool
     *
     * @see #newSingleThreadExecutor(Class, boolean)
     */
    default ExecutorService newSingleThreadExecutor(Class<?> owner) {
        return newSingleThreadExecutor(owner, true);
    }

    /**
     * Creates an executor that uses a single worker thread operating off an unbounded queue.
     *
     * <p>
     * Tasks are guaranteed to execute sequentially, and no more than one task will be active at any given time.
     *
     * @param owner the class which will use the created thread pool
     * @param daemon if {@code true}, the thread in the pool will be a {@linkplain Thread#isDaemon() daemon}
     *
     * @return the newly created thread pool
     *
     * @see #newSingleThreadExecutor(Class)
     */
    ExecutorService newSingleThreadExecutor(Class<?> owner, boolean daemon);


    /**
     * Creates an executor that uses a single worker thread operating off a prioritized queue.
     *
     * <p>
     * Tasks are guaranteed to execute sequentially in accordance with their {@linkplain IPriorityTask priorities},
     * and no more than one task will be active at any given time.
     * The thread in the pool will be a {@linkplain Thread#isDaemon() daemon}.
     *
     * @param owner the class which will use the created thread pool
     *
     * @return the newly created thread pool
     *
     * @see #newPrioritySingleThreadExecutor(Class, boolean)
     * @see IPriorityTask
     * @see IPriorityRunnable
     * @see IPriorityCallable
     */
    default IPriorityExecutorService newPrioritySingleThreadExecutor(Class<?> owner) {
        return newPrioritySingleThreadExecutor(owner, true);
    }

    /**
     * Creates an executor that uses a single worker thread operating off a prioritized queue.
     *
     * <p>
     * Tasks are guaranteed to execute sequentially in accordance with their {@linkplain IPriorityTask priorities},
     * and no more than one task will be active at any given time.
     *
     * @param owner the class which will use the created thread pool
     * @param daemon if {@code true}, the thread in the pool will be a {@linkplain Thread#isDaemon() daemon}
     *
     * @return the newly created thread pool
     *
     * @see #newPrioritySingleThreadExecutor(Class)
     * @see IPriorityTask
     * @see IPriorityRunnable
     * @see IPriorityCallable
     */
    IPriorityExecutorService newPrioritySingleThreadExecutor(Class<?> owner, boolean daemon);


    /**
     * Creates a thread pool that can schedule commands to run after a given delay, or to execute periodically.
     *
     * <p>
     * All threads in the pool will be {@linkplain Thread#isDaemon() daemons}.
     *
     * @param owner the class which will use the created thread pool
     * @param poolSize the number of threads in the pool
     *
     * @return the newly created thread pool
     *
     * @see #newScheduledThreadPool(Class, int, boolean)
     */
    default ScheduledExecutorService newScheduledThreadPool(Class<?> owner, int poolSize) {
        return newScheduledThreadPool(owner, poolSize, true);
    }

    /**
     * Creates a thread pool that can schedule commands to run after a given delay, or to execute periodically.
     *
     * @param owner the class which will use the created thread pool
     * @param poolSize the number of threads in the pool
     * @param daemon if {@code true}, all threads in the pool will be {@linkplain Thread#isDaemon() daemons}
     *
     * @return the newly created thread pool
     *
     * @see #newScheduledThreadPool(Class, int, boolean)
     */
    ScheduledExecutorService newScheduledThreadPool(Class<?> owner, int poolSize, boolean daemon);


    /**
     * Creates a single-threaded executor that can schedule commands to run after a given delay, or to execute periodically.
     *
     * <p>
     * Tasks are guaranteed to execute sequentially, and no more than one task will be active at any given time.
     * The thread in the pool will be a {@linkplain Thread#isDaemon() daemon}.
     *
     * @param owner the class which will use the created thread pool
     *
     * @return the newly created thread pool
     *
     * @see #newSingleThreadScheduledExecutor(Class, boolean)
     */
    default ScheduledExecutorService newSingleThreadScheduledExecutor(Class<?> owner) {
        return newSingleThreadScheduledExecutor(owner, true);
    }

    /**
     * Creates a single-threaded executor that can schedule commands to run after a given delay, or to execute periodically.
     *
     * <p>
     * Tasks are guaranteed to execute sequentially, and no more than one task will be active at any given time.
     *
     * @param owner the class which will use the created thread pool
     * @param daemon if {@code true}, the thread in the pool will be a {@linkplain Thread#isDaemon() daemon}
     *
     * @return the newly created thread pool
     *
     * @see #newSingleThreadScheduledExecutor(Class)
     */
    ScheduledExecutorService newSingleThreadScheduledExecutor(Class<?> owner, boolean daemon);


    /**
     * Gets the handler invoked when a thread faced with an uncaught exception.
     */
    IUncaughtExceptionHandler getUncaughtExceptionHandler();

    /**
     * Sets the handler invoked when a thread faced with an uncaught exception.
     */
    void setUncaughtExceptionHandler(IUncaughtExceptionHandler exceptionHandler);
}
