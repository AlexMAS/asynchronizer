package ru.asynchronizer.util.function;

import java.util.function.Function;
import java.util.function.Supplier;

public final class SingletonSupplier<T> implements Supplier<T> {

    private final Supplier<T> delegate;
    private volatile T instance;


    public SingletonSupplier(Supplier<T> delegate) {
        this.delegate = delegate;
    }


    public static <T> SingletonSupplier<T> of(Supplier<T> delegate) {
        return (delegate instanceof SingletonSupplier<?>)
                ? (SingletonSupplier<T>) delegate
                : new SingletonSupplier<>(delegate);
    }

    public static <T> SingletonSupplier<T> ofThrowable(ThrowableSupplier<T> delegate) {
        return new SingletonSupplier<>(delegate);
    }


    public boolean isValueCreated() {
        return (instance != null);
    }

    @Override
    public T get() {
        if (instance == null) {
            synchronized (delegate) {
                if (instance == null) {
                    instance = delegate.get();
                }
            }
        }

        return instance;
    }


    public <R> SingletonSupplier<R> getThen(Function<T, R> selector) {
        return new SingletonSupplier<>(() -> selector.apply(get()));
    }
}
