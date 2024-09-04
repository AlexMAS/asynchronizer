package ru.asynchronizer.util.concurrent;

import java.util.List;
import java.util.concurrent.ExecutorService;

import ru.asynchronizer.util.IDisposable;

/**
 * Wraps a given instance of the {@link ExecutorService} to ignore invoking {@link #shutdown()} and {@link #shutdownNow()}.
 *
 * <p>
 * This class can be used to protect a shared instance of the {@link ExecutorService}.
 */
public class CommonExecutorService extends DelegatedExecutorService implements IDisposable {

    public CommonExecutorService(ExecutorService target) {
        super(target);
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
