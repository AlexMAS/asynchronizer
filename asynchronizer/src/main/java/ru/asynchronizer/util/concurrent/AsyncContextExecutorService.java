package ru.asynchronizer.util.concurrent;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.*;

import ru.asynchronizer.util.IDisposable;

@Slf4j
final class AsyncContextExecutorService extends DelegatedExecutorService implements IDisposable {

    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(2);

    private final Class<?> owner;
    private final IAsyncContext context;
    private final ShutdownAction shutdownAction;
    private final IUncaughtExceptionHandler exceptionHandler;


    public AsyncContextExecutorService(ExecutorService target, Class<?> owner, IAsyncContext context, ShutdownAction shutdownAction, IUncaughtExceptionHandler exceptionHandler) {
        super(target);
        this.owner = owner;
        this.context = context;
        this.exceptionHandler = exceptionHandler;
        this.shutdownAction = shutdownAction;
    }


    @Override
    protected Runnable wrapTask(Runnable task) {
        return new AsyncContextRunnable(task, context, exceptionHandler);
    }

    @Override
    protected <T> Callable<T> wrapTask(Callable<T> task) {
        return new AsyncContextCallable<>(task, context, exceptionHandler);
    }


    @Override
    public void shutdown() {
        var success = shutdownAction.invoke(this);

        if (!success) {
            log.atWarn()
                    .setMessage("The executor has already been shut down. Owner: {owner}.")
                    .addKeyValue("owner", owner)
                    .log();
            return;
        }

        super.shutdown();

        try {
            if (!super.awaitTermination(SHUTDOWN_TIMEOUT.getSeconds(), TimeUnit.SECONDS)) {
                super.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.atError()
                    .setMessage("The executor has been shut down with errors. Owner: {owner}.")
                    .addKeyValue("owner", owner)
                    .setCause(e)
                    .log();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        var success = shutdownAction.invoke(this);

        if (!success) {
            log.atWarn()
                    .setMessage("The executor has already been shut down. Owner: {owner}.")
                    .addKeyValue("owner", owner)
                    .log();
            return Collections.emptyList();
        }

        return super.shutdownNow();
    }


    @Override
    public void dispose() {
        log.atWarn()
                .setMessage("The executor has not been shut down after usage. Owner: {owner}.")
                .addKeyValue("owner", owner)
                .log();
        shutdown();
    }


    interface ShutdownAction {

        boolean invoke(AsyncContextExecutorService executor);
    }
}
