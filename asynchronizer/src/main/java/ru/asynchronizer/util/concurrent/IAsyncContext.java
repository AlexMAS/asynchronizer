package ru.asynchronizer.util.concurrent;

import ru.asynchronizer.util.IDisposable;

/**
 * Represents data related to an asynchronous control flow.
 *
 * <p>
 * Because the task-based asynchronous programming model tends to abstract the use of
 * threads, {@link IAsyncContext} can be used to persist data across threads. It also
 * provides optional {@linkplain #subscribeToChange(IAsyncContextObserver) notifications}
 * when the value associated with the current thread changes, either because it was
 * explicitly changed by {@linkplain #setProperty(String, Object) setting a property value},
 * or implicitly changed e.g. before or after an asynchronous task.
 */
public interface IAsyncContext {

    /**
     * Captures the current context to be able to use it for another thread.
     */
    IAsyncContextCapture capture();

    /**
     * Gets the property value.
     */
    Object getProperty(String name);

    /**
     * Sets the property value.
     */
    void setProperty(String name, Object value);

    /**
     * Subscribes to the context changes.
     */
    IDisposable subscribeToChange(IAsyncContextObserver observer);
}
