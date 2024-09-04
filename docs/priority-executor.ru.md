# IPriorityExecutorService

## Обстоятельство

* Задачи должны выполняться асинхронно.
* Задачи должны выполняться на основе их приоритета, а не порядка их поступления.

## Использование

Интерфейс [`IPriorityExecutorService`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IPriorityExecutorService.java)
расширяет `java.util.concurrent.ExecutorService` и предоставляет возможность асинхронной обработки задач на основе их
приоритета, а не порядка их поступления. Если задачи имеют одинаковый приоритет, они будут обработаны в порядке поступления.

Использование `IPriorityExecutorService` ничем не отличается от использования `java.util.concurrent.ExecutorService`.
Для постановки задач в очередь на исполнение используйте привычные методы `execute()`, `submit()` и т.д.
Особенность лишь в способе определения приоритета задач, о чем будет сказано ниже.

### Создание исполнителя

Самый простой способ создания `IPriorityExecutorService` - использовать фабричные методы
[`IExecutorFactory.newPrioritySingleThreadExecutor()`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IExecutorFactory.java)
или [`IExecutorFactory.newPriorityFixedThreadPool()`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IExecutorFactory.java).

```java
IPriorityExecutorService priorityExecutor = Asynchronizer.executorFactory()
        .newPrioritySingleThreadExecutor(getClass());
```

Созданный таким образом исполнитель будет учитывать [асинхронный контекст](async-context.ru.md).

Более прямолинейный способ создания `IPriorityExecutorService` - создание экземпляра класса [`PriorityExecutorService`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/PriorityExecutorService.java),
используя его конструктор.

```java
int poolSize;
ThreadFactory threadFactory;
...
IPriorityExecutorService priorityExecutor = new PriorityExecutorService(poolSize, threadFactory);
```

### Приоритизация задач

Приоритет задачи можно указать явно и неявно.

#### Явное указание приоритета

Задача должна реализовать интерфейс [`IPriorityTask`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IPriorityTask.java),
явно определив его метод `priority()`. Этот метод возвращает числовое значение приоритета: чем больше число,
тем больше приоритет у задачи.

Для удобства созданы два расширения интерфейса `IPriorityTask`:

* [`IPriorityRunnable`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IPriorityRunnable.java)
* [`IPriorityCallable`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IPriorityCallable.java)

Интерфейс `IPriorityRunnable` используется для тех задач, которые не возвращают результат своей работы,
а интерфейс `IPriorityCallable` для тех, которые возвращают.

Задача реализует интерфейс `IPriorityTask` явно, если она должна инкапсулировать логику вычисления приоритета.

```java
class MyRunnableTask implements IPriorityRunnable {

    @Override
    public int priority() {
        // Вычисление приоритета
    }

    @Override
    public void run() {
        // Реализация задачи
    }
}

class MyCallableTask implements IPriorityCallable<String> {

    @Override
    public int priority() {
        // Вычисление приоритета
    }

    @Override
    public String call() {
        // Реализация задачи
    }
}
```

Если приоритет задачи определяется извне и есть необходимость типизировать задачи, определение задач можно немного упростить, используя `record`.

```java
record MyRunnableTask(int priority) implements IPriorityRunnable {

    @Override
    public void run() {
        // Реализация задачи
        System.out.println("Привет!");
    }
}

record MyCallableTask(int priority) implements IPriorityCallable<String> {

    @Override
    public String call() {
        // Реализация задачи
        return "Привет!";
    }
}
```

Самый простой способ определить приоритет - воспользоваться одним из методов `with()`, указав приоритет и реализацию задачи.

```java
var myRunnableTask = IPriorityRunnable.withPriority(priority, () -> {
    // Реализация задачи
    System.out.println("Привет!");
});

var myCallableTask = IPriorityCallable.withPriority(priority, () -> {
    // Реализация задачи
    return "Привет!";
});
```

Создав задачу, ее можно передать на исполнение как обычно.

```java
IPriorityExecutorService priorityExecutor;
...
priorityExecutor.execute(myRunnableTask);
priorityExecutor.submit(myCallableTask);
```

#### Неявное указание приоритета

Если задачи поступают извне и имеют один и тот же приоритет, можно сразу определить исполнитель с этим уровнем приоритета и все
задачи с таким приоритетом делегировать ему. Для этого можно воспользоваться одним из методов `IPriorityExecutorService.with()`.

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

Удобство и основное преимущество такого подхода в том, что источник задач не отвечает за их приоритизацию.
