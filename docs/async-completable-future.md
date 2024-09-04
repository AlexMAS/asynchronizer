# AsyncCompletableFuture

## Circumstance

* You are using the [Async Context](async-context.md).
* There are asynchronous continuations based on a `CompletableFuture` instance.

## Using

The [`AsyncCompletableFuture`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/AsyncCompletableFuture.java) class
repeats `java.util.concurrent.CompletableFuture` functionality but allows to specify the default executor.
If the default executor is not defined the [`Asynchronizer.commonPool()`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/Asynchronizer.java)
is used.

The `AsyncCompletableFuture` class allows to set executors that honor the [async context](async-context.md) in continuations
for which an executor is not defined explicitly i.e. like `CompletableFuture.thenAcceptAsync(Consumer)`.
In such cases `CompletableFuture` uses the `ForkJoinPool.commonPool()`, and it can provoke unexpected behaviour
for the side that provides the future instance. At least, the [async context](async-context.md) will not be available for
continuations, because they are created with the executor which does not copy the context.

```java
IAsyncContext context = Asynchronizer.context();
context.setProperty("userId", 123);
...

AsyncCompletableFuture.completedFuture("Hello, ")
        // This continuation will have the same context
        .thenAcceptAsync(result -> {
            var userId = context.getProperty("userId");
            System.out.println(result + userId); // Output: "Hello, 123"
        })
        .get();
```
