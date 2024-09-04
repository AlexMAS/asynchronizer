package ru.asynchronizer.util.concurrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

import org.junit.jupiter.api.Test;

public class ForkJoinPoolTest {

    private static final int POOL_SIZE = 5;
    private static final int FAST_TASK_COUNT = 10;
    private static final int SLOW_TASK_COUNT = 10;
    private static final int SLOW_TASK_DURATION = 1000; // ms


    @Test
    public void cachedThreadPoolTest() throws Exception {

        // Given
        CountDownLatch completed = new CountDownLatch(FAST_TASK_COUNT + SLOW_TASK_COUNT);
        Executor executor = Executors.newCachedThreadPool();

        // When

        for (int i = 0; i < SLOW_TASK_COUNT; i++) {
            executor.execute(() -> slowTask(completed));
        }

        for (int i = 0; i < FAST_TASK_COUNT; i++) {
            executor.execute(() -> fastTask(completed));
        }

        long startTime = System.currentTimeMillis();
        completed.await();
        long endTime = System.currentTimeMillis();

        // Then
        System.out.format("Elapsed: %s ms", endTime - startTime);
    }

    @Test
    public void fixedThreadPoolTest() throws Exception {

        // Given
        CountDownLatch completed = new CountDownLatch(FAST_TASK_COUNT + SLOW_TASK_COUNT);
        Executor executor = Executors.newFixedThreadPool(POOL_SIZE);

        // When

        for (int i = 0; i < SLOW_TASK_COUNT; i++) {
            executor.execute(() -> slowTask(completed));
        }

        for (int i = 0; i < FAST_TASK_COUNT; i++) {
            executor.execute(() -> fastTask(completed));
        }

        long startTime = System.currentTimeMillis();
        completed.await();
        long endTime = System.currentTimeMillis();

        // Then
        System.out.format("Elapsed: %s ms", endTime - startTime);
    }

    @Test
    public void forkJoinCommonPoolTest() throws Exception {

        // Given
        CountDownLatch completed = new CountDownLatch(FAST_TASK_COUNT + SLOW_TASK_COUNT);
        Executor executor = ForkJoinPool.commonPool();

        // When

        for (int i = 0; i < SLOW_TASK_COUNT; i++) {
            executor.execute(() -> slowTask(completed));
        }

        for (int i = 0; i < FAST_TASK_COUNT; i++) {
            executor.execute(() -> fastTask(completed));
        }

        long startTime = System.currentTimeMillis();
        completed.await();
        long endTime = System.currentTimeMillis();

        // Then
        System.out.format("Elapsed: %s ms", endTime - startTime);
    }

    @Test
    public void commonPoolTest() throws Exception {

        // Given
        CountDownLatch completed = new CountDownLatch(FAST_TASK_COUNT + SLOW_TASK_COUNT);
        Executor executor = Asynchronizer.commonPool();

        // When

        for (int i = 0; i < SLOW_TASK_COUNT; i++) {
            executor.execute(() -> slowTask(completed));
        }

        for (int i = 0; i < FAST_TASK_COUNT; i++) {
            executor.execute(() -> fastTask(completed));
        }

        long startTime = System.currentTimeMillis();
        completed.await();
        long endTime = System.currentTimeMillis();

        // Then
        System.out.format("Elapsed: %s ms", endTime - startTime);
    }


    private static void slowTask(CountDownLatch completed) {
        try {
            Thread.sleep(SLOW_TASK_DURATION);
        } catch (InterruptedException e) {
            // Ignore
        }
        completed.countDown();
    }

    private static void fastTask(CountDownLatch completed) {
        completed.countDown();
    }
}
