# IPriorityExecutorService

## Circumstance

* Tasks must execute asynchronously.
* Tasks must execute based on their priority rather than  the order they income.

## Using

The [`IPriorityExecutorService`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IPriorityExecutorService.java) interface
extends the `java.util.concurrent.ExecutorService` one and provides the ability to execute tasks asynchronously based on their priority
rather than the order they income. If two tasks have the same priority they will be executed in the order they income.

The usage of the `IPriorityExecutorService` interface is the same as the `java.util.concurrent.ExecutorService` one.
To enqueue a task to execute use usual methods like `execute()`, `submit()` etc.
The only difference is in the task prioritization which is described below.

### The executor instance

The easiest way to get an `IPriorityExecutorService` instance is to use the
[`IExecutorFactory.newPrioritySingleThreadExecutor()`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IExecutorFactory.java)
or [`IExecutorFactory.newPriorityFixedThreadPool()`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IExecutorFactory.java) factory methods.

```java
IPriorityExecutorService priorityExecutor = Asynchronizer.executorFactory()
        .newPrioritySingleThreadExecutor(getClass());
```

The executor created this way will honor the [Async Context](async-context.md).

More direct way to get an `IPriorityExecutorService` instance is to use the [`PriorityExecutorService`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/PriorityExecutorService.java) class.

```java
int poolSize;
ThreadFactory threadFactory;
...
IPriorityExecutorService priorityExecutor = new PriorityExecutorService(poolSize, threadFactory);
```

### Task prioritization

A task priority can be defined either explicitly or implicitly.

#### Explicit prioritization

A task must implement the [`IPriorityTask`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IPriorityTask.java) interface
and explicitly define the `priority()` method. This method returns an integer which defines the task priority:
the bigger value the higher priority.

For convenience, there are two extensions of the `IPriorityTask` interface:

* [`IPriorityRunnable`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IPriorityRunnable.java)
* [`IPriorityCallable`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IPriorityCallable.java)

The `IPriorityRunnable` interface is used for tasks which do not return result, the `IPriorityCallable` one for those which do.

A task implements the `IPriorityTask` interface explicitly if it must encapsulate the task prioritization logic.

```java
class MyRunnableTask implements IPriorityRunnable {

    @Override
    public int priority() {
        // Calculate the priority
    }

    @Override
    public void run() {
        // Do the task
    }
}

class MyCallableTask implements IPriorityCallable<String> {

    @Override
    public int priority() {
        // Calculate the priority
    }

    @Override
    public String call() {
        // Do the task
    }
}
```

If a task priority is set externally and you would like to declare task types explicitly, a task definition can be simplified by using `record`.

```java
record MyRunnableTask(int priority) implements IPriorityRunnable {

    @Override
    public void run() {
        // Do the task
        System.out.println("Hello!");
    }
}

record MyCallableTask(int priority) implements IPriorityCallable<String> {

    @Override
    public String call() {
        // Do the task
        return "Hello!";
    }
}
```

But the easiest way to define a task priority is to use one of the `with()` methods, passing the priority and the task implementation.

```java
var myRunnableTask = IPriorityRunnable.withPriority(priority, () -> {
    // Do the task
    System.out.println("Hello!");
});

var myCallableTask = IPriorityCallable.withPriority(priority, () -> {
    // Do the task
    return "Hello!";
});
```

Once a task has been created, it can be submitted for execution as usual.

```java
IPriorityExecutorService priorityExecutor;
...
priorityExecutor.execute(myRunnableTask);
priorityExecutor.submit(myCallableTask);
```

#### Implicit prioritization

If tasks income from an external source and have the same priority, you can define an executor with that priority, and delegate
all the tasks to it. In this case you can use one of the `IPriorityExecutorService.with()` methods.

```java
IPriorityExecutorService priorityExecutor;
...
ExecutorService priority42Executor = priorityExecutor.withPriority(42);
ExecutorService lowPriorityExecutor = priorityExecutor.withLowPriority();
ExecutorService highPriorityExecutor = priorityExecutor.withHighPriority();
...
lowPriorityExecutor.execute(() -> System.out.println("Low priority task"));
highPriorityExecutor.execute(() -> System.out.println("High priority task"));
```

The convenience and main advantage is in that a task source is not responsible for its prioritization.
