# Concurrency and threading

## Contents

- [Change-driven routing](#change-driven-routing)
- [Review targets](#review-targets)
- [Lock identity and before/after analysis](#lock-identity-and-beforeafter-analysis)
- [Nested acquisitions](#nested-acquisitions)
- [Monitor stability](#monitor-stability)
- [Critical-section scope](#critical-section-scope)
- [Refactors that lose protection](#refactors-that-lose-protection)
- [Atomic map initialization](#atomic-map-initialization)
- [Finding evidence](#finding-evidence)
- [Suppressions](#suppressions)

Escalate a concurrency issue to security when it can bypass authentication/permission checks, corrupt token validity or cache state, break key rotation, or expose sensitive data.

## Change-driven routing

Perform deeper analysis for changed, added, removed, or moved `synchronized` blocks/methods, Kotlin `@Synchronized`, explicit `Lock`/`withLock`/`Mutex`, and modified lock expressions, helpers, receivers, or shared-state access paths. Include refactors without new locking tokens in the diff, such as moving a guarded mutation into another helper or changing its callers.

Limit the trace to changed behavior and the callers/callees needed to establish its protection. Do not flag a file merely because it contains synchronization elsewhere.

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

## Lock identity and before/after analysis

1. Identify affected shared state, its invariant, and the threads/coroutines that can access it concurrently. Compare before/after held-lock sets at each affected read, write, and call boundary, including releases and exceptional exits.
2. Resolve actual lock identities and aliases. Instance synchronized methods lock `this`; static synchronized methods lock the declaring class's `Class` object. Resolve the JVM owner for Kotlin `@Synchronized`, including object/companion and static bridges; do not infer identity from receiver spelling or annotation alone.
3. Trace relevant caller/callee acquisitions while locks remain held. Record paths such as `refresh: cacheLock -> keyLock` and the reachable competing path. Verify that two names denote the same object, or that two objects are actually distinct, before comparing order.
4. Check the concrete primitive. JVM monitors are reentrant on the same thread; Kotlin `Mutex` is non-reentrant. Reacquiring a held mutex before release can self-suspend (or fail for a repeated owner token), not safely reenter. Check an explicit `Lock` implementation's reentrancy and paired release (`finally`/`withLock`); `synchronized(lock)` and `lock.lock()` are different mechanisms.
5. State unresolved aliases, unknown callees, or unverified concurrent reachability. Follow concrete implementations only as needed to establish actual acquisitions. Do not invent a lock edge from an unknown implementation or call a partial trace proven deadlock.

## Nested acquisitions

Report an order inversion only with distinct locks, conflicting held-lock order, and concurrently reachable paths. For this illustrative shared instance, concurrent `refresh()` and `rotate()` can deadlock:

```java
final class LockOrderExample {
    private final Object cacheLock = new Object();
    private final Object keyLock = new Object();
    private int version;

    void refresh() {
        synchronized (cacheLock) {
            synchronized (keyLock) { version++; }
        }
    }

    void rotate() {
        synchronized (keyLock) {
            synchronized (cacheLock) { version++; }
        }
    }
}
```

ABBA evidence: T1 in `refresh` holds `cacheLock` and waits for `keyLock`; T2 in `rotate` holds `keyLock` and waits for `cacheLock`. Recommend a consistent order across both paths and their concrete callees, not indiscriminate lock removal.

True negatives, absent another conflicting path:

- Both entry points acquire `cacheLock` then `keyLock` on this same instance. Nesting alone is not a defect.
- `synchronized (cacheLock) { synchronized (cacheLock) { version++; } }` reenters the same JVM monitor on one thread. The same applies when a helper reacquires that exact monitor.
- Different instances' locks do not form this ABBA cycle unless aliases or shared callees connect them; verify object identity first.

Do not apply JVM reentry reasoning to `mutex.withLock { mutex.withLock { update() } }`: if that path reacquires the same Kotlin mutex before release, it cannot reenter. Establish the actual path/identity rather than treating every nested locking construct alike.

## Monitor stability

Flag replacement only when it can split protection for the same shared state. Example members of a shared object:

```java
private volatile Object lock = new Object();
private int count;

void increment() {
    synchronized (lock) { count++; }
}

void replaceLock() {
    lock = new Object();
}
```

T1 can hold the old monitor while another thread replaces `lock` and T2 enters on the new monitor. Both increments now access `count` without a common exclusion/visibility boundary. `volatile` makes replacement visible, not mutually exclusive with holders of the old object.

Safe counterexample: remove replacement, use `private final Object lock = new Object()` (or Kotlin `private val lock = Any()`), safely publish the owning object, and guard every relevant state access with that one stable monitor. Do not request `final` just because a monitor field is non-final if identity is verified stable.

A final reference alone is not proof: two instances' private-final monitors do not protect the same static mutable map, and a final lock does not protect accesses that bypass it.

## Critical-section scope

Require a concrete blocking/contended path **and** a safe narrowing. Example: a changed writer holds `stateLock` during a blocking disk write, stalling the UI's `readState()` on that same monitor. Recommend writing after release only if an immutable snapshot is captured under the lock, the file remains owned/alive until completion, the write API independently prevents partial/interleaved writes where required, concurrent write order is permitted by contract, and no guarded invariant depends on write completion. Otherwise preserve the required ordering/invariant; do not propose that split.

Do not recommend moving guarded reads/writes outside a lock or removing synchronization for style. A shorter lock is not automatically safer. Unsafe check-then-act split for shared reservation state:

```java
boolean reserve() {
    final boolean available;
    synchronized (lock) { available = reserved < capacity; }
    if (available) {
        synchronized (lock) { reserved++; }
    }
    return available;
}
```

With `reserved == 0` and `capacity == 1`, T1 and T2 can both pass the check and then reserve, exceeding capacity. Keep the check and increment in one critical section; individually synchronized accesses do not preserve the compound invariant.

Safe narrowing counterexample, assuming concurrent calls may linearize at list insertion:

```kotlin
private class LabelStore {
    private val lock = Any()
    private val labels = mutableListOf<String>()

    fun add(input: String) {
        require(input.length <= 256)
        val normalized = input.trim()
        synchronized(lock) { labels.add(normalized) }
    }
}
```

Moving this bounded, pure normalization before acquisition is safe: each call owns its immutable input/result, computation reads no shared state, and the list mutation stays protected by the original stable monitor. This is a counterexample, not a performance finding by itself. For shared inputs, capture a consistent immutable snapshot under the original lock and verify lifetime, visibility, commit ordering, and any need to revalidate before acting; do not let a mutable alias escape.

## Refactors that lose protection

Compare effective coverage, not just keywords. Before, concurrent callers on the same instance serialize ID allocation:

```java
synchronized int nextId() { return ++id; }
```

Dangerous after: a changed wrapper and helper have lost that monitor, so T1 and T2 can lose updates and return duplicate IDs:

```java
int nextId() { return increment(); }
private int increment() { return ++id; }
```

Safe extraction: retain the entry point's instance monitor for the whole operation, with every other helper caller holding that exact monitor:

```java
synchronized int nextId() { return increment(); }
private int increment() { return ++id; }
```

Do not demand redundant synchronization on this private helper. If it moves to a different object, synchronizing that helper's receiver is not equivalent to holding the original caller's monitor.

Apply the same comparison to dropped Kotlin `@Synchronized`, a changed explicit lock expression, or a call-site-only refactor: a caller bypassing a synchronized wrapper to invoke an unguarded helper loses protection without adding any locking token. A change from `static synchronized` to instance synchronization leaves shared static state under different monitors across instances. Conversely, preserved monitor identity and complete coverage on every relevant path are not defects merely because code moved.

## Atomic map initialization

Bad when concurrent callers share a mutable map without a common lock:

```kotlin
if (cache[key] == null) {
    cache[key] = computeValue()
}
```

`MutableMap.getOrPut` is not synchronized and does not fix this race. The concurrent-map overload can invoke its initializer more than once across competing calls, even when another call installs the value. Verify the actual receiver type/overload. Individually thread-safe map operations do not make a compound invariant atomic.

Good with explicit ownership and initialization constraints:

```kotlin
private class LengthCache {
    private val cacheLock = Any()
    private val cache = mutableMapOf<String, Int>()

    fun lengthFor(key: String): Int = synchronized(cacheLock) {
        cache[key] ?: key.length.also { cache[key] = it }
    }
}
```

Require safe publication of this instance and **all** map reads, writes, removals, and iterations to use `cacheLock`; neither map nor monitor escapes. `key.length` deliberately illustrates bounded, pure, nonblocking, non-null initialization, and the returned `Int` is immutable. Do not substitute network/disk work or move state-dependent initialization out of the lock without preserving the invariant. Check a concrete concurrent-map implementation's contract before proposing an alternative API; do not promise universal initializer execution counts or lifetime guarantees.

(Java volatile double-checked and Kotlin lazy examples are omitted; use the standard safe patterns.)

## Finding evidence

Anchor each finding to a changed line (or the affected call/declaration when locking was removed). Name the actual monitor(s), competing paths/threads, before/after held locks, broken invariant or concrete contention, impact, and minimal safe fix. An unresolved call boundary is an analysis limit, not evidence of deadlock.

For example, on the changed outer acquisition in `rotate`: "`rotate` now holds this instance's `keyLock` while waiting for `cacheLock`; concurrent `refresh` holds the same `cacheLock` while waiting for `keyLock`, so both operations can stop progressing. Acquire `cacheLock` before `keyLock` on both paths." Confirm that ordering also holds in relevant concrete callees.

Use the existing [High/Medium impact criteria](comment-quality-severity.md#severity); never assign blanket severity to nesting, non-final references, or large scopes. Escalate security-relevant races based on their demonstrated impact, not the syntax of the locking construct.

## Suppressions

Do not flag:

- Intentional thread confinement through a clearly enforced single-thread dispatcher/executor.
- Read-only data after construction (effectively immutable).
- Generated code with known synchronization wrappers.
- Consistent lock order across reachable paths or same-monitor JVM reentry without a demonstrated conflicting path.
- Stable private-final/`val` monitors guarding all relevant accesses, or non-final syntax without actual replacement/inconsistent protection.
- Helper extraction preserving the original monitor on all callers.
- Bounded pure computation on thread-owned inputs outside a lock when invariants, visibility, ordering, snapshots, and resource lifetimes remain valid.
