package ru.asynchronizer.util.concurrent;

import java.util.Map;

/**
 * Observes changes of the {@link IAsyncContext}.
 */
public interface IAsyncContextObserver {

    /**
     * Is called when the context property has been changed.
     */
    void contextChange(String property, Object value);

    /**
     * Is called when the entire context has been changed.
     */
    void contextSwitch(Map<String, Object> context);
}
