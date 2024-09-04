package ru.asynchronizer.util.concurrent;

import java.util.List;
import java.util.concurrent.*;

import ru.asynchronizer.util.IDisposable;

/**
 * Wraps a given instance of the {@link ExecutorService} to ignore invoking {@link #shutdown()} and {@link #shutdownNow()}.
 *
 * <p>
 * This class can be used to protect a shared instance of the {@link ScheduledExecutorService}.
 * The underlying scheduled executor only schedule tasks, real processing is performed in the given common pool.
 * It is very important because this technique allows to eliminate exhausting of the target scheduled pool.
 */
public class CommonScheduledExecutorService extends DelegatedExecutorService implements ScheduledExecutorService, IDisposable {

    private final ScheduledExecutorService target;


    public CommonScheduledExecutorService(ScheduledExecutorService target, ExecutorService commonPool) {
        super(commonPool);
        this.target = target;
    }


    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return target.schedule(delegateTask(task), delay, unit);
    }

    @Override
    public <T> ScheduledFuture<T> schedule(Callable<T> task, long delay, TimeUnit unit) {
        return target.schedule(delegateTask(task), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return target.scheduleAtFixedRate(delegateTask(task), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        return target.scheduleWithFixedDelay(delegateTask(task), initialDelay, delay, unit);
    }


    private Runnable delegateTask(Runnable task) {
        return () -> submit(task);
    }

    private <T> Callable<T> delegateTask(Callable<T> task) {
        return () -> {
            submit(task);
            return null;
        };
    }


    @Override
    public void shutdown() {

    }

    @Override
    public List<Runnable> shutdownNow() {
        return List.of();
    }


    public void dispose() {
        target.shutdown();
    }

    @Override
    public void close() {

    }
}
