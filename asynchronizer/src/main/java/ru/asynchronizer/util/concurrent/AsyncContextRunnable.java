package ru.asynchronizer.util.concurrent;

final class AsyncContextRunnable implements Runnable, IPriorityTask {

    private final Runnable target;
    private final IAsyncContextCapture parentContext;
    private final IUncaughtExceptionHandler exceptionHandler;


    public AsyncContextRunnable(Runnable target, IAsyncContext context, IUncaughtExceptionHandler exceptionHandler) {
        this.target = target;
        this.parentContext = context.capture();
        this.exceptionHandler = exceptionHandler;
    }


    @Override
    public int priority() {
        return IPriorityRunnable.getPriority(target);
    }

    @Override
    public void run() {
        try (var ignored = parentContext.use()) {
            target.run();
        } catch (RuntimeException e) {
            exceptionHandler.handle(e);
            throw e;
        }
    }
}
