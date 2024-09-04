# Async Context and MDC

## Circumstance

* You are using the [SLF4J MDC](https://www.slf4j.org/manual.html#mdc).
* You are using the [Async Context](async-context.md).
* Some properties of the context must be included in the log entries.

## Using

In your [IoC](https://en.wikipedia.org/wiki/Inversion_of_control) container register
the [`AsyncContextMdcAdapter`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/AsyncContextMdcAdapter.java) class
as a singleton dependency of the application.

On the application startup invoke the `run()` method:

```java
AsyncContextMdcAdapter mdcAdapter;
...
mdcAdapter.run();
```

For example, the [context](async-context.md) contains the `userId` property and you are using [`log4j2`](https://logging.apache.org/log4j).
To add this property to the log entries, mention it in the [pattern layout](https://logging.apache.org/log4j/2.x/manual/pattern-layout.html)
as `%X{userId}`.

### Spring Boot

If you are using Spring Boot, add the next registration:

```java
@Bean
public AsyncContextMdcAdapter mdcAdapter(IAsyncContext context) {
    return new AsyncContextMdcAdapter(context);
}
```

And a handler of the application startup event:

```java
@Component
public class AsyncContextMdcAdapterRunner {

    private final AsyncContextMdcAdapter mdcAdapter;

    ...

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStarted() {
        mdcAdapter.run();
    }
}
```
