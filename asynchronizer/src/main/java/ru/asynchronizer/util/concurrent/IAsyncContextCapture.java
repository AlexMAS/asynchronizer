package ru.asynchronizer.util.concurrent;

import java.util.Map;

import ru.asynchronizer.util.IDisposable;

/**
 * Represents the state of an asynchronous control flow.
 */
public interface IAsyncContextCapture {

    /**
     * Copies the captured data to the given destination.
     */
    void copyTo(Map<String, Object> destination);

    /**
     * Uses this capture to the current thread.
     *
     * @return the action to return the current thread to its previous state
     */
    IDisposable use();
}
