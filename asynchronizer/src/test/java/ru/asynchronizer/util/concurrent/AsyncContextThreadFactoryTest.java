package ru.asynchronizer.util.concurrent;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AsyncContextThreadFactoryTest {

    @Test
    public void shouldCreateThreads() {

        // Given
        var normalFactory = new AsyncContextThreadFactory(getClass(), false, (t, e) -> { });
        var daemonFactory = new AsyncContextThreadFactory(getClass(), true, (t, e) -> { });

        // When
        var normalThread = normalFactory.newThread(() -> { });
        var daemonThread = daemonFactory.newThread(() -> { });

        // Then

        assertThat(normalThread).isNotNull();
        assertThat(normalThread.isDaemon()).isEqualTo(false);

        assertThat(daemonThread).isNotNull();
        assertThat(daemonThread.isDaemon()).isEqualTo(true);
    }

    @Test
    public void shouldHandleUncaughtException() throws Exception {

        // Given
        var expectedException = new RuntimeException();
        var actualException = new CompletableFuture<Throwable>();
        var factory = new AsyncContextThreadFactory(getClass(), false, (t, e) -> actualException.complete(e));

        // When
        var thread = factory.newThread(() -> { throw expectedException; });
        thread.start();
        thread.join();

        // Then
        assertThat(actualException.isDone()).isEqualTo(true);
        assertThat(actualException.get()).isEqualTo(expectedException);
    }
}
