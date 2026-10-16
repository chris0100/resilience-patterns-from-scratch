package dev.chris0100.resilience;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class RetryTest {

    private final Retry retry = new Retry(3, Duration.ofMillis(100), Duration.ofSeconds(1),
            ex -> ex instanceof TransientException, d -> { /* no real sleep in tests */ });

    @Test
    void retriesTransientErrorsUntilSuccess() {
        AtomicInteger calls = new AtomicInteger();
        String result = retry.call(() -> {
            if (calls.incrementAndGet() < 3) {
                throw new TransientException("503");
            }
            return "ok";
        });
        assertEquals("ok", result);
        assertEquals(3, calls.get());
    }

    @Test
    void doesNotRetryBusinessErrors() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(IllegalArgumentException.class, () -> retry.call(() -> {
            calls.incrementAndGet();
            throw new IllegalArgumentException("400 bad request");
        }));
        assertEquals(1, calls.get());
    }

    @Test
    void givesUpAfterMaxAttempts() {
        AtomicInteger calls = new AtomicInteger();
        assertThrows(TransientException.class, () -> retry.call(() -> {
            calls.incrementAndGet();
            throw new TransientException("timeout");
        }));
        assertEquals(3, calls.get());
    }

    @Test
    void backoffNeverExceedsTheCap() {
        for (int attempt = 1; attempt <= 10; attempt++) {
            assertTrue(retry.delayFor(attempt).toMillis() <= 1_000);
        }
    }
}
