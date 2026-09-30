# Concurrency and threading

Escalate a concurrency issue to security when it can bypass authentication/permission checks, corrupt token validity or cache state, break key rotation, or expose sensitive data.

## Review targets

Flag verified instances of:

- Unsynchronized mutable shared collections, caches, or flags accessed across threads/coroutine contexts.
- Double-checked lazy initialization without `volatile`/`@Volatile`, or other unsafe publication.
- Visibility gaps between background writes and readers without a memory barrier.
- TOCTOU behavior around permissions or files after suspension or I/O.
- Blocking or long-running work on the main thread.
- Unbounded coroutine launches such as `repeat(1000) { launch { ... } }`, or executor creation without throttling/back-pressure.
- Manual thread lifetimes or missing cancellation propagation.
- `CancellationException` caught without being rethrown.
- Resources that are not closed in `finally`/`use` when cancellation can interrupt the operation.
- Repeated collection of a cold `Flow` that re-runs expensive work, or incorrect state/event flow semantics.

Use atomics for independent counters/flags, a mutex or synchronization for compound operations, and immutable snapshot replacement for infrequently updated shared structures. Prefer structured concurrency (`coroutineScope`/`supervisorScope`), owned lifecycle scopes, `withContext(Dispatchers.IO)` for blocking I/O, and channels/mutexes/semaphores instead of spin waits.

For large loops, verify cancellation through `isActive`. Avoid repeatedly switching dispatchers inside a tight hot loop.

Avoid `GlobalScope`; use lifecycle, `ViewModel`, or injected scopes. When a threading contract needs clarification, suggest the established annotations:

- `@MainThread`, `@AnyThread`, or `@WorkerThread`.
- `@GuardedBy("lock")` for guarded fields.
- `@Volatile` for independently read/written fields that do not use full synchronization.

## Example

Bad (data race):

```kotlin
if (cache[key] == null) {
    cache[key] = computeValue()
}
```

Good:

```kotlin
val value = cache.getOrPut(key) { computeValue() }
```

(Java volatile double-checked and Kotlin lazy examples are omitted; use the standard safe patterns.)

## Suppressions

Do not flag:

- Intentional thread confinement through a clearly enforced single-thread dispatcher/executor.
- Read-only data after construction (effectively immutable).
- Generated code with known synchronization wrappers.
