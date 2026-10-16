package dev.chris0100.resilience;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Limits concurrent calls to one dependency so it cannot take all the threads of the service. */
public final class Bulkhead {

    public static class BulkheadFullException extends RuntimeException {
        public BulkheadFullException() {
            super("bulkhead is full");
        }
    }

    private final Semaphore permits;
    private final Duration maxWait;

    public Bulkhead(int maxConcurrentCalls, Duration maxWait) {
        this.permits = new Semaphore(maxConcurrentCalls);
        this.maxWait = maxWait;
    }

    public <T> T call(Supplier<T> action) {
        boolean acquired;
        try {
            acquired = permits.tryAcquire(maxWait.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BulkheadFullException();
        }
        if (!acquired) {
            throw new BulkheadFullException();
        }
        try {
            return action.get();
        } finally {
            permits.release();
        }
    }

    public int availablePermits() {
        return permits.availablePermits();
    }
}
