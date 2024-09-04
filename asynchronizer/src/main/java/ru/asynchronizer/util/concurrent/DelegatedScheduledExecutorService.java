package ru.asynchronizer.util.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The wrapper class that exposes only the {@link ScheduledExecutorService} methods based on a given instance of the {@link ScheduledExecutorService}.
 */
public abstract class DelegatedScheduledExecutorService extends DelegatedExecutorService implements ScheduledExecutorService {

    protected final ScheduledExecutorService target;


    protected DelegatedScheduledExecutorService(ScheduledExecutorService target) {
        super(target);
        this.target = target;
    }


    @Override
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return target.schedule(wrapTask(task), delay, unit);
    }

    @Override
    public <T> ScheduledFuture<T> schedule(Callable<T> task, long delay, TimeUnit unit) {
        return target.schedule(wrapTask(task), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return target.scheduleAtFixedRate(wrapTask(task), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long delay, TimeUnit unit) {
        return target.scheduleWithFixedDelay(wrapTask(task), initialDelay, delay, unit);
    }
}
