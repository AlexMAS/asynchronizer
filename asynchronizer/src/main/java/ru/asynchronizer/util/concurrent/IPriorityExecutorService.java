package ru.asynchronizer.util.concurrent;

import java.util.concurrent.ExecutorService;

/**
 * A thread pool to operate off a prioritized queue.
 */
public interface IPriorityExecutorService extends ExecutorService {

    /**
     * Returns a view for which any task has the given priority.
     *
     * @param priority the task priority
     */
    ExecutorService withPriority(int priority);

    /**
     * Returns a view for which any task has the lowest priority.
     */
    default ExecutorService withLowPriority() {
        return withPriority(IPriorityTask.LOW_PRIORITY);
    }

    /**
     * Returns a view for which any task has the highest priority.
     */
    default ExecutorService withHighPriority() {
        return withPriority(IPriorityTask.HIGH_PRIORITY);
    }
}
