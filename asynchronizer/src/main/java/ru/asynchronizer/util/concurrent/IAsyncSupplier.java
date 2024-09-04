package ru.asynchronizer.util.concurrent;

@FunctionalInterface
public interface IAsyncSupplier<T> {

    T get(IAsyncFlow flow) throws Throwable;
}
