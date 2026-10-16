package dev.chris0100.resilience;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    /** Clock we can move forward manually. */
    static final class MutableClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @Test
    void opensAfterFailuresFailsFastAndRecoversInHalfOpen() {
        MutableClock clock = new MutableClock();
        CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofSeconds(30), clock);

        for (int i = 0; i < 3; i++) {
            assertThrows(TransientException.class, () -> breaker.call(() -> {
                throw new TransientException("down");
            }));
        }
        assertEquals(CircuitBreaker.State.OPEN, breaker.state());
        assertThrows(CircuitBreaker.OpenCircuitException.class, () -> breaker.call(() -> "never called"));

        clock.now = clock.now.plusSeconds(31);
        assertEquals(CircuitBreaker.State.HALF_OPEN, breaker.state());

        assertEquals("recovered", breaker.call(() -> "recovered"));
        assertEquals(CircuitBreaker.State.CLOSED, breaker.state());
    }

    @Test
    void failureInHalfOpenOpensAgain() {
        MutableClock clock = new MutableClock();
        CircuitBreaker breaker = new CircuitBreaker(1, Duration.ofSeconds(10), clock);

        assertThrows(TransientException.class, () -> breaker.call(() -> { throw new TransientException("x"); }));
        clock.now = clock.now.plusSeconds(11);
        assertThrows(TransientException.class, () -> breaker.call(() -> { throw new TransientException("still down"); }));
        assertEquals(CircuitBreaker.State.OPEN, breaker.state());
    }
}
