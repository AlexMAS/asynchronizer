package ru.asynchronizer.util.concurrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class AsyncContextThreadFactory implements ThreadFactory {

    private static final Map<Class<?>, AtomicInteger> POOL_NUMBER = new ConcurrentHashMap<>();

    private final boolean daemon;
    private final Thread.UncaughtExceptionHandler exceptionHandler;
    private final ThreadGroup threadGroup;
    private final AtomicInteger threadNumber;
    private final String threadNamePrefix;


    public AsyncContextThreadFactory(Class<?> owner, boolean daemon, Thread.UncaughtExceptionHandler exceptionHandler) {
        this.daemon = daemon;
        this.exceptionHandler = exceptionHandler;
        this.threadGroup = Thread.currentThread().getThreadGroup();
        this.threadNumber = new AtomicInteger(1);
        this.threadNamePrefix = String.format("%s-pool-%d-thread-", owner.getSimpleName(), getPoolNumber(owner));
    }


    @Override
    public Thread newThread(Runnable action) {
        var threadName = threadNamePrefix + threadNumber.getAndIncrement();
        var thread = new Thread(threadGroup, action, threadName);
        thread.setDaemon(daemon);
        thread.setUncaughtExceptionHandler(exceptionHandler);

        if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
        }

        return thread;
    }


    private static int getPoolNumber(Class<?> owner) {
        return POOL_NUMBER.computeIfAbsent(owner,
                        c -> new AtomicInteger(1))
                .getAndIncrement();
    }
}
