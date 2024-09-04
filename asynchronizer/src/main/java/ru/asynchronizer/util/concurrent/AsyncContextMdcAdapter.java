package ru.asynchronizer.util.concurrent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.MDC;

import ru.asynchronizer.util.IDisposable;

public final class AsyncContextMdcAdapter implements IDisposable {

    private final IAsyncContext context;

    private IDisposable contextSubscription;


    public AsyncContextMdcAdapter(IAsyncContext context) {
        this.context = context;
    }


    public void run() {
        contextSubscription = context.subscribeToChange(new MdcAsyncContextObserver());
    }

    @Override
    public void dispose() {
        if (contextSubscription != null) {
            try {
                contextSubscription.close();
            } finally {
                contextSubscription = null;
            }
        }
    }


    private static class MdcAsyncContextObserver implements IAsyncContextObserver {

        @Override
        public void contextChange(String name, Object value) {
            MDC.put(name, Objects.toString(value, ""));
        }

        @Override
        public void contextSwitch(Map<String, Object> context) {
            var contextMap = new HashMap<String, String>(context.size());
            context.forEach((p, v) -> contextMap.put(p, Objects.toString(v, "")));
            MDC.setContextMap(contextMap);
        }
    }
}
