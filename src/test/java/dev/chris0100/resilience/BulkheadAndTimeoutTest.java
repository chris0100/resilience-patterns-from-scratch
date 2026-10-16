package dev.chris0100.resilience;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BulkheadAndTimeoutTest {

    @Test
    void bulkheadRejectsWhenAllPermitsAreBusy() throws Exception {
        Bulkhead bulkhead = new Bulkhead(1, Duration.ofMillis(50));
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread busy = new Thread(() -> bulkhead.call(() -> {
            inside.countDown();
            await(release);
            return "done";
        }));
        busy.start();
        assertTrue(inside.await(1, TimeUnit.SECONDS));

        assertThrows(Bulkhead.BulkheadFullException.class, () -> bulkhead.call(() -> "second"));

        release.countDown();
        busy.join();
        assertEquals(1, bulkhead.availablePermits());
    }

    @Test
    void timeoutTurnsASlowCallIntoATransientError() {
        ExecutorService pool = Executors.newCachedThreadPool();
        Timeout timeout = new Timeout(pool);

        assertEquals("fast", timeout.call(() -> "fast", Duration.ofMillis(500)));
        assertThrows(TransientException.class, () -> timeout.call(() -> {
            await(new CountDownLatch(1));
            return "slow";
        }, Duration.ofMillis(100)));
        pool.shutdownNow();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
