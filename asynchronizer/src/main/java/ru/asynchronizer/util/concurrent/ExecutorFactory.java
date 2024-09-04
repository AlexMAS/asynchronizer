package ru.asynchronizer.util.concurrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.function.Function;

import ru.asynchronizer.util.IDisposable;

public final class ExecutorFactory implements IExecutorFactory, IDisposable {

    private static final IUncaughtExceptionHandler NULL_ERROR_HANDLER = e -> { };

    private final IAsyncContext context;
    private final Collection<IDisposable> executors;
    private final Thread.UncaughtExceptionHandler threadExceptionHandler;
    private final IUncaughtExceptionHandler taskExceptionHandler;
    private volatile IUncaughtExceptionHandler userExceptionHandler;


    public ExecutorFactory(IAsyncContext context) {
        this.context = context;
        this.executors = ConcurrentHashMap.newKeySet();
        this.threadExceptionHandler = this::handleThreadUncaughtException;
        this.taskExceptionHandler = this::handleTaskUncaughtException;
        this.userExceptionHandler = NULL_ERROR_HANDLER;
    }


    @Override
    public ThreadFactory newThreadFactory(Class<?> owner, boolean daemon) {
        return new AsyncContextThreadFactory(owner, daemon, threadExceptionHandler);
    }

    @Override
    public ExecutorService newCachedThreadPool(Class<?> owner, boolean daemon) {
        return createExecutor(owner, daemon, f -> Executors.newCachedThreadPool(f));
    }

    @Override
    public ExecutorService newFixedThreadPool(Class<?> owner, int poolSize, boolean daemon) {
        return createExecutor(owner, daemon, f -> Executors.newFixedThreadPool(poolSize, f));
    }

    @Override
    public IPriorityExecutorService newPriorityFixedThreadPool(Class<?> owner, int poolSize, boolean daemon) {
        return createPriorityExecutor(owner, daemon, f -> new PriorityExecutorService(poolSize, f));
    }

    @Override
    public ExecutorService newSingleThreadExecutor(Class<?> owner, boolean daemon) {
        return createExecutor(owner, daemon, f -> Executors.newSingleThreadExecutor(f));
    }

    @Override
    public IPriorityExecutorService newPrioritySingleThreadExecutor(Class<?> owner, boolean daemon) {
        return createPriorityExecutor(owner, daemon, f -> new PriorityExecutorService(1, f));
    }

    @Override
    public ScheduledExecutorService newScheduledThreadPool(Class<?> owner, int poolSize, boolean daemon) {
        return createScheduledExecutor(owner, daemon, f -> Executors.newScheduledThreadPool(poolSize, f));
    }

    @Override
    public ScheduledExecutorService newSingleThreadScheduledExecutor(Class<?> owner, boolean daemon) {
        return createScheduledExecutor(owner, daemon, f -> Executors.newSingleThreadScheduledExecutor(f));
    }

    @Override
    public IUncaughtExceptionHandler getUncaughtExceptionHandler() {
        return (userExceptionHandler != NULL_ERROR_HANDLER) ? userExceptionHandler : null;
    }

    @Override
    public void setUncaughtExceptionHandler(IUncaughtExceptionHandler exceptionHandler) {
        this.userExceptionHandler = (exceptionHandler != null) ? exceptionHandler : NULL_ERROR_HANDLER;
    }

    @Override
    public void dispose() {

    }

    public void disposeNow() {
        IDisposable.dispose(new ArrayList<>(executors));
        executors.clear();
    }


    private ExecutorService createExecutor(Class<?> owner, boolean daemon, Function<ThreadFactory, ? extends ExecutorService> supplier) {
        var threadFactory = newThreadFactory(owner, daemon);
        var innerExecutor = supplier.apply(threadFactory);
        var contextExecutor = new AsyncContextExecutorService(innerExecutor, owner, context, executors::remove, taskExceptionHandler);
        executors.add(contextExecutor);
        return contextExecutor;
    }

    private IPriorityExecutorService createPriorityExecutor(Class<?> owner, boolean daemon, Function<ThreadFactory, IPriorityExecutorService> supplier) {
        var contextExecutor = createExecutor(owner, daemon, supplier);
        return new DelegatedPriorityExecutorService(contextExecutor);
    }

    private ScheduledExecutorService createScheduledExecutor(Class<?> owner, boolean daemon, Function<ThreadFactory, ScheduledExecutorService> supplier) {
        var threadFactory = newThreadFactory(owner, daemon);
        var innerExecutor = supplier.apply(threadFactory);
        var contextExecutor = new AsyncContextScheduledExecutorService(innerExecutor, owner, context, executors::remove, taskExceptionHandler);
        executors.add(contextExecutor);
        return contextExecutor;
    }

    private void handleThreadUncaughtException(Thread thread, Throwable exception) {
        userExceptionHandler.handle(exception);
    }

    private void handleTaskUncaughtException(Throwable exception) {
        userExceptionHandler.handle(exception);
    }
}
