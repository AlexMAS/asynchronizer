# Пакетная обработка

## Обстоятельство

* Имеется поток событий.
* События могут приходить из разных источников.
* Есть необходимость использовать пакетную обработку событий.
* Пакетная обработка должна выполняться асинхронно.

## Использование

### Событие

Опишите свое событие.

Например, вы получаете значения из IoT устройств. Каждое устройство может иметь несколько тегов,
значения которых меняются со временем. Тегом может быть сенсор (температуры, давления, мощности и т.д.)
или переключатель (описывающий состояния вида включено/выключено).

```java
record TagValue(String tag, Instant timestamp, double value) { }
```

### Обработчик событий

Реализуйте [пакетный обработчик](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/IQueueHandler.java) для ваших событий.

Например, нам нужно сохранять поступающие значения тегов.

```java
class TagValueLogger implements IQueueHandler<TagValue> {

    private final TagValueRepository tagValueRepository;

    @Override
    public void handle(Collection<TagValue> items) {
        tagValueRepository.saveAll(items);
    }
}
```

### Диспетчер очереди событий

Создайте экземпляр класса [`QueueDispatcher`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/QueueDispatcher.java).

Например, ниже мы создаем очередь, которая вызывает регистратор событий либо когда размер очереди
становится больше `1000`, либо не реже одного раза в минуту.

```java
TagValueLogger tagValueLogger;

var maxBufferSize = 1000;
var bufferFlushTimeout = Duration.ofMinutes(1);
var tagValueQueue = new QueueDispatcher<>(tagValueLogger, maxBufferSize, bufferFlushTimeout);
```

### Публикация событий

Публикуйте свои события.

```java
IQueueDispatcher<TagValue> tagValueQueue;

tagValueQueue.enqueue(new TagValue("outdoor-temperature", Instant.now(), 10));
```
