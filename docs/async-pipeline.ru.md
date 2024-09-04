# AsyncPipeline

## Обстоятельство

* Есть алгоритм, который состоит из нескольких асинхронных стадий.
* Есть желание, чтобы код алгоритма декларировал логику и был максимально очищен от технических деталей.

## Концепция

Использование `CompletableFuture` API для реализации даже не очень сложных асинхронных алгоритмов почти всегда заканчивается
спагетти-кодом, состоящим из множества callback-функций.

Класс [`AsyncPipeline`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/AsyncPipeline.java)
позволяет определить алгоритм, состоящий из асинхронных шагов, в декларативном стиле, скрыть большую часть технических
деталей поддержки параллелизма, сделать код чистым и сфокусированным на бизнес-правилах.

Класс `AsyncPipeline` реализует шаблон проектирования "конвейер" (pipeline) и организует вычислительный процесс как последовательность асинхронных
стадий, где результат одной стадии является входными данными для другой.

![](images/async-pipeline.pic1.ru.png)

Все вычисления выполняются в контексте [`IAsyncFlow`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IAsyncFlow.java),
который является общим для всех стадий конвейера. Это позволяет разделять контекст исполнения и прерывать выполнение конвейера
в любой момент в зависимости от бизнес-правил.

## Использование

Использование класса `AsyncPipeline` лучше описать на примере.

Представьте, что вы реализуете некоторую разновидность [распределенной системы контроля версий (DVCS)](https://en.wikipedia.org/wiki/Distributed_version_control),
подобной Git. В вашей кодовой базе есть два класса для работы с локальным и удаленным репозиторием.

```java
class LocalRepository {
    CompletableFuture<Commit> getLastCommit();
    CompletableFuture<Void> saveCommits(Collection<Commit> commits);
    ...
}

class RemoteRepository {
    CompletionStage<Collection<Commit>> loadCommitsSince(CompletableFuture<Commit> lastCommit);
    ...
}
```

Основываясь на этом, вы можете реализовать метод `pull`.

```java
CompletableFuture<Void> pull(String repository) {
    var localRepository = new LocalRepository(repository);
    var remoteRepository = new RemoteRepository(repository);

    return AsyncPipeline
            .supply(flow -> localRepository.getLastCommit())
            .await((flow, lastCommit) -> remoteRepository.loadCommitsSince(lastCommit))
            .await((flow, remoteCommits) -> localRepository.saveCommits(remoteCommits))
            .toCompletableFuture();
}
```

Вы можете оптимизировать вычислительный поток, прервав его, если нет удаленных комитов.

```java
CompletableFuture<Void> pull(String repository) {
    var localRepository = new LocalRepository(repository);
    var remoteRepository = new RemoteRepository(repository);

    return AsyncPipeline
            .supply(flow -> localRepository.getLastCommit())
            .await((flow, lastCommit) -> remoteRepository.loadCommitsSince(lastCommit))
            // Последующие стадии не будут выполнены, если нет удаленных комитов
            .interruptIf(remoteCommits -> remoteCommits.isEmpty())
            .await((flow, remoteCommits) -> localRepository.saveCommits(remoteCommits))
            .toCompletableFuture();
}
```

Достаточно просто можно добавить обработчик ошибок для каждой стадии. Например, давайте залогируем ошибки обращения к удаленному репозиторию.

```java
CompletableFuture<Void> pull(String repository) {
    var localRepository = new LocalRepository(repository);
    var remoteRepository = new RemoteRepository(repository);

    return AsyncPipeline
            .supply(flow -> localRepository.getLastCommit())
            .await((flow, lastCommit) -> remoteRepository.loadCommitsSince(lastCommit))
            // Здесь обработка возможных ошибок предшествующей стадии
            .onError(loadError -> log.atError().setCause(loadError)
                    .log("Невозможно загрузить изменения из удаленного репозитория."))
            .interruptIf(remoteCommits -> remoteCommits.isEmpty())
            .await((flow, remoteCommits) -> localRepository.saveCommits(remoteCommits))
            .toCompletableFuture();
}
```

Если есть код, который должен быть выполнен в любом случае в конце конвейера, определите finally-блок.

```java
CompletableFuture<Void> pull(String repository) {
    var localRepository = new LocalRepository(repository);
    var remoteRepository = new RemoteRepository(repository);

    return AsyncPipeline
            .supply(flow -> localRepository.getLastCommit())
            .await((flow, lastCommit) -> remoteRepository.loadCommitsSince(lastCommit))
            .onError(loadError -> log.atError().setCause(loadError)
                    .log("Невозможно загрузить изменения из удаленного репозитория."))
            .interruptIf(remoteCommits -> remoteCommits.isEmpty())
            .await((flow, remoteCommits) -> localRepository.saveCommits(remoteCommits))
            // Эта функция будет выполнена в любом случае в конце конвейера
            .onFinally((result, error) -> remoteRepository.close())
            .toCompletableFuture();
}
```
