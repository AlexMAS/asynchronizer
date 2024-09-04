package ru.asynchronizer.util.concurrent;

import java.util.concurrent.Callable;

final class AsyncContextCallable<T> implements Callable<T>, IPriorityTask {

    private final Callable<T> target;
    private final IAsyncContextCapture parentContext;
    private final IUncaughtExceptionHandler exceptionHandler;


    public AsyncContextCallable(Callable<T> target, IAsyncContext context, IUncaughtExceptionHandler exceptionHandler) {
        this.target = target;
        this.parentContext = context.capture();
        this.exceptionHandler = exceptionHandler;
    }


    @Override
    public int priority() {
        return IPriorityCallable.getPriority(target);
    }

    @Override
    public T call() throws Exception {
        try (var ignored = parentContext.use()) {
            return target.call();
        } catch (RuntimeException e) {
            exceptionHandler.handle(e);
            throw e;
        }
    }
}
