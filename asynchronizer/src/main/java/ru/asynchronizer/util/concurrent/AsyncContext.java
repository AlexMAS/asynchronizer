package ru.asynchronizer.util.concurrent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import ru.asynchronizer.util.IDisposable;

final class AsyncContext implements IAsyncContext, IDisposable {

    private final ThreadLocal<AsyncContextData> context;
    private final Collection<IAsyncContextObserver> observers;


    public AsyncContext() {
        this.context = new ThreadLocal<>();
        this.observers = new CopyOnWriteArrayList<>();
    }


    @Override
    public IAsyncContextCapture capture() {
        return new AsyncContextCapture();
    }

    @Override
    public Object getProperty(String name) {
        return getData().getProperty(name);
    }

    @Override
    public void setProperty(String name, Object value) {
        if (getData().setProperty(name, value)) {
            notifyContextChange(name, value);
        }
    }

    @Override
    public IDisposable subscribeToChange(IAsyncContextObserver observer) {
        observers.add(observer);
        return () -> observers.remove(observer);
    }

    @Override
    public void dispose() {
        observers.clear();
    }


    private AsyncContextData getData() {
        var data = context.get();

        if (data == null) {
            data = new AsyncContextData();
            context.set(data);
        }

        return data;
    }

    private void setData(AsyncContextData data) {
        if (data == null) {
            data = new AsyncContextData();
        }

        context.set(data);

        notifyContextSwitch(data.getProperties());
    }


    private void notifyContextChange(String property, Object value) {
        if (!observers.isEmpty()) {
            observers.forEach(o -> o.contextChange(property, value));
        }
    }

    private void notifyContextSwitch(Map<String, Object> context) {
        if (!observers.isEmpty()) {
            observers.forEach(o -> o.contextSwitch(context));
        }
    }


    private class AsyncContextCapture implements IAsyncContextCapture {

        private final AsyncContextData data;


        AsyncContextCapture() {
            this.data = new AsyncContextData(getData());
        }


        @Override
        public void copyTo(Map<String, Object> destination) {
            destination.putAll(data.getProperties());
        }

        @Override
        public IDisposable use() {
            var currentData = getData();
            setData(data);
            return () -> setData(currentData);
        }
    }


    private static class AsyncContextData {

        private final Map<String, Object> properties;


        AsyncContextData() {
            this.properties = new HashMap<>();
        }

        AsyncContextData(AsyncContextData other) {
            this.properties = new HashMap<>(other.properties);
        }


        public Map<String, Object> getProperties() {
            return properties;
        }

        public Object getProperty(String name) {
            return properties.get(name);
        }

        public boolean setProperty(String name, Object value) {
            return !Objects.equals(value, properties.put(name, value));
        }
    }
}
