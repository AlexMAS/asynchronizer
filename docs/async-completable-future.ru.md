# AsyncCompletableFuture

## Обстоятельство

* Используется [асинхронный контекст](async-context.ru.md).
* Имеются асинхронные продолжения на безе экземпляра `CompletableFuture`.

## Использование

Класс [`AsyncCompletableFuture`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/AsyncCompletableFuture.java)
повторяет функциональность `java.util.concurrent.CompletableFuture`, но позволяет определить исполнитель по умолчанию.
Если исполнитель по умолчанию не определен явно, используется [`Asynchronizer.commonPool()`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/Asynchronizer.java).

Класс `AsyncCompletableFuture` позволяет установить исполнитель по умолчанию, учитывающий [асинхронный контекст](async-context.ru.md)
в продолжениях, для которых исполнитель не определен явно, как например, `CompletableFuture.thenAcceptAsync(Consumer)`.
В таких случаях `CompletableFuture` использует `ForkJoinPool.commonPool()`, и это может спровоцировать неожиданное
поведение для стороны, которая предоставляет экземпляр будущего. Как минимум, [асинхронный контекст](async-context.ru.md) не будет доступен
для продолжений, так они создаются с исполнителем, который не копирует контекст.

```java
IAsyncContext context = Asynchronizer.context();
context.setProperty("userId", 123);
...

AsyncCompletableFuture.completedFuture("Привет, ")
        // Это продолжение будет иметь тот же контекст
        .thenAcceptAsync(result -> {
            var userId = context.getProperty("userId");
            System.out.println(result + userId); // Вывод: "Привет, 123"
        })
        .get();
```
