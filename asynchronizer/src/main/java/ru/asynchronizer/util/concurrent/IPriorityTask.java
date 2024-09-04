package ru.asynchronizer.util.concurrent;

/**
 * The priority task.
 *
 * <p>
 * The higher the priority, the more chances the task will be processed sooner.
 *
 * @see IPriorityRunnable
 * @see IPriorityCallable
 */
public interface IPriorityTask {

    /**
     * The lowest priority.
     */
    int LOW_PRIORITY = Integer.MIN_VALUE;

    /**
     * The highest priority.
     */
    int HIGH_PRIORITY = Integer.MAX_VALUE;


    /**
     * Gets the task priority.
     */
    int priority();
}
