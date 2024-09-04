package ru.asynchronizer.util.concurrent;

@FunctionalInterface
public interface IAsyncThenSupplier<T, R> {

    R get(IAsyncFlow flow, T t) throws Throwable;

    default IAsyncSupplier<R> carry(T t) {
        return flow -> get(flow, t);
    }
}
