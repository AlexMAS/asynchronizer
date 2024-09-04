package ru.asynchronizer.util.concurrent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class DelegatedPriorityExecutorService extends DelegatedExecutorService implements IPriorityExecutorService {

    private final Map<Integer, PriorityExecutorServiceView> views;


    public DelegatedPriorityExecutorService(ExecutorService target) {
        super(target);
        this.views = new ConcurrentHashMap<>();
    }


    @Override
    public ExecutorService withPriority(int priority) {
        return views.computeIfAbsent(priority, p -> new PriorityExecutorServiceView(this, p));
    }


    @Override
    public void shutdown() {
        views.clear();
        super.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        views.clear();
        return super.shutdownNow();
    }
}
