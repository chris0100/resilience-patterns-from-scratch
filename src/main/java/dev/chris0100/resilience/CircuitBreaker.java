package dev.chris0100.resilience;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Minimal count-based circuit breaker.
 * CLOSED: calls pass; after N consecutive failures -> OPEN.
 * OPEN: calls fail fast until the wait duration passes -> HALF_OPEN.
 * HALF_OPEN: one trial call; success -> CLOSED, failure -> OPEN again.
 */
public final class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    public static class OpenCircuitException extends RuntimeException {
        public OpenCircuitException() {
            super("circuit is open - failing fast");
        }
    }

    private final int failureThreshold;
    private final Duration openWait;
    private final Clock clock;

    private State state = State.CLOSED;
    private int consecutiveFailures;
    private Instant openedAt;

    public CircuitBreaker(int failureThreshold, Duration openWait, Clock clock) {
        this.failureThreshold = failureThreshold;
        this.openWait = openWait;
        this.clock = clock;
    }

    public synchronized State state() {
        if (state == State.OPEN && clock.instant().isAfter(openedAt.plus(openWait))) {
            state = State.HALF_OPEN;
        }
        return state;
    }

    public <T> T call(Supplier<T> action) {
        if (state() == State.OPEN) {
            throw new OpenCircuitException();
        }
        try {
            T result = action.get();
            onSuccess();
            return result;
        } catch (RuntimeException ex) {
            onFailure();
            throw ex;
        }
    }

    private synchronized void onSuccess() {
        consecutiveFailures = 0;
        state = State.CLOSED;
    }

    private synchronized void onFailure() {
        consecutiveFailures++;
        if (state == State.HALF_OPEN || consecutiveFailures >= failureThreshold) {
            state = State.OPEN;
            openedAt = clock.instant();
        }
    }
}
