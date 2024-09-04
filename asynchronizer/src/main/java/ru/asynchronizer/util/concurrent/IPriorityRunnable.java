package ru.asynchronizer.util.concurrent;

public interface IPriorityRunnable extends IPriorityTask, Runnable {

    static int getPriority(Runnable task) {
        return (task instanceof IPriorityTask)
                ? ((IPriorityTask) task).priority()
                : IPriorityTask.LOW_PRIORITY;
    }

    static IPriorityRunnable withLowPriority(Runnable task) {
        return withPriority(LOW_PRIORITY, task);
    }

    static IPriorityRunnable withHighPriority(Runnable task) {
        return withPriority(HIGH_PRIORITY, task);
    }

    static IPriorityRunnable withPriority(int priority, Runnable task) {

        return new IPriorityRunnable() {

            @Override
            public int priority() {
                return priority;
            }

            @Override
            public void run() {
                task.run();
            }
        };
    }
}
