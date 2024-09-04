package ru.asynchronizer.util.concurrent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import lombok.extern.slf4j.*;

import ru.asynchronizer.util.IDisposable;

import static ru.asynchronizer.util.concurrent.Asynchronizer.commonPool;

/**
 * The implementation of the {@link IQueueDispatcher}.
 *
 * <p>
 * The implementation buffers incoming items in the internal queue and process
 * they asynchronously with batching. The moments of processing incoming items
 * depend on two criteria which can be defined in the constructor:
 *
 * <ul>
 *     <li>{@literal bufferSize} - when the queue grows larger than this value</li>
 *     <li>{@literal bufferTimeout} - when items in the queue stay longer than this value</li>
 * </ul>
 *
 * Either of these two conditions starts the queue processing.
 *
 * @param <T> the type of processed items
 */
@Slf4j
public class QueueDispatcher<T> implements IQueueDispatcher<T> {

    public static final int DEFAULT_BUFFER_SIZE = 5_000;
    public static final Duration DEFAULT_BUFFER_TIMEOUT = Duration.ofSeconds(5);

    private final IQueueHandler<T> handler;
    private final int bufferSize;
    private final long bufferTimeout;
    private final LinkedBlockingQueue<T> buffer;
    private final Object notifyObject;
    private final AtomicBoolean disposed;
    private final ExecutorService handlingExecutor;
    private final Future<?> handlingTask;
    private final AtomicBoolean observersNotifiedAboutFailure;
    private final Collection<Runnable> successObservers;
    private final Collection<Consumer<Throwable>> failureObservers;


    /**
     * Creates a new instance of the class with default parameters.
     *
     * @param handler the queue handler
     *
     * @see #DEFAULT_BUFFER_SIZE
     * @see #DEFAULT_BUFFER_TIMEOUT
     */
    public QueueDispatcher(IQueueHandler<T> handler) {
        this(handler, DEFAULT_BUFFER_SIZE, DEFAULT_BUFFER_TIMEOUT);
    }

    /**
     * Creates a new instance of the class with the given buffer size and timeout.
     *
     * @param handler the queue handler
     * @param bufferSize defines that the queue is processed at least when the queue grows larger than this value
     * @param bufferTimeout defines that the queue is processed at least when items in the queue stay longer than this value
     *
     * @see #DEFAULT_BUFFER_SIZE
     * @see #DEFAULT_BUFFER_TIMEOUT
     */
    public QueueDispatcher(IQueueHandler<T> handler, int bufferSize, Duration bufferTimeout) {
        this.handler = handler;
        this.bufferSize = bufferSize;
        this.bufferTimeout = bufferTimeout.toMillis();
        this.buffer = new LinkedBlockingQueue<>(4 * bufferSize);
        this.notifyObject = new Object();
        this.disposed = new AtomicBoolean(false);
        this.handlingExecutor = Asynchronizer.executorFactory().newSingleThreadExecutor(getClass(), false);
        this.handlingTask = handlingExecutor.submit(this::queueHandlingThread);
        this.observersNotifiedAboutFailure = new AtomicBoolean(false);
        this.successObservers = new CopyOnWriteArrayList<>();
        this.failureObservers = new CopyOnWriteArrayList<>();
    }


    @Override
    public void enqueue(T item) {
        if (!disposed.get()) {
            try {
                buffer.put(item);

                // Notify the consumer thread when the queue is full
                if (buffer.size() >= bufferSize) {
                    synchronized (notifyObject) {
                        notifyObject.notify();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void enqueue(Collection<T> item) {
        if (!disposed.get()) {
            try {
                for (var point : item) {
                    buffer.put(point);
                }

                // Notify the consumer thread when the queue is full
                if (buffer.size() >= bufferSize) {
                    synchronized (notifyObject) {
                        notifyObject.notify();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    @Override
    public IDisposable subscribeToSuccess(Runnable observer) {
        successObservers.add(observer);
        return () -> successObservers.remove(observer);
    }

    private void notifySuccess() {
        if (observersNotifiedAboutFailure.getAndSet(false)) {
            for (var observer : successObservers) {
                commonPool().execute(observer);
            }
        }
    }


    @Override
    public IDisposable subscribeToFailure(Consumer<Throwable> observer) {
        failureObservers.add(observer);
        return () -> failureObservers.remove(observer);
    }

    private void notifyFailure(Throwable failure) {
        for (var observer : failureObservers) {
            commonPool().execute(() -> observer.accept(failure));
        }

        observersNotifiedAboutFailure.set(true);
    }


    @Override
    public void dispose() {
        if (!disposed.getAndSet(true)) {

            successObservers.clear();
            failureObservers.clear();

            // Notify the consumer thread to complete
            synchronized (notifyObject) {
                notifyObject.notify();
            }

            // Wait for the consumer thread is completed
            awaitWritingCompleted();

            handlingExecutor.shutdown();
        }
    }


    protected void awaitWritingCompleted() {
        if (!handlingTask.isDone()) {
            try {
                handlingTask.get();
            } catch (Exception ignore) {
                // Ignore
            }
        }
    }


    private void queueHandlingThread() {
        while (true) {

            // Wait until the queue is not full
            if (!disposed.get() && buffer.size() < bufferSize) {
                synchronized (notifyObject) {
                    try {
                        notifyObject.wait(bufferTimeout);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }

            // Do nothing for empty queue
            if (buffer.isEmpty()) {
                if (disposed.get()) {
                    break;
                }
                continue;
            }

            // Take a batch of items from the queue
            var items = new ArrayList<T>(Math.max(bufferSize, buffer.size()));
            buffer.drainTo(items);

            // Handle the batch
            try {
                handler.handle(items);
                notifySuccess();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable e) {
                notifyFailure(e);
                log.atError()
                        .setMessage("Cannot process a batch of items. These items has been lost.")
                        .setCause(e)
                        .log();
            }
        }
    }
}
