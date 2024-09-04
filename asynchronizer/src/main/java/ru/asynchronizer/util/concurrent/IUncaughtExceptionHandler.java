package ru.asynchronizer.util.concurrent;

@FunctionalInterface
public interface IUncaughtExceptionHandler {

    void handle(Throwable exception);
}
