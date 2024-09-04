# Batch Processing

## Circumstance

* There is an event stream.
* The events can come from different sources.
* There is need to process events with batching.
* The batch processing must be asynchronous.

## Using

### Event

Describe your event.

For example, you receive values from IoT devices. Each device can have several tags
which values change over time. A tag can be any sensor (temperature, pressure, power, etc.)
or switch (describing on/off states).

```java
record TagValue(String tag, Instant timestamp, double value) { }
```

### Event Handler

Implement [a batch handler](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IQueueHandler.java) for your events.

For example, we need to store incoming tag values.

```java
class TagValueLogger implements IQueueHandler<TagValue> {

    private final TagValueRepository tagValueRepository;

    @Override
    public void handle(Collection<TagValue> items) {
        tagValueRepository.saveAll(items);
    }
}
```

### Event Queue Dispatcher

Create an instance of the [`QueueDispatcher`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/QueueDispatcher.java) class.

For example, below we create the queue which calls the logger either when the queue size
grows larger than `1000`, or at least once per minute.

```java
TagValueLogger tagValueLogger;

var maxBufferSize = 1000;
var bufferFlushTimeout = Duration.ofMinutes(1);
var tagValueQueue = new QueueDispatcher<>(tagValueLogger, maxBufferSize, bufferFlushTimeout);
```

### Event Publishing

Publish your events.

```java
IQueueDispatcher<TagValue> tagValueQueue;

tagValueQueue.enqueue(new TagValue("outdoor-temperature", Instant.now(), 10));
```
