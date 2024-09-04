# RepeatedCompletableFuture

## Обстоятельство

* Имеется [идемпотентная](https://en.wikipedia.org/wiki/Idempotence) функция, которая завершается неудачей, пока не будут обеспечены подходящие условия.
* Функция должна исполняться до тех пор, пока не будет достигнут успех.
* Функция должна выполняться асинхронно.

## Использование

Если есть возможность запускать зависимые микросервисы одновременно, это может упростить их развертывание.
Но для этого нам нужно сделать эти микросервисы терпимее к кратковременной недоступности некоторых ресурсов.
Обычно, прежде чем взаимодействовать с ресурсом, мы должны установить с ним соединение. Поскольку во время
подключения ресурс может быть некоторое время недоступен, мы выполняем несколько попыток подключения прежде,
чем сообщить о проблеме.

Допустим, ваш ресурс имеет следующий клиентский API.

```java
interface SomeResourceClient {
    SomeResourceConnection connect(String uri) throws IOException;
}

interface SomeResourceConnection {
    String loadData();
}
```

Используя класс [`RepeatedCompletableFuture`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/RepeatedCompletableFuture.java)
вы можете создать асинхронную задачу, которая пытается подключиться к этому ресурсу до тех пор, не будет достигнут успех.

```java
SomeResourceClient resourceClient;
...

RepeatedCompletableFuture<SomeResourceConnection> resourceConnection = RepeatedCompletableFuture.startAttempts(
        () -> resourceClient.connect("my/resource"),
        error -> log.atWarn().setCause(error).log("Попытка подключения не удалась."),
        Duration.ofSeconds(5)
);
```

В последнем примере метод `connect()` будет вызываться *бесконечно*, пока не завершится без исключения.
Если попытка завершается неудачей, ошибка записывается в журнал, и новая попытка подключения выполняется
через 5 секунд.

Если бесконечные попытки нецелесообразны, вы можете прервать этот процесс, вызвав метод `cancel()`.
В следующем примере попытки прерываются после 10 неудач.

```java
SomeResourceClient resourceClient;
AtomicInteger connectionAttempts;
RepeatedCompletableFuture<SomeResourceConnection> resourceConnection;
...

void connect() {
    resourceConnection = RepeatedCompletableFuture.startAttempts(
            () -> resourceClient.connect("my/resource"),
            error -> {
                log.atWarn().setCause(error).log("Попытка подключения не удалась.");

                if (connectionAttempts.incrementAndGet() >= 10) {
                    resourceConnection.cancel(true);
                }
            },
            Duration.ofSeconds(5)
    );
}
```

Для доступа к соединению вы можете дождаться успеха или использовать текущее состояние соединения, вызвав метод `lastAttempt()`.
В первом случае вы не сможете работать с ресурсом, пока соединение не будет установлено или процесс этого соединения не будет прерван.
Во втором случае вы можете получить ошибку, если последняя попытка была неудачной.

```java
// Дождитесь успешного подключения
resourceConnection
        .thenAccept(connection -> {
            var data = connection.loadData();
            System.out.println(data);
        });

// Или используйте текущее состояние соединения
resourceConnection.lastAttempt()
        .thenAccept(connection -> {
            var data = connection.loadData();
            System.out.println(data);
        });
```
