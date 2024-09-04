package ru.asynchronizer.util.function;

import lombok.*;

@FunctionalInterface
public interface ThrowableRunnable extends Runnable {

    @Override
    @SneakyThrows
    default void run() {
        tryRun();
    }

    void tryRun() throws Exception;
}
