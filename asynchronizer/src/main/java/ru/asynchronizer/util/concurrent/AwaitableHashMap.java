package ru.asynchronizer.util.concurrent;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import ru.asynchronizer.util.ObjectRef;

/**
 * Maps keys to values and provides an ability to await adding and removing keys.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public final class AwaitableHashMap<K, V> {

    private final Map<K, V> delegatedMap;
    private final Map<K, CompletableFuture<V>> awaitersOnAdded;
    private final Map<K, CompletableFuture<Boolean>> awaitersOnRemoved;


    public AwaitableHashMap() {
        this(new ConcurrentHashMap<>());
    }

    public AwaitableHashMap(Map<K, V> delegatedMap) {
        this.delegatedMap = delegatedMap;
        this.awaitersOnAdded = new ConcurrentHashMap<>();
        this.awaitersOnRemoved = new ConcurrentHashMap<>();
    }


    /**
     * Returns a {@link Set} view of the keys contained in this map.
     */
    public Set<K> keySet() {
        return delegatedMap.keySet();
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     */
    public Collection<V> values() {
        return delegatedMap.values();
    }


    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     */
    public V get(K key) {
        return delegatedMap.get(key);
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old value
     * is replaced by the specified value.
     */
    public V put(K key, V value) {
        var previousValue = delegatedMap.put(key, value);

        // If the key is just added, not replaced
        if (previousValue == null) {
            var keyAwaiter = awaitersOnAdded.remove(key);

            if (keyAwaiter != null) {
                keyAwaiter.complete(value);
            }
        }

        return previousValue;
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     */
    public V remove(K key) {
        var previous = delegatedMap.remove(key);

        if (previous != null) {
            var awaiter = awaitersOnRemoved.remove(key);

            if (awaiter != null) {
                awaiter.complete(true);
            }
        }

        return previous;
    }


    /**
     * Returns a {@link CompletableFuture} instance to await a value of the specified key.
     * The returned future will be completed as soon as the specified key is added.
     * The future is completed with an added value. In case the key already exists,
     * the method returns a completed future with a value of the specified key.
     */
    public CompletableFuture<V> await(K key) {
        return await(key, 0, null);
    }

    /**
     * Returns a {@link CompletableFuture} instance to await a value of the specified key.
     * The returned future will be completed as soon as the specified key is added or
     * the given timeout is expired. The future is completed normally with an added value
     * if the specified key has been added before the given timeout; otherwise
     * exceptionally with a {@link TimeoutException}. In case the key already exists,
     * the method returns a completed future with a value of the specified key.
     */
    public CompletableFuture<V> await(K key, long timeout, TimeUnit unit) {
        return awaitInternal(key, timeout, unit, null, false);
    }

    /**
     * Returns a {@link CompletableFuture} instance to await a value of the specified key.
     * The returned future will be completed as soon as the specified key is added or
     * the given timeout is expired. The future is completed normally with an added value
     * if the specified key has been added before the given timeout; otherwise
     * with the given default value. In case the key already exists,
     * the method returns a completed future with a value of the specified key.
     */
    public CompletableFuture<V> awaitOrDefault(K key, V defaultValue, long timeout, TimeUnit unit) {
        return awaitInternal(key, timeout, unit, defaultValue, true);
    }

    private CompletableFuture<V> awaitInternal(K key, long timeout, TimeUnit unit, V defaultValue, boolean withDefaultValue) {
        var value = delegatedMap.get(key);

        // If the key already exists, return its value immediately
        if (value != null) {
            return CompletableFuture.completedFuture(value);
        }

        var keyAwaiterRef = new ObjectRef<CompletableFuture<V>>();

        awaitersOnAdded.compute(key, (k, awaiter) -> {
            var value2 = delegatedMap.get(k);

            // If the key already exists, return its value and remove the key awaiter
            if (value2 != null) {
                keyAwaiterRef.set(CompletableFuture.completedFuture(value2));
                return null;
            }

            // If the key awaiter already exists, return it
            if (awaiter != null) {
                keyAwaiterRef.set(awaiter);
                return awaiter;
            }

            // Create a new key awaiter which is removed automatically as soon as completed
            CompletableFuture<V> keyAwaiter = new FinalizableCompletableFuture<>(() -> awaitersOnAdded.remove(k));

            if (timeout > 0 && unit != null) {
                if (withDefaultValue) {
                    keyAwaiter = keyAwaiter.completeOnTimeout(defaultValue, timeout, unit);
                } else {
                    keyAwaiter = keyAwaiter.orTimeout(timeout, unit);
                }
            }

            keyAwaiterRef.set(keyAwaiter);

            return keyAwaiter;
        });

        return keyAwaiterRef.get();
    }


    /**
     * Returns a {@link CompletableFuture} instance to await the specified key is removed.
     * The returned future will be completed as soon as the specified key is removed.
     * In case the key does not exist, the method returns a completed future.
     */
    public CompletableFuture<Boolean> awaitRemove(K key) {
        return awaitRemove(key, 0, null);
    }

    /**
     * Returns a {@link CompletableFuture} instance to await the specified key is removed.
     * The returned future will be completed as soon as the specified key is removed or
     * the given timeout is expired. The future is completed normally if the specified key
     * has been removed before the given timeout; otherwise exceptionally with a {@link TimeoutException}.
     * In case the key does not exist, the method returns a completed future.
     */
    public CompletableFuture<Boolean> awaitRemove(K key, long timeout, TimeUnit unit) {
        // If the key does not exist, return a completed future immediately
        if (!delegatedMap.containsKey(key)) {
            return CompletableFutureUtil.completedFalse();
        }

        var keyAwaiterRef = new ObjectRef<CompletableFuture<Boolean>>();

        awaitersOnRemoved.compute(key, (k, awaiter) -> {
            // If the key does not exist, return a completed future and remove the key awaiter
            if (!delegatedMap.containsKey(k)) {
                keyAwaiterRef.set(CompletableFutureUtil.completedFalse());
                return null;
            }

            // If the key awaiter already exists, return it
            if (awaiter != null) {
                keyAwaiterRef.set(awaiter);
                return awaiter;
            }

            // Create a new key awaiter which is removed automatically as soon as completed
            CompletableFuture<Boolean> keyAwaiter = new FinalizableCompletableFuture<>(() -> awaitersOnRemoved.remove(k));

            if (timeout > 0 && unit != null) {
                keyAwaiter = keyAwaiter.orTimeout(timeout, unit);
            }

            keyAwaiterRef.set(keyAwaiter);

            return keyAwaiter;
        });

        return keyAwaiterRef.get();
    }


    /**
     * Clears this map and completes all key removal awaiters.
     */
    public void clear() {
        delegatedMap.clear();
        awaitersOnRemoved.values().forEach(awaiter -> awaiter.complete(true));
        awaitersOnRemoved.clear();
    }

}
