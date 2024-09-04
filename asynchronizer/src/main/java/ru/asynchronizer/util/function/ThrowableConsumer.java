package ru.asynchronizer.util.function;

import java.util.function.Consumer;
import lombok.*;

@FunctionalInterface
public interface ThrowableConsumer<T> extends Consumer<T> {

    @Override
    @SneakyThrows
    default void accept(T t) {
        tryAccept(t);
    }

    void tryAccept(T t) throws Exception;
}
