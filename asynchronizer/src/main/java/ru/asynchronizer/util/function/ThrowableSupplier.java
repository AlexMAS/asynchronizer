package ru.asynchronizer.util.function;

import java.util.function.Supplier;
import lombok.*;

@FunctionalInterface
public interface ThrowableSupplier<T> extends Supplier<T> {

    @Override
    @SneakyThrows
    default T get() {
        return tryGet();
    }

    T tryGet() throws Exception;
}
