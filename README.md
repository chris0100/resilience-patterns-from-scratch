# Resilience Patterns From Scratch

Retry, circuit breaker, bulkhead and timeout implemented in plain Java, to understand what libraries like **Resilience4j** do under the hood.

| Pattern | Class | Resilience4j equivalent |
|---------|-------|-------------------------|
| Retry with exponential backoff + jitter, only for transient errors | `Retry` | `Retry` + `IntervalFunction.ofExponentialRandomBackoff` |
| Circuit breaker (CLOSED -> OPEN -> HALF_OPEN) | `CircuitBreaker` | `CircuitBreaker` |
| Bulkhead (max concurrent calls) | `Bulkhead` | `Bulkhead` (semaphore) |
| Timeout | `Timeout` | `TimeLimiter` |

## Run

```bash
mvn test
```

## Interview notes

- **Retry only transient failures** (timeouts, 503, connection reset). Never retry a 400 or a validation error.
- **Retries need idempotency**: if the first call actually succeeded, a retry can duplicate the effect.
- **Backoff + jitter** avoids a thundering herd when many clients retry at the same time.
- **Circuit breaker** fails fast when a dependency is down, protects your threads, and gives the dependency time to recover.
- **Order matters**: usually `Retry(CircuitBreaker(Bulkhead(Timeout(call))))`. The timeout is the innermost guard.
- In Spring Boot: `resilience4j-spring-boot3` with `@Retry`, `@CircuitBreaker`, `@Bulkhead` and config in `application.yml`.
