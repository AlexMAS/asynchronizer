package ru.asynchronizer.util.concurrent;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AwaitableHashMapTest {

    // PUT


    @Test
    public void shouldPutValue() {

        // Given
        var map = new AwaitableHashMap<Integer, String>();

        // When
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");

        // Then
        assertThat(map.get(1)).isEqualTo("A");
        assertThat(map.get(2)).isEqualTo("B");
        assertThat(map.get(3)).isEqualTo("C");
        assertThat(map.get(4)).isNull();
        assertThat(map.keySet()).containsExactlyInAnyOrder(1, 2, 3);
        assertThat(map.values()).containsExactlyInAnyOrder("A", "B", "C");
    }

    @Test
    public void shouldAwaitValueInfinitely() {

        // Given
        var map = new AwaitableHashMap<Integer, String>();

        // When
        var futureValue1 = map.await(1);
        var futureValue2 = map.await(2);
        var futureValue3 = map.await(3);

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(false);
        assertThat(futureValue2.isDone()).isEqualTo(false);
        assertThat(futureValue3.isDone()).isEqualTo(false);

        // When
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(true);
        assertThat(futureValue2.isDone()).isEqualTo(true);
        assertThat(futureValue3.isDone()).isEqualTo(true);
        assertThat(futureValue1.getNow(null)).isEqualTo("A");
        assertThat(futureValue2.getNow(null)).isEqualTo("B");
        assertThat(futureValue3.getNow(null)).isEqualTo("C");
    }

    @Test
    public void shouldAwaitValueWithTimeout() {

        // Given
        var map = new AwaitableHashMap<Integer, String>();

        // When
        var futureValue1 = map.await(1, 5, TimeUnit.SECONDS);
        var futureValue2 = map.await(2, 5, TimeUnit.SECONDS);
        var futureValue3 = map.await(3, 5, TimeUnit.SECONDS);

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(false);
        assertThat(futureValue2.isDone()).isEqualTo(false);
        assertThat(futureValue3.isDone()).isEqualTo(false);

        // When
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(true);
        assertThat(futureValue2.isDone()).isEqualTo(true);
        assertThat(futureValue3.isDone()).isEqualTo(true);
        assertThat(futureValue1.getNow(null)).isEqualTo("A");
        assertThat(futureValue2.getNow(null)).isEqualTo("B");
        assertThat(futureValue3.getNow(null)).isEqualTo("C");
    }

    @Test
    public void shouldAwaitValueOrGetDefaultByTimeout() throws Exception {

        // Given
        var map = new AwaitableHashMap<Integer, String>();

        // When
        var futureValue1 = map.awaitOrDefault(1, "X", 1, TimeUnit.SECONDS);
        var futureValue2 = map.awaitOrDefault(2, "Y", 1, TimeUnit.SECONDS);
        var futureValue3 = map.awaitOrDefault(3, "Z", 1, TimeUnit.SECONDS);

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(false);
        assertThat(futureValue2.isDone()).isEqualTo(false);
        assertThat(futureValue3.isDone()).isEqualTo(false);

        // When
        map.put(1, "A");

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(true);
        assertThat(futureValue1.getNow(null)).isEqualTo("A");
        assertThat(futureValue2.isDone()).isEqualTo(false);
        assertThat(futureValue3.isDone()).isEqualTo(false);

        // When
        futureValue2.get(5, TimeUnit.SECONDS);
        futureValue3.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(futureValue2.getNow(null)).isEqualTo("Y");
        assertThat(futureValue3.getNow(null)).isEqualTo("Z");
    }

    @Test
    public void shouldNotAwaitExistingValue() {

        // Given
        var map = new AwaitableHashMap<Integer, String>();
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");

        // When
        var futureValue1 = map.await(1, 5, TimeUnit.SECONDS);
        var futureValue2 = map.await(2, 5, TimeUnit.SECONDS);
        var futureValue3 = map.await(3, 5, TimeUnit.SECONDS);

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(true);
        assertThat(futureValue2.isDone()).isEqualTo(true);
        assertThat(futureValue3.isDone()).isEqualTo(true);
        assertThat(futureValue1.getNow(null)).isEqualTo("A");
        assertThat(futureValue2.getNow(null)).isEqualTo("B");
        assertThat(futureValue3.getNow(null)).isEqualTo("C");
    }

    @Test
    public void shouldThrowExceptionByTimeout() {

        // Given
        var map = new AwaitableHashMap<Integer, String>();

        // When

        var neverCompleted = map.await(1, 1, TimeUnit.MILLISECONDS);

        Throwable actualError = null;

        try {
            neverCompleted.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            actualError = e.getCause();
        }


        // Then
        assertThat(neverCompleted.isDone()).isEqualTo(true);
        assertThat(actualError).isInstanceOf(TimeoutException.class);
    }


    // REMOVE


    @Test
    public void shouldRemoveValue() {

        // Given
        var map = new AwaitableHashMap<Integer, String>();
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");

        // When
        map.remove(2);

        // Then
        assertThat(map.get(1)).isEqualTo("A");
        assertThat(map.get(3)).isEqualTo("C");
        assertThat(map.get(4)).isNull();
        assertThat(map.keySet()).containsExactlyInAnyOrder(1, 3);
        assertThat(map.values()).containsExactlyInAnyOrder("A", "C");
    }

    @Test
    public void shouldAwaitRemoveValueInfinitely() {

        // Given
        var map = new AwaitableHashMap<Integer, String>();
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");

        // When
        var futureValue1 = map.awaitRemove(1);
        var futureValue2 = map.awaitRemove(2);
        var futureValue3 = map.awaitRemove(3);

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(false);
        assertThat(futureValue2.isDone()).isEqualTo(false);
        assertThat(futureValue3.isDone()).isEqualTo(false);

        // When
        map.remove(1);
        map.remove(2);
        map.remove(3);

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(true);
        assertThat(futureValue2.isDone()).isEqualTo(true);
        assertThat(futureValue3.isDone()).isEqualTo(true);
        assertThat(futureValue1.getNow(null)).isEqualTo(true);
        assertThat(futureValue2.getNow(null)).isEqualTo(true);
        assertThat(futureValue3.getNow(null)).isEqualTo(true);
    }

    @Test
    public void shouldAwaitRemoveValueWithTimeout() {

        // Given
        var map = new AwaitableHashMap<Integer, String>();
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");

        // When
        var futureValue1 = map.awaitRemove(1, 5, TimeUnit.SECONDS);
        var futureValue2 = map.awaitRemove(2, 5, TimeUnit.SECONDS);
        var futureValue3 = map.awaitRemove(3, 5, TimeUnit.SECONDS);

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(false);
        assertThat(futureValue2.isDone()).isEqualTo(false);
        assertThat(futureValue3.isDone()).isEqualTo(false);

        // When
        map.remove(1);
        map.remove(2);
        map.remove(3);

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(true);
        assertThat(futureValue2.isDone()).isEqualTo(true);
        assertThat(futureValue3.isDone()).isEqualTo(true);
        assertThat(futureValue1.getNow(null)).isEqualTo(true);
        assertThat(futureValue2.getNow(null)).isEqualTo(true);
        assertThat(futureValue3.getNow(null)).isEqualTo(true);
    }

    @Test
    public void shouldNotAwaitRemovedValue() {

        // Given
        var map = new AwaitableHashMap<Integer, String>();

        // When
        var futureValue1 = map.awaitRemove(1, 5, TimeUnit.SECONDS);
        var futureValue2 = map.awaitRemove(2, 5, TimeUnit.SECONDS);
        var futureValue3 = map.awaitRemove(3, 5, TimeUnit.SECONDS);

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(true);
        assertThat(futureValue2.isDone()).isEqualTo(true);
        assertThat(futureValue3.isDone()).isEqualTo(true);
        assertThat(futureValue1.getNow(null)).isEqualTo(false);
        assertThat(futureValue2.getNow(null)).isEqualTo(false);
        assertThat(futureValue3.getNow(null)).isEqualTo(false);
    }


    // CLEAR


    @Test
    public void shouldClearValues() {

        // Given
        var map = new AwaitableHashMap<Integer, String>();
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");

        // When
        map.clear();

        // Then
        assertThat(map.get(1)).isNull();
        assertThat(map.get(3)).isNull();
        assertThat(map.get(3)).isNull();
        assertThat(map.keySet()).isEmpty();
        assertThat(map.values()).isEmpty();
    }

    @Test
    public void shouldCompleteRemoveAwaiters() {

        // Given
        var map = new AwaitableHashMap<Integer, String>();
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");

        // When
        var futureValue1 = map.awaitRemove(1, 5, TimeUnit.SECONDS);
        var futureValue2 = map.awaitRemove(2, 5, TimeUnit.SECONDS);
        var futureValue3 = map.awaitRemove(3, 5, TimeUnit.SECONDS);

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(false);
        assertThat(futureValue2.isDone()).isEqualTo(false);
        assertThat(futureValue3.isDone()).isEqualTo(false);

        // When
        map.clear();

        // Then
        assertThat(futureValue1.isDone()).isEqualTo(true);
        assertThat(futureValue2.isDone()).isEqualTo(true);
        assertThat(futureValue3.isDone()).isEqualTo(true);
        assertThat(futureValue1.getNow(null)).isEqualTo(true);
        assertThat(futureValue2.getNow(null)).isEqualTo(true);
        assertThat(futureValue3.getNow(null)).isEqualTo(true);
    }
}
