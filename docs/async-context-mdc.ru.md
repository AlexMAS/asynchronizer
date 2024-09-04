# Асинхронный контекст и MDC

## Обстоятельство

* Используется [SLF4J MDC](https://www.slf4j.org/manual.html#mdc).
* Используется [асинхронный контекст](async-context.md).
* Некоторые свойства контекста должны быть включены в записи журнала событий.

## Использование

В своем [IoC](https://en.wikipedia.org/wiki/Inversion_of_control)-контейнере зарегистрируйте
класс [`AsyncContextMdcAdapter`](../asynchronizer/src/main/java/ru/asynchronizer/util/concurrent/AsyncContextMdcAdapter.java)
как singleton-зависимость приложения.

На старте приложения вызовите метод `run()`:

```java
AsyncContextMdcAdapter mdcAdapter;
...
mdcAdapter.run();
```

Например, [контекст](async-context.ru.md) содержит свойство `userId` и вы используете [`log4j2`](https://logging.apache.org/log4j).
Чтобы добавить это свойство в записи журнала, добавьте его в [шаблон записи](https://logging.apache.org/log4j/2.x/manual/pattern-layout.html)
как `%X{userId}`.

### Spring Boot

Если вы используете Spring Boot, добавьте следующую регистрацию:

```java
@Bean
public AsyncContextMdcAdapter mdcAdapter(IAsyncContext context) {
    return new AsyncContextMdcAdapter(context);
}
```

И обработчик события запуска приложения:

```java
@Component
public class AsyncContextMdcAdapterRunner {

    private final AsyncContextMdcAdapter mdcAdapter;

    ...

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationStarted() {
        mdcAdapter.run();
    }
}
```
