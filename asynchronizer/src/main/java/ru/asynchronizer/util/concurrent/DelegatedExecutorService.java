package ru.asynchronizer.util.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * The wrapper class that exposes only the {@link ExecutorService} methods based on a given instance of the {@link ExecutorService}.
 */
public abstract class DelegatedExecutorService implements ExecutorService {

    protected final ExecutorService target;


    protected DelegatedExecutorService(ExecutorService target) {
        this.target = target;
    }


    @Override
    public void shutdown() {
        target.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return target.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return target.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return target.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return target.awaitTermination(timeout, unit);
    }


    @Override
    public void execute(Runnable task) {
        target.execute(wrapTask(task));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return target.submit(wrapTask(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return target.submit(wrapTask(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return target.submit(wrapTask(task));
    }


    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return target.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return target.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return target.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return target.invokeAny(tasks, timeout, unit);
    }


    protected Runnable wrapTask(Runnable task) {
        return task;
    }

    protected <T> Callable<T> wrapTask(Callable<T> task) {
        return task;
    }
}
