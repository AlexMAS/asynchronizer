package ru.asynchronizer.util.concurrent;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

final class PriorityExecutorServiceView extends DelegatedExecutorService {

    private final int priority;


    public PriorityExecutorServiceView(ExecutorService parent, int priority) {
        super(parent);
        this.priority = priority;
    }


    @Override
    protected Runnable wrapTask(Runnable task) {
        return IPriorityRunnable.withPriority(priority, task);
    }

    @Override
    protected <T> Callable<T> wrapTask(Callable<T> task) {
        return IPriorityCallable.withPriority(priority, task);
    }


    @Override
    public void shutdown() {

    }

    @Override
    public List<Runnable> shutdownNow() {
        return List.of();
    }
}
