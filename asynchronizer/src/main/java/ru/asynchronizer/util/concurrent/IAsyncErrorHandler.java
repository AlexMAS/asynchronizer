package ru.asynchronizer.util.concurrent;

@FunctionalInterface
public interface IAsyncErrorHandler {

    void handle(Throwable error);
}
