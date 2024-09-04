# AsyncPipeline

## Circumstance

* There is an algorithm which consists of several asynchronous stages.
* You would like the algorithm code declare logic and be as clean from technical details as possible.

## Concept

Using the `CompletableFuture` API to implement even not very complicated asynchronous algorithms almost always ends up
with spaghetti-code consisting lots of callback functions.

The [`AsyncPipeline`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/AsyncPipeline.java) class
allows to define an algorithm consisting of asynchronous stages in a declarative style, hide most of the technical
details supporting concurrency, make the code clean and focused on business rules. 

The `AsyncPipeline` class implements the pipeline design pattern and organizes computation as a sequence of asynchronous
stages where the result of one stage is the input data to the next.

![](images/async-pipeline.pic1.png)

All computations are performed in the context of [`IAsyncFlow`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IAsyncFlow.java)
which is common for all stages of the pipeline. It allows to share the execution context and interrupt the pipeline
execution at any moment depending on business rules.

## Using

Usage of the `AsyncPipeline` class better to describe with an example.

Imagine you implement some kind of [distributed version control system (DVCS)](https://en.wikipedia.org/wiki/Distributed_version_control)
like Git. In your codebase there are two classes to work with local and remote repositories.

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

Based on this you can implement the `pull` method.

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

You can optimize the computation flow by interrupting it if there are no remote commits.

```java
CompletableFuture<Void> pull(String repository) {
    var localRepository = new LocalRepository(repository);
    var remoteRepository = new RemoteRepository(repository);

    return AsyncPipeline
            .supply(flow -> localRepository.getLastCommit())
            .await((flow, lastCommit) -> remoteRepository.loadCommitsSince(lastCommit))
            // The next stages won't be executed if there are no remote commits
            .interruptIf(remoteCommits -> remoteCommits.isEmpty())
            .await((flow, remoteCommits) -> localRepository.saveCommits(remoteCommits))
            .toCompletableFuture();
}
```

It is easy to add an error handler for each stage. For example, let's log errors of the remote repository.

```java
CompletableFuture<Void> pull(String repository) {
    var localRepository = new LocalRepository(repository);
    var remoteRepository = new RemoteRepository(repository);

    return AsyncPipeline
            .supply(flow -> localRepository.getLastCommit())
            .await((flow, lastCommit) -> remoteRepository.loadCommitsSince(lastCommit))
            // Here handle possible errors of the above stage
            .onError(loadError -> log.atError().setCause(loadError)
                    .log("Cannot load commits from the remote repository."))
            .interruptIf(remoteCommits -> remoteCommits.isEmpty())
            .await((flow, remoteCommits) -> localRepository.saveCommits(remoteCommits))
            .toCompletableFuture();
}
```

If there is code which must be performed at the end of the pipeline anyway, define a finally-block.

```java
CompletableFuture<Void> pull(String repository) {
    var localRepository = new LocalRepository(repository);
    var remoteRepository = new RemoteRepository(repository);

    return AsyncPipeline
            .supply(flow -> localRepository.getLastCommit())
            .await((flow, lastCommit) -> remoteRepository.loadCommitsSince(lastCommit))
            .onError(loadError -> log.atError().setCause(loadError)
                    .log("Cannot load commits from the remote repository."))
            .interruptIf(remoteCommits -> remoteCommits.isEmpty())
            .await((flow, remoteCommits) -> localRepository.saveCommits(remoteCommits))
            // This function will be executed at the end of the pipeline anyway
            .onFinally((result, error) -> remoteRepository.close())
            .toCompletableFuture();
}
```
