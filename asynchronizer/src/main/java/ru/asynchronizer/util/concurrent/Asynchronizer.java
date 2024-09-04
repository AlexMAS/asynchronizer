package ru.asynchronizer.util.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * The entry point to support passing the asynchronous context.
 *
 * @see IAsyncContext
 */
public final class Asynchronizer {

    private static final AsyncContext CONTEXT = new AsyncContext();
    private static final ExecutorFactory EXECUTOR_FACTORY = new ExecutorFactory(CONTEXT);
    private static final CommonExecutorService COMMON_POOL = new CommonExecutorService(EXECUTOR_FACTORY.newCachedThreadPool(Asynchronizer.class));
    private static final CommonScheduledExecutorService COMMON_SCHEDULED_POOL = new CommonScheduledExecutorService(EXECUTOR_FACTORY.newScheduledThreadPool(Asynchronizer.class, 5), COMMON_POOL);


    private Asynchronizer() {
        // Static only class
    }


    /**
     * Returns an {@link IAsyncContext} instance that must be used across the application.
     */
    public static IAsyncContext context() {
        return CONTEXT;
    }

    /**
     * Returns an {@link IExecutorFactory} instance that must be used to create new executors.
     */
    public static IExecutorFactory executorFactory() {
        return EXECUTOR_FACTORY;
    }

    /**
     * Returns an {@link ExecutorService} instance that can be used in case when there is no special executor defined/needed.
     */
    public static ExecutorService commonPool() {
        return COMMON_POOL;
    }

    /**
     * Returns an {@link ScheduledExecutorService} instance that can be used in case when there is no special executor defined/needed.
     */
    public static ScheduledExecutorService commonScheduledPool() {
        return COMMON_SCHEDULED_POOL;
    }


    /**
     * Gets the handler invoked when a thread faced with an uncaught exception.
     */
    public static IUncaughtExceptionHandler getUncaughtExceptionHandler() {
        return executorFactory().getUncaughtExceptionHandler();
    }

    /**
     * Sets the handler invoked when a thread faced with an uncaught exception.
     */
    public static void setUncaughtExceptionHandler(IUncaughtExceptionHandler exceptionHandler) {
        executorFactory().setUncaughtExceptionHandler(exceptionHandler);

        if (exceptionHandler != null) {
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> exceptionHandler.handle(e));
        } else {
            Thread.setDefaultUncaughtExceptionHandler(null);
        }
    }


    /**
     * Terminates all asynchronous activity.
     */
    public static void shutdown() {
        COMMON_SCHEDULED_POOL.dispose();
        COMMON_POOL.dispose();
        EXECUTOR_FACTORY.disposeNow();
        CONTEXT.dispose();
    }
}
