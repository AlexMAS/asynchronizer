# AwaitableQueue

## Circumstance

* There are a lot of asynchronous activities that must be awaited (to continue processing).
* We do not want to await each asynchronous activity in a separate thread.

## Concept

Sometimes we have to use a third-party library with asynchronous methods returning `java.util.concurrent.Future` instances.
Such an API is inconvenient because the `Future` does not give an elegant way to await the result. We have to either retrieve
the result synchronously by invoking the `Future.get()` method, or check periodically the result availability by invoking
the `Future.get(long, TimeUnit)` method. The first approach eliminates all the advantages of asynchronicity, and both can
leave you with lots of threads in the `WAITING`/`TIMED_WAITING` state. If you use a thread pool, it will eventually be exhausted.
All depends on how many such asynchronous tasks are awaited in parallel. Nonetheless, lots of awaiting threads means
suboptimal use of resources (CPU/RAM).

The [`AwaitableQueue`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/AwaitableQueue.java) class provides an efficient
approach to await completion of asynchronous tasks that do not provide a convenient way to await such as the `CompletableFuture` class.
Each asynchronous activity must implement the [`IAwaitable`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IAwaitable.java) interface
and be enqueued with either the `enqueue(IAwaitable)` or `enqueue(IAwaitable, Duration)` methods. These methods return
an instance of the `CompletableFuture` class which can be used for asynchronous awaiting without any downsides.
The provided future can be safely interrupted as well, for example, with the `CompletableFuture.cancel(boolean)` method.
As soon as a task is completed it is dequeued automatically.

The implementation is based on a single-threaded executor that, in an infinite loop, sequentially polls enqueued tasks,
giving each a small piece of time to synchronously wait for completion. If a task completes in time, the appropriate future
is completed as well and the task is dequeued. Awaiting all tasks in a single thread allows to utilize CPU/RAM effectively -
only one thread is in active wait. On the other hand all tasks are polled sequentially thus in case of long queue fast
tasks can await completion longer. This negative effect can be minimized by decreasing the task poll timeout or using
a separate queue.

## Using

In the following example an asynchronous `download()` method returns a resource content by its URI. The provided instance
of the `Future<String>` is converted to a `CompletableFuture<String>` instance that can be awaited efficiently,
for example by using the [`AsyncPipeline`](async-pipeline.md).

```java
AwaitableQueue awaitableQueue = new AwaitableQueue(Asynchronizer.commonPool());
...
Future<String> contentFuture = download("https://...");
IAwaitable<String> contentAwaitable = IAwaitable.of(contentFuture);
CompletableFuture<String> contentCompletableFuture = awaitableQueue.enqueue(contentAwaitable);
```

In this way the `AwaitableQueue` can be used to convert an obsolete, inconvenient, and awkward API to a modern asynchronous one.

### IAwaitable

The `IAwaitable` interface already has the factory method based on a `Future` instance. Other types of awaitable objects
are also easy to convert to the `IAwaitable`. For example, you have a `CountDownLatch` instance and can access some result
as soon as the latch be opened.

```java
// A latch
CountDownLatch resultLatch;
// An accessor to the latched result
Supplier<String> resultSupplier;
```

Add the `LatchedResult` class that implements the `IAwaitable` interface.

```java
class LatchedResult<T> implements IAwaitable<T> {

    private final CountDownLatch resultLatch;
    private final Supplier<T> resultSupplier;

    public LatchedResult(CountDownLatch resultLatch, Supplier<T> resultSupplier) {
        this.resultLatch = resultLatch;
        this.resultSupplier = resultSupplier;
    }

    @Override
    public IAwaiter<T> getAwaiter() {
        return new IAwaiter<>() {

            @Override
            public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
                return resultLatch.await(timeout, unit);
            }

            @Override
            public T getResult() {
                return resultSupplier.get();
            }
        };
    }
}
```

After that you will be able to create an `IAwaitable` instance.

```java
IAwaitable<String> latchedAwaitable = new LatchedResult<>(resultLatch, resultSupplier);
CompletableFuture<String> resultCompletableFuture = awaitableQueue.enqueue(latchedAwaitable);
```
