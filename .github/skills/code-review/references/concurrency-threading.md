# Concurrency and threading

Escalate a concurrency issue to security when it can bypass authentication/permission checks, corrupt token validity or cache state, break key rotation, or expose sensitive data.

## Review targets

Flag verified instances of:

- Unsynchronized mutable shared collections, caches, or flags accessed across threads/coroutine contexts.
- Double-checked lazy initialization without `volatile`/`@Volatile`, or other unsafe publication.
- Visibility gaps between background writes and readers without a memory barrier.
- TOCTOU behavior around permissions or files after suspension or I/O.
- Blocking or long-running work on the main thread.
- Unbounded coroutine launches or executor creation without throttling/back-pressure.
- `GlobalScope`, manual thread lifetimes, or missing cancellation propagation.
- `CancellationException` caught without being rethrown.
- Resources that are not closed in `finally`/`use` when cancellation can interrupt the operation.
- Repeated collection of a cold `Flow` that re-runs expensive work, or incorrect state/event flow semantics.
- Compound cache/token operations guarded only by atomic handling of individual fields.

Use atomics for independent counters/flags, a mutex or synchronization for compound operations, and immutable snapshot replacement for infrequently updated shared structures. Prefer structured concurrency (`coroutineScope`/`supervisorScope`), owned lifecycle scopes, `withContext(Dispatchers.IO)` for blocking I/O, and channels/mutexes/semaphores instead of spin waits.

For large loops, verify cancellation through `isActive`. Avoid repeatedly switching dispatchers inside a tight hot loop.

When a threading contract is unclear and relevant to correctness, recommend established annotations such as `@MainThread`, `@AnyThread`, `@WorkerThread`, `@GuardedBy`, or `@Volatile`; do not request annotations without a concrete ambiguity or race.

## Example

This check-then-write is not atomic:

```kotlin
if (cache[key] == null) {
    cache[key] = computeValue()
}
```

Prefer the established atomic access pattern:

```kotlin
val value = cache.getOrPut(key) { computeValue() }
```

## Suppressions

Do not flag:

- Clearly enforced single-thread confinement.
- Read-only data after safe construction and publication.
- Generated code with a verified synchronization wrapper.
- Hypothetical races that depend only on unverified flight/configuration combinations.

Do not introduce synchronization requirements beyond the behavior and rules already established in the repository instructions.
