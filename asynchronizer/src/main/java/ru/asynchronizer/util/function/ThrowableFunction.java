package ru.asynchronizer.util.function;

import java.util.function.Function;
import lombok.*;

@FunctionalInterface
public interface ThrowableFunction<T, R> extends Function<T, R> {

    @Override
    @SneakyThrows
    default R apply(T t) {
        return tryApply(t);
    }

    R tryApply(T t) throws Exception;
}
