package dev.chris0100.resilience;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Retries transient failures with exponential backoff and full jitter. */
public final class Retry {

    private final int maxAttempts;
    private final Duration baseDelay;
    private final Duration maxDelay;
    private final Predicate<Throwable> retryOn;
    private final Sleeper sleeper;

    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration d) throws InterruptedException;
    }

    public Retry(int maxAttempts, Duration baseDelay, Duration maxDelay, Predicate<Throwable> retryOn, Sleeper sleeper) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        this.maxAttempts = maxAttempts;
        this.baseDelay = baseDelay;
        this.maxDelay = maxDelay;
        this.retryOn = retryOn;
        this.sleeper = sleeper;
    }

    public static Retry transientOnly(int maxAttempts, Duration baseDelay) {
        return new Retry(maxAttempts, baseDelay, Duration.ofSeconds(10),
                ex -> ex instanceof TransientException, Thread::sleep);
    }

    public <T> T call(Supplier<T> action) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException ex) {
                last = ex;
                if (!retryOn.test(ex) || attempt == maxAttempts) {
                    throw ex;
                }
                pause(delayFor(attempt));
            }
        }
        throw last; // unreachable
    }

    /** Full jitter: random value between 0 and min(maxDelay, base * 2^(attempt-1)). */
    Duration delayFor(int attempt) {
        long cap = Math.min(maxDelay.toMillis(), baseDelay.toMillis() * (1L << Math.min(attempt - 1, 20)));
        return Duration.ofMillis(ThreadLocalRandom.current().nextLong(cap + 1));
    }

    private void pause(Duration d) {
        try {
            sleeper.sleep(d);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting to retry", e);
        }
    }
}
