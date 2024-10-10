# Asynchronizer

![Asynchronizer](logo-128.png)

[![javadoc][asynchronizer-javadoc-badge]][asynchronizer-javadoc-page]

Java Concurrent API не предоставляет достаточных возможностей для комфортной разработки
асинхронного кода и работы с ним. Всякий раз приходится что-то изобретать, иначе прикладная
логика может потеряться на фоне обилия технических деталей, необходимых для поддержки асинхронности.
Библиотека предоставляет набор средств, нацеленных на упрощение разработки асинхронных компонентов,
сделать их код более понятным и выразительным, максимально очистить логику от технических деталей.

Например, реализация команды `git pull` могла бы выглядеть так:

```java
CompletableFuture<Void> pull(String repository) {
    var localRepository = new LocalRepository(repository);
    var remoteRepository = new RemoteRepository(repository);

    return AsyncPipeline
            // Получение последнего комита в локальном репозитории
            .supply(flow -> localRepository.getLastCommit())
            // Загрузка новых комитов из удаленного репозитория
            .await((flow, lastCommit) -> remoteRepository.loadCommitsSince(lastCommit))
            // Сохранение новых комитов в локальном репозитории
            .await((flow, remoteCommits) -> localRepository.saveCommits(remoteCommits))
            .toCompletableFuture();
}
```

## Установка

**Gradle**
```groovy
implementation 'ru.asynchronizer:asynchronizer:1.0.0'
```

**Gradle** (Kotlin)
```kotlin
implementation("ru.asynchronizer:asynchronizer:1.0.0")
```

**Maven**
```xml
<dependency>
    <groupId>ru.asynchronizer</groupId>
    <artifactId>asynchronizer</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Документация

* [Руководство по использованию](docs/README.ru.md)

[asynchronizer-javadoc-badge]: https://javadoc.io/badge2/ru.asynchronizer/asynchronizer/javadoc.svg
[asynchronizer-javadoc-page]: https://javadoc.io/doc/ru.asynchronizer/asynchronizer
