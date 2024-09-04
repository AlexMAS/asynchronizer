package ru.asynchronizer.util.concurrent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public final class PriorityExecutorService extends ThreadPoolExecutor implements IPriorityExecutorService {

    private final Map<Integer, PriorityExecutorServiceView> views;


    public PriorityExecutorService(int poolSize, ThreadFactory threadFactory) {
        super(poolSize, poolSize,
                0L, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<>(11, PriorityExecutorService::compare),
                threadFactory);

        this.views = new ConcurrentHashMap<>();
    }


    private static int compare(Runnable x, Runnable y) {
        return PriorityRunnableFuture.compare(x, y);
    }


    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        var task = super.newTaskFor(runnable, value);
        var priority = IPriorityRunnable.getPriority(runnable);
        return new PriorityRunnableFuture<>(priority, task);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        var task = super.newTaskFor(callable);
        var priority = IPriorityCallable.getPriority(callable);
        return new PriorityRunnableFuture<>(priority, task);
    }


    @Override
    public ExecutorService withPriority(int priority) {
        return views.computeIfAbsent(priority, p -> new PriorityExecutorServiceView(this, p));
    }


    @Override
    public void shutdown() {
        views.clear();
        super.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        views.clear();
        return super.shutdownNow();
    }
}
