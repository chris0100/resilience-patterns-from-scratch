package dev.chris0100.resilience;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/** Bounds how long we wait for a call. The slow call is cancelled so it does not keep a thread forever. */
public final class Timeout {

    private final ExecutorService executor;

    public Timeout(ExecutorService executor) {
        this.executor = executor;
    }

    public <T> T call(Supplier<T> action, Duration limit) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(action, executor);
        try {
            return future.get(limit.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new TransientException("call timed out after " + limit.toMillis() + " ms");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException(e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
