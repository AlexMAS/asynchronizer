package ru.asynchronizer.util.concurrent;

@FunctionalInterface
public interface IAsyncRunnable {

    void run(IAsyncFlow flow) throws Throwable;
}
