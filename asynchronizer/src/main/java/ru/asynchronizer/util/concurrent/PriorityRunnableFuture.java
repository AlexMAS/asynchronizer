package ru.asynchronizer.util.concurrent;

import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicLong;

public final class PriorityRunnableFuture<T> extends DelegatedRunnableFuture<T> implements IPriorityRunnable, Comparable<Runnable> {

    private static final AtomicLong sequence = new AtomicLong(0);

    private final long order;
    private final int priority;


    public PriorityRunnableFuture(int priority, RunnableFuture<T> target) {
        super(target);
        this.order = sequence.getAndIncrement();
        this.priority = priority;
    }


    @Override
    public int priority() {
        return priority;
    }


    @Override
    @SuppressWarnings("NullableProblems")
    public int compareTo(Runnable another) {
        return compare(this, another);
    }

    public static int compare(Runnable x, Runnable y) {
        int priorityX;
        long orderX;

        if (x instanceof PriorityRunnableFuture<?> priorityRunnableX) {
            priorityX = priorityRunnableX.priority;
            orderX = priorityRunnableX.order;
        } else {
            priorityX = IPriorityRunnable.getPriority(x);
            orderX = 0;
        }

        int priorityY;
        long orderY;

        if (y instanceof PriorityRunnableFuture<?> priorityRunnableY) {
            priorityY = priorityRunnableY.priority;
            orderY = priorityRunnableY.order;
        } else {
            priorityY = IPriorityRunnable.getPriority(y);
            orderY = 0;
        }

        var result = Integer.compare(priorityY, priorityX);

        if (result != 0) {
            return result;
        }

        return Long.compare(orderX, orderY);
    }
}
