# AwaitableQueue

## Обстоятельство

* Есть много асинхронных задач, которые нужно ожидать (для продолжения работы).
* Нет желания ожидать каждую асинхронную задачу в отдельном потоке.

## Концепция

Иногда мы вынуждены использовать стороннюю библиотеку с асинхронными методами, возвращающими экземпляры `java.util.concurrent.Future`.
Такой API неудобен, поскольку `Future` не предоставляет элегантного способа ожидания результата. Мы вынуждены либо запрашивать 
результат синхронно, вызывая метод `Future.get()`, либо проверять его доступность периодически, вызывая метод
`Future.get(long, TimeUnit)`. Первый способ сводит на нет все преимущества асинхронности, и оба способа могут
оставить вас с огромным количеством потоков в состоянии `WAITING`/`TIMED_WAITING`. Если вы используете пул потоков,
в конечном итоге он будет исчерпан. Всё зависит от того, как много подобных асинхронных задач ожидаются параллельно.
Так или иначе, большое количество ждущих потоков означает неоптимальное использование ресурсов (CPU/RAM).

Класс [`AwaitableQueue`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/AwaitableQueue.java) предоставляет эффективный
подход к ожиданию завершения асинхронных задач, которые не предоставляют удобных методов ожидания, как например у класса `CompletableFuture`.
Каждая асинхронная задача должна реализовывать интерфейс [`IAwaitable`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IAwaitable.java)
и поставлена в очередь с помощью метода `enqueue(IAwaitable)` или `enqueue(IAwaitable, Duration)`. Эти методы возвращают
экземпляр класса `CompletableFuture`, который может быть использован для асинхронного ожидания без каких-либо недостатков.
Предоставленное будущее может быть безопасно прервано, например, с помощью метода `CompletableFuture.cancel(boolean)`.
Как только задача завершается, она автоматически удаляется из очереди.

Реализация основана на однопоточном исполнителе, который в бесконечном цикле, последовательно опрашивает поставленные в очередь задачи,
предоставляя каждой небольшой интервал времени для синхронного ожидания завершения. Если задача завершается вовремя, соответствующее будущее
также завершается, а задача удаляется из очереди. Ожидание всех задач в одном потоке позволяет эффективно утилизировать CPU/RAM -
только один поток в состоянии активного ожидания. С другой стороны, все задачи опрашиваются последовательно, значит, в случае длинной очереди
быстрые задачи могут ожидать завершения дольше. Этот негативный эффект можно минимизировать, уменьшив таймаут опроса задач
или создав отдельную очередь.

## Использование

В следующем примере асинхронный метод `download()` возвращает содержимое ресурса по его URI. Предоставленный экземпляр
`Future<String>` преобразуется в экземпляр `CompletableFuture<String>`, который можно эффективно ожидать,
например, используя [`AsyncPipeline`](async-pipeline.ru.md).

```java
AwaitableQueue awaitableQueue = new AwaitableQueue(Asynchronizer.commonPool());
...
Future<String> contentFuture = download("https://...");
IAwaitable<String> contentAwaitable = IAwaitable.of(contentFuture);
CompletableFuture<String> contentCompletableFuture = awaitableQueue.enqueue(contentAwaitable);
```

Таким образом, `AwaitableQueue` может быть использован для преобразования устаревшего, неудобного, неуклюжего API
в современный асинхронный аналог.

### IAwaitable

Интерфейс `IAwaitable` уже имеет фабричный метод на основе экземпляра `Future`. Другие типы ожидаемых объектов
также легко преобразовать в `IAwaitable`. Например, у вас есть экземпляр `CountDownLatch`, и вы можете получить
доступ к некоторому результату сразу после снятия блокировки.

```java
// Блокировка
CountDownLatch resultLatch;
// Метод доступа к результату
Supplier<String> resultSupplier;
```

Добавьте класс `LatchedResult`, который реализует интерфейс `IAwaitable`.

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

После этого вы сможете создать экземпляр `IAwaitable`.

```java
IAwaitable<String> latchedAwaitable = new LatchedResult<>(resultLatch, resultSupplier);
CompletableFuture<String> resultCompletableFuture = awaitableQueue.enqueue(latchedAwaitable);
```
