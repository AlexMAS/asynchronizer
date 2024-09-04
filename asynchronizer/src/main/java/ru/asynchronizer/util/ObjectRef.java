package ru.asynchronizer.util;

import java.util.Objects;
import java.util.function.Consumer;

public final class ObjectRef<T> {

    private T value;


    public ObjectRef() {
        this(null);
    }

    public ObjectRef(T value) {
        this.value = value;
    }


    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }


    public void ifPresent(Consumer<? super T> consumer) {
        if (get() != null) {
            consumer.accept(get());
        }
    }


    @Override
    public String toString() {
        return Objects.toString(value);
    }
}
