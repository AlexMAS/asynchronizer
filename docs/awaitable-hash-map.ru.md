# AwaitableHashMap

## Обстоятельство

* Есть пары ключ-значение, которые необходимо организовать в виде хэш-таблицы.
* Ключи добавляются/удаляются в/из хэш-таблицу асинхронно.
* Есть необходимость дождаться добавления/удаления некоторого ключа, чтобы продолжить асинхронный процесс.

## Использование

Использование класса [`AwaitableHashMap`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/AwaitableHashMap.java)
лучше описать на примере.

Представьте, что вы реализуете клиент для [Docker](https://www.docker.com/).
Создание и удаление контейнеров тяжелые и долгие операции, поэтому вы решили выполнять их асинхронно.
Каждый контейнер имеет уникальное имя и описывается классом `Container`.

```java
class Container {
    CompletableFuture<Void> start();
    CompletableFuture<Void> stop();
    CompletableFuture<CommandResult> execute(String command);
    ...
}
```

Для удобства вы решили отслеживать все существующие контейнеры, используя `AwaitableHashMap`.

```java
var containers = new AwaitableHashMap<String, Container>();
```

### Добавление значений

Сразу, как только контейнер создан и подготовлен к использованию, добавьте его в список `containers`. 

```java
void createContainer(String name, String image) {
    var container = new Container(name, image);
    ...
    containers.put(name, container);
}
```

### Ожидание значений

Вы можете взаимодействовать с контейнером сразу, как только он станет доступен. Предположим, вы собираетесь выполнить shell-команду.
Для доступа к контейнеру просто запросите его по имени, используя метод `await()`, возвращающий экземпляр `CompletableFuture<Container>`.
Ожидая результат, вы получите экземпляр `Container` запрашиваемого контейнера и сможете взаимодействовать с ним.

```java
var commandResult = containers.await("my-debian")
        .thenCompose(container -> container
                .execute("echo Hello World!"));
```

### Удаление значений

Сразу, как только контейнер удален, удалите его из списка `containers`.

```java
void deleteContainer(String name) {
    var container = containers.get(name);
    ...
    containers.remove(name);
}
```

### Ожидание удаления

Вы можете освободить какие-то ресурсы, связанные с контейнером, сразу, как только он удаляется. Предположим вам нужно удалить
все связанные с контейнером тома. Для ожидания удаления используйте метод `awaitRemove()`, возвращающий экземпляр `CompletableFuture<Boolean>`.
Ожидая результат, вы получите признак, был ли удален контейнер в действительности.

```java
containers.awaitRemove(containerName)
        .thenAccept(success -> {
            if (success) {
                volumes.removeRelated(containerName);
            }
        });
```
