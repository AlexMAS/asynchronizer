package ru.asynchronizer.util.concurrent;

@FunctionalInterface
public interface IAsyncThenRunnable<T> {

    void run(IAsyncFlow flow, T t) throws Throwable;

    default IAsyncRunnable carry(T t) {
        return flow -> run(flow, t);
    }
}
