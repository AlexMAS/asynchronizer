# Asynchronizer

![Asynchronizer](logo-128.png)

[![javadoc][asynchronizer-javadoc-badge]][asynchronizer-javadoc-page]

Java Concurrent API does not have enough to develop asynchronous code and work with it comfortably.
Every time we have to invent something, otherwise the business logic can be obscured by all the technical
details needed to support asynchronicity. The library provides a set of tools that aim to simplify
development of asynchronous components, make their code easy-to-read and expressive, and clean
the logic from technical details as much as possible.

For example, an implementation of the `git pull` command might look like this:

```java
CompletableFuture<Void> pull(String repository) {
    var localRepository = new LocalRepository(repository);
    var remoteRepository = new RemoteRepository(repository);

    return AsyncPipeline
            // Getting the latest commit of the local repository
            .supply(flow -> localRepository.getLastCommit())
            // Loading a set of new commits from the remote repository
            .await((flow, lastCommit) -> remoteRepository.loadCommitsSince(lastCommit))
            // Saving the new commits to the local repository
            .await((flow, remoteCommits) -> localRepository.saveCommits(remoteCommits))
            .toCompletableFuture();
}
```

## Installation

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

## Documentation

* [Usage Guide](docs/README.md)

[asynchronizer-javadoc-badge]: https://javadoc.io/badge2/ru.asynchronizer/asynchronizer/javadoc.svg
[asynchronizer-javadoc-page]: https://javadoc.io/doc/ru.asynchronizer/asynchronizer
