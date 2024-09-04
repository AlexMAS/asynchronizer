# RepeatedCompletableFuture

## Circumstance

* There is an [idempotent](https://en.wikipedia.org/wiki/Idempotence) function that fails until suitable conditions are provided.
* The function must be executed until success.
* The function must be executed asynchronously.

## Using

If it is possible to start dependent microservices concurrently, it can simplify their deployment. But for that
we need to make that microservices a little tolerant to short-term unavailability of some resources. Usually,
before interacting with a resource, we have to establish a connection with it. Since during the connection,
the resource may be unavailable for a while, we perform several connection attempts before reporting the problem. 

Imagine your resource has the following client API.

```java
interface SomeResourceClient {
    SomeResourceConnection connect(String uri) throws IOException;
}

interface SomeResourceConnection {
    String loadData();
}
```

Using the [`RepeatedCompletableFuture`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/RepeatedCompletableFuture.java) class
you can create an asynchronous task that tries to connect to this resource until success.

```java
SomeResourceClient resourceClient;
...

RepeatedCompletableFuture<SomeResourceConnection> resourceConnection = RepeatedCompletableFuture.startAttempts(
        () -> resourceClient.connect("my/resource"),
        error -> log.atWarn().setCause(error).log("The connection attempt failed."),
        Duration.ofSeconds(5)
);
```

In the last example, the `connect()` method will be invoked *infinitely* until it completes without exception.
If an attempt fails, this error is logged and a new connection attempt will be performed in 5 seconds.

If infinite attempts are inappropriate, you can interrupt this process by invoking the `cancel()` method.
In the following example, the attempts are interrupted after 10 failures.

```java
SomeResourceClient resourceClient;
AtomicInteger connectionAttempts;
RepeatedCompletableFuture<SomeResourceConnection> resourceConnection;
...

void connect() {
    resourceConnection = RepeatedCompletableFuture.startAttempts(
            () -> resourceClient.connect("my/resource"),
            error -> {
                log.atWarn().setCause(error).log("The connection attempt failed.");

                if (connectionAttempts.incrementAndGet() >= 10) {
                    resourceConnection.cancel(true);
                }
            },
            Duration.ofSeconds(5)
    );
}
```

To access the connection you can await success or use the current connection state by invoking the `lastAttempt()` method.
In the first case you will not be able to work with the resource until either the connection is not established or this
connection process is interrupted. In the second case you may get an error if the last attempt was unsuccessful.

```java
// Await the successful connection
resourceConnection
        .thenAccept(connection -> {
            var data = connection.loadData();
            System.out.println(data);
        });

// Or use the current connection state
resourceConnection.lastAttempt()
        .thenAccept(connection -> {
            var data = connection.loadData();
            System.out.println(data);
        });
```
