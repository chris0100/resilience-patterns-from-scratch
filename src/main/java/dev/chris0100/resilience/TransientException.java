package dev.chris0100.resilience;

/** Marks failures that are worth retrying (timeouts, 503, connection reset). */
public class TransientException extends RuntimeException {
    public TransientException(String message) {
        super(message);
    }
}
