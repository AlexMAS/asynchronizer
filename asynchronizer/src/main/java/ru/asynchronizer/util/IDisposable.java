package ru.asynchronizer.util;

/**
 * Defines the {@link #dispose()} method must be invoked on the service scope destroying.
 */
public interface IDisposable extends AutoCloseable {

    void dispose();


    @Override
    default void close() {
        dispose();
    }


    default IDisposable andThen(IDisposable after) {
        return () -> {
            dispose();
            after.dispose();
        };
    }


    static IDisposable combine(Iterable<? extends IDisposable> items) {
        return () -> dispose(items);
    }

    static IDisposable combine(IDisposable... items) {
        return () -> dispose(items);
    }


    static void dispose(Iterable<? extends IDisposable> items) {
        if (items != null) {
            for (var i : items) {
                if (i != null) {
                    i.dispose();
                }
            }
        }
    }

    static void dispose(IDisposable... items) {
        if (items != null) {
            for (var i : items) {
                if (i != null) {
                    i.dispose();
                }
            }
        }
    }
}
