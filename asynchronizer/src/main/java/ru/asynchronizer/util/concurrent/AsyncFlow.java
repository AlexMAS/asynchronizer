package ru.asynchronizer.util.concurrent;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

final class AsyncFlow implements IAsyncFlow {

    private final Executor executor;
    private final AtomicBoolean interrupted;
    private Object result;


    public AsyncFlow(Executor executor) {
        this.executor = executor;
        this.interrupted = new AtomicBoolean(false);
        this.result = null;
    }


    @Override
    public Executor executor() {
        return executor;
    }

    @Override
    public void interrupt(Object result) {
        if (!interrupted.getAndSet(true)) {
            this.result = result;
        }
    }

    public boolean isInterrupted() {
        return interrupted.get();
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult() {
        return (T) result;
    }
}
