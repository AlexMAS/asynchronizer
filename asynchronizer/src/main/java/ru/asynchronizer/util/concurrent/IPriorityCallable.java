package ru.asynchronizer.util.concurrent;

import java.util.concurrent.Callable;

public interface IPriorityCallable<R> extends IPriorityTask, Callable<R> {

    static int getPriority(Callable<?> task) {
        return (task instanceof IPriorityTask)
                ? ((IPriorityTask) task).priority()
                : IPriorityTask.LOW_PRIORITY;
    }

    static <R> IPriorityCallable<R> withLowPriority(Callable<R> task) {
        return withPriority(LOW_PRIORITY, task);
    }

    static <R> IPriorityCallable<R> withHighPriority(Callable<R> task) {
        return withPriority(HIGH_PRIORITY, task);
    }

    static <R> IPriorityCallable<R> withPriority(int priority, Callable<R> task) {

        return new IPriorityCallable<>() {

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public R call() throws Exception {
                return task.call();
            }
        };
    }
}
