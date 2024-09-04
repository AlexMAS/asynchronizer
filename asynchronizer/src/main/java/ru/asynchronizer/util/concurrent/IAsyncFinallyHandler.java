package ru.asynchronizer.util.concurrent;

@FunctionalInterface
public interface IAsyncFinallyHandler<T> {

    void handle(T result, Throwable error);
}
