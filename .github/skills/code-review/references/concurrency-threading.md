# Concurrency and threading

## Contents

- [Change-driven routing](#change-driven-routing)
- [Review targets](#review-targets)
- [Lock identity and before/after analysis](#lock-identity-and-beforeafter-analysis)
- [Nested acquisitions](#nested-acquisitions)
- [Calls under locks](#calls-under-locks)
  - [Runtime-target trace](#runtime-target-trace)
  - [Unknown target boundaries](#unknown-target-boundaries)
  - [Invariant-preserving remediation](#invariant-preserving-remediation)
  - [Synthetic paired cases](#synthetic-paired-cases)
- [Monitor stability](#monitor-stability)
- [Critical-section scope](#critical-section-scope)
- [Refactors that lose protection](#refactors-that-lose-protection)
- [Atomic map initialization](#atomic-map-initialization)
- [Finding evidence](#finding-evidence)
- [Suppressions](#suppressions)

Escalate a concurrency issue to security when it can bypass authentication/permission checks, corrupt token validity or cache state, break key rotation, or expose sensitive data.

## Change-driven routing

Perform deeper analysis for changed, added, removed, or moved `synchronized` blocks/methods, Kotlin `@Synchronized`, explicit `Lock`/`withLock`/`Mutex`, and modified lock expressions, helpers, receivers, or shared-state access paths. Include refactors without new locking tokens in the diff, such as moving a guarded mutation into another helper or changing its callers.

Also route changed interface/abstract calls, concrete Java overridable-method calls, and synchronous callback/listener/lambda invocations under a held lock. Follow changed injection, implementation selection, hierarchy, override, or callback bodies back to affected locked callers even when those callers and synchronization keywords are absent from the diff. This is focused routing, not a rule that arbitrary calls under locks are defective.

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

```kotlin
private class LockOrderExample {
    private val cacheLock = Any()
    private val keyLock = Any()
    private var version = 0

    fun refresh() {
        synchronized(cacheLock) {
            synchronized(keyLock) { version++ }
        }
    }

    fun rotate() {
        synchronized(keyLock) {
            synchronized(cacheLock) { version++ }
        }
    }
}
```

ABBA evidence: T1 in `refresh` holds `cacheLock` and waits for `keyLock`; T2 in `rotate` holds `keyLock` and waits for `cacheLock`. Recommend a consistent order across both paths and their concrete callees, not indiscriminate lock removal.

True negatives, absent another conflicting path:

- Both entry points acquire `cacheLock` then `keyLock` on this same instance. Nesting alone is not a defect.
- `synchronized(cacheLock) { synchronized(cacheLock) { version++ } }` reenters the same JVM monitor on one thread. The same applies when a helper reacquires that exact monitor.
- Different instances' locks do not form this ABBA cycle unless aliases or shared callees connect them; verify object identity first.

Do not apply JVM reentry reasoning to `mutex.withLock { mutex.withLock { update() } }`: if that path reacquires the same Kotlin mutex before release, it cannot reenter. Establish the actual path/identity rather than treating every nested locking construct alike.

## Calls under locks

### Runtime-target trace

1. Locate the changed call or implementation/injection/hierarchy/override/callback and the affected locked caller. Resolve the concrete held monitors/locks and compare before/after held-lock sets through invocation and release, including exceptional exits. Apply this to Java/Kotlin `synchronized`/`@Synchronized`, explicit `Lock`/`withLock`, and `Mutex`.
2. Resolve possible runtime targets from constructors, factories, dependency injection, subtype/override relationships, and listener/lambda wiring. Inspect actual implementations, including concrete Java methods that can be overridden. Declared type or method name proves neither safety nor danger; a harmless base body does not describe an injected override. Bound the trace to targets and callers relevant to changed behavior.
3. Follow each resolved callee while the caller's lock remains held. Identify actual additional acquisitions, blocking I/O, waits, and reentrant reads/writes of the caller's invariant. For ABBA, show distinct lock identities and a concurrently reachable reverse path; another lock inside an override alone is insufficient.
4. Distinguish registering/enqueueing a callback from invoking it before release. Inspect executor/dispatcher behavior rather than assuming asynchronous delivery: inline execution is possible. For a deferred worker, check whether the caller waits via `join`, `Future.get`, a latch, or another dependency while holding a lock the worker needs. Those waits do not release monitors; `Object.wait` releases its target monitor only, so other held locks may still participate.
5. For same-thread callbacks, separate benign JVM monitor reentry from actual observation/mutation of incomplete state. Show the partial invariant, concrete callback access, and incorrect result or state. For the same non-reentrant Kotlin `Mutex`, a callback that reacquires it before release can self-suspend or fail with a repeated owner token. Check the actual explicit `Lock` contract rather than borrowing JVM semantics.

### Unknown target boundaries

For unresolved injection or external/native/reflective callees, record the exact boundary and missing contract (for example, dispatch timing, additional locks, worker waits, or access to a partially updated invariant). Do not invent a lock edge, presume a safe implementation/default, or report probable/proven deadlock without that evidence.

Stay silent when the only evidence is a generic interface or unknown method under a lock. At most one clearly non-blocking `Sanity check:` contract note is permitted for a changed risk already established locally, such as newly executing an uncontrolled callback while a shared auth-cache lock is held. State that local fact and ask for the specific missing contract; do not warn about every unknown callee or duplicate an existing order-inversion finding.

### Invariant-preserving remediation

Do not mechanically move an external call outside its guard. Capture an immutable snapshot and complete the owning state transition or reservation under the original lock only when the open call preserves atomicity, visibility, invocation order, thread affinity, resource lifetime, and auth/token/key semantics. Revalidate a generation/state before commit or reconcile completion where required. A mutable alias is not a snapshot.

Revalidation after an irreversible call cannot undo stale authorization, duplicate side effects, or use of an invalidated resource. Establish reservation/ownership, cancellation, and completion ordering before starting such work; do not split check-and-act or add retries that repeat irreversible operations. A verified lock-order contract across the concrete targets can be an alternative. Do not replace compound protection with `volatile` or finalize global APIs merely to prevent overrides.

### Synthetic paired cases

These are synthetic teaching examples, not claims about an actual incident/PR or Common runtime code. Assume safe publication and shared-instance concurrent reachability where specified. A negative applies only under its stated target, state, and lifetime constraints.

Use Kotlin for language-neutral examples, matching the default for new code. Kotlin classes/functions are final by default; preserve intended virtual dispatch with explicit `open`/`override`. Java comparisons remain where virtual-by-default methods or legacy blocking-thread APIs are the teaching point. Translating a JVM monitor example does not mean replacing its monitor with a coroutine `Mutex`.

#### Runtime override: ABBA versus the same target with consistent order

Suppose only factory/implementation selection changed from exact `BaseWork` to `LockingWork`; the interface call and its outer acquisition are unchanged:

```kotlin
private class DispatchExample {
    interface Work { fun run() }
    open class BaseWork : Work {
        override fun run() {}
    }

    private val cacheLock = Any()
    private val keyLock = Any()
    private val work: Work = LockingWork()
    private var version = 0

    private inner class LockingWork : BaseWork() {
        override fun run() {
            synchronized(keyLock) { version++ }
        }
    }

    fun refresh() {
        synchronized(cacheLock) { work.run() }
    }

    fun rotate() {
        synchronized(keyLock) {
            synchronized(cacheLock) { version++ }
        }
    }
}
```

**Positive:** T1 `refresh -> LockingWork.run` holds this instance's `cacheLock` and waits for `keyLock`; T2 `rotate` holds the same `keyLock` and waits for `cacheLock`. Cite the changed target selection/override, unchanged locked call, both bodies, identities, and stalled operations. The base class's concrete overridable method being empty is not proof of the runtime target.

**Negative with the same runtime target:** replace `rotate` with the following, with no other reachable reverse path. Both operations acquire `cacheLock -> keyLock`; do not flag the interface or overridable call:

```kotlin
fun rotate() {
    synchronized(cacheLock) {
        synchronized(keyLock) { version++ }
    }
}
```

Exact `BaseWork` with its verified empty nonblocking body is another negative. Abstract-method dispatch requires the same runtime-target analysis, not an automatic finding.

Compact Java comparison: a non-final instance method is virtual without Kotlin's `open`; the actual injected subclass body still matters. In Kotlin, `open class BaseWork` allows subclassing and its `override fun run()` remains overridable unless explicitly marked `final`.

```java
class JavaBaseWork {
    void run() { }
}
final class JavaOverrideWork extends JavaBaseWork {
    @Override void run() { }
}
```

#### Callback: partial invariant versus completed-state JVM reentry

```kotlin
private class CallbackStateExample {
    private val stateLock = Any()
    private var left = 0
    private var right = 0

    fun balanced(): Boolean = synchronized(stateLock) { left == right }

    fun updatePartial(value: Int, listener: () -> Unit) {
        synchronized(stateLock) {
            left = value
            listener()
            right = value
        }
    }

    fun updateComplete(value: Int, listener: () -> Unit) {
        synchronized(stateLock) {
            left = value
            right = value
            listener()
        }
    }
}
```

**Positive:** with initial `(0, 0)`, `value == 1`, and the actual wired listener reading `balanced()` to choose a result, `updatePartial` reports an unbalanced state. JVM reentry succeeds; the defect is observation of the incomplete `left == right` invariant, not self-deadlock. A changed callback body can introduce this access without changing the locked caller.

**Negative:** `updateComplete` with that same bounded read-only listener observes completed state. Do not flag JVM reentry itself. This is not a blanket guarantee for other listeners that mutate state, acquire another lock, throw before required completion, or wait.

#### Worker wait versus a truly deferred queue with no await

```java
final class WorkerExample {
    private final Object stateLock = new Object();
    private int value;

    int read() {
        synchronized (stateLock) { return value; }
    }

    void notifyAndWait() throws InterruptedException {
        synchronized (stateLock) {
            final Thread worker = new Thread(() -> { read(); });
            worker.start();
            worker.join();
        }
    }

}
```

**Positive (legacy Java thread API):** T1 holds `stateLock` and waits for worker completion in `join`; the worker waits for that exact monitor in `read`. Async execution does not break this wait cycle.

For the deferred negative, use a Kotlin function type rather than Java `Runnable` interop:

```kotlin
private class DeferredExample {
    private val stateLock = Any()
    private val pending = java.util.ArrayDeque<() -> Unit>()
    private var value = 0

    fun read(): Int = synchronized(stateLock) { value }

    fun enqueue() {
        synchronized(stateLock) { pending.add { read(); Unit } }
    }

    fun drainOne() {
        val notification = synchronized(stateLock) { pending.poll() }
        notification?.invoke()
    }
}
```

**Negative:** `enqueue` only stores the lambda; `drainOne` invokes it after releasing its acquisition, and no caller waits under `stateLock`. With all queue accesses guarded and no outer lock held by the draining caller, registration is not invocation-under-lock. This notification intentionally reads current state at delivery; if the contract instead needs event-time state/order, preserve that with a snapshot/ordered delivery rather than assuming equivalence.

The queue is a verified private standard `ArrayDeque`, not an unknown executor that might invoke inline. `Thread.join` intentionally remains Java; coroutine suspension and `Mutex` ownership require their own analysis below.

#### Collection read: verified standard implementation versus unknown custom dispatch

```kotlin
private class CollectionExample {
    private val ownerGuard = Any()
    private val owned: MutableList<String> = java.util.ArrayList()

    fun ownedSize(): Int = synchronized(ownerGuard) { owned.size }

    fun suppliedSize(supplied: List<*>): Int =
        synchronized(ownerGuard) { supplied.size }
}
```

**Negative:** `owned` is an actual non-escaping standard `ArrayList`; every access uses `ownerGuard`. Its bounded `size` read adds no lock/wait edge. This conclusion follows the construction and guard, not the declared `List` name.

Do not generalize this leaf to all read operations: even standard `ArrayList.contains` can invoke the search argument's `equals`. Trace that target/body and any subclass override rather than treating the collection class as a whitelist.

**Unknown:** `suppliedSize` alone does not establish the runtime target or a cycle. Do not call it safe or warn merely because it is an interface call. A verified custom `size` getter taking B plus a reachable B -> this instance's `ownerGuard` path would be a positive ABBA case; inspect that implementation rather than whitelisting `Map`/`List` or inventing the path. For example, this runtime target adds a real edge to `secondLock`, but the reverse path must still be established:

```kotlin
val custom = object : AbstractList<String>() {
    override val size: Int
        get() = synchronized(secondLock) { 0 }

    override fun get(index: Int): String =
        throw IndexOutOfBoundsException(index.toString())
}
```

#### Open call: version revalidation versus an invalidation race

```kotlin
private class OpenCallExample {
    private val stateLock = Any()
    private var generation = 0L
    private var allowed = true
    private var value = 0

    fun invalidate() {
        synchronized(stateLock) {
            allowed = false
            generation++
        }
    }

    fun computeUnsafe(compute: () -> Int): Boolean {
        synchronized(stateLock) {
            if (!allowed) return false
        }
        val result = compute()
        synchronized(stateLock) {
            value = result
            return true
        }
    }

    fun computeValidated(compute: () -> Int): Boolean {
        val observed = synchronized(stateLock) {
            if (!allowed) return false
            generation
        }
        val result = compute()
        synchronized(stateLock) {
            if (!allowed || generation != observed) return false
            value = result
            return true
        }
    }
}
```

Assume the verified computation uses immutable thread-owned input, produces a discardable value, has no irreversible business side effects or invalidatable resource, and may complete in either order across callers. All state accesses remain under `stateLock`.

**Positive:** a mechanical move leading to `computeUnsafe` lets T2 invalidate after T1's check but before T1's commit. T1 still writes `value` and returns success while `allowed == false`. Keeping individual accesses synchronized did not preserve the invariant.

**Negative:** `computeValidated` rejects the stale result under the original guard; `invalidate` changes permission/generation atomically. This is a safe open call under the stated constraints, not a fix for arbitrary auth/network/key operations or ordered callbacks. If the computation performs an irreversible operation, revalidation afterward is too late; require a proven reservation/lifetime protocol instead.

#### Mutex callback: reacquisition versus invocation after release

This Kotlin example owns one `kotlinx.coroutines.sync.Mutex`, guards mutable `value` with that mutex, and assumes a suspending caller with a callback wired to `write()`:

```kotlin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private class MutexCallbackExample {
    private val mutex = Mutex()
    private var value = 0

    suspend fun write() = mutex.withLock { value++ }
    suspend fun invokeLocked(callback: suspend () -> Unit) = mutex.withLock { callback() }
}
```

**Positive:** `invokeLocked { write() }` reacquires that same non-reentrant mutex before the outer release and self-suspends (or fails with a repeated owner token). Cite both the callback wiring and the concrete mutex identity, not just nested syntax.

**Negative:** a verified invocation of `write()` after the outer release, with no outer caller waiting while holding that mutex and with state/lifetime/order preserved, has no reacquisition-under-lock cycle. Do not recommend that move unless those constraints actually hold.

## Monitor stability

Flag replacement only when it can split protection for the same shared state. Example shared object:

```kotlin
private class MonitorExample {
    @Volatile private var lock: Any = Any()
    private var count = 0

    fun increment() {
        synchronized(lock) { count++ }
    }

    fun replaceLock() {
        lock = Any()
    }
}
```

T1 can hold the old monitor while another thread replaces `lock` and T2 enters on the new monitor. Both increments now access `count` without a common exclusion/visibility boundary. `volatile` makes replacement visible, not mutually exclusive with holders of the old object.

Safe counterexample: remove replacement, use `private val lock = Any()` (Java: `private final Object lock = new Object()`), safely publish the owning object, and guard every relevant state access with that one stable monitor. Do not request `val`/`final` just because a monitor field is mutable/non-final if identity is verified stable.

A final reference alone is not proof: two instances' private-final monitors do not protect the same static mutable map, and a final lock does not protect accesses that bypass it.

## Critical-section scope

Require a concrete blocking/contended path **and** a safe narrowing. Example: a changed writer holds `stateLock` during a blocking disk write, stalling the UI's `readState()` on that same monitor. Recommend writing after release only if an immutable snapshot is captured under the lock, the file remains owned/alive until completion, the write API independently prevents partial/interleaved writes where required, concurrent write order is permitted by contract, and no guarded invariant depends on write completion. Otherwise preserve the required ordering/invariant; do not propose that split.

Do not recommend moving guarded reads/writes outside a lock or removing synchronization for style. A shorter lock is not automatically safer. Unsafe check-then-act split for shared reservation state:

```kotlin
private class ReservationExample {
    private val lock = Any()
    private val capacity = 1
    private var reserved = 0

    fun reserve(): Boolean {
        val available = synchronized(lock) { reserved < capacity }
        if (available) {
            synchronized(lock) { reserved++ }
        }
        return available
    }
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

```kotlin
@Synchronized fun nextId(): Int = ++id
```

Here `id` is a mutable instance field and `@Synchronized` holds that instance's JVM monitor (`this`), not a coroutine mutex. Dangerous after: a changed wrapper and helper have lost that monitor, so T1 and T2 can lose updates and return duplicate IDs:

```kotlin
fun nextId(): Int = increment()
private fun increment(): Int = ++id
```

Safe extraction: retain the entry point's instance monitor for the whole operation, with every other helper caller holding that exact monitor:

```kotlin
@Synchronized fun nextId(): Int = increment()
private fun increment(): Int = ++id
```

Brief Java comparison: the instance method's `synchronized` modifier holds the same `this` monitor; removing it from an unguarded wrapper loses the same protection.

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

Anchor each finding to a changed line (or the affected call/declaration when locking was removed). For changed implementation/injection/override/callback bodies, cite that changed line and the affected locked call. Name the actual monitor(s), runtime callee/callback, competing paths/threads or reentrant invariant access, before/after held locks, broken invariant or concrete contention, impact, and minimal safe fix. An unresolved call boundary is an analysis limit, not evidence of deadlock; apply the bounded [unknown-target rule](#unknown-target-boundaries).

For example, on the changed outer acquisition in `rotate`: "`rotate` now holds this instance's `keyLock` while waiting for `cacheLock`; concurrent `refresh` holds the same `cacheLock` while waiting for `keyLock`, so both operations can stop progressing. Acquire `cacheLock` before `keyLock` on both paths." Confirm that ordering also holds in relevant concrete callees.

Use the existing [High/Medium impact criteria](comment-quality-severity.md#severity); never assign blanket severity to nesting, non-final references, large scopes, or interface/abstract/overridable dispatch. An interface call is not automatically High severity or a merge blocker. Escalate security-relevant races based on their demonstrated impact, not the syntax of the locking construct.

## Suppressions

Do not flag:

- Intentional thread confinement through a clearly enforced single-thread dispatcher/executor.
- Read-only data after construction (effectively immutable).
- Generated code with known synchronization wrappers.
- Consistent lock order across reachable paths or same-monitor JVM reentry without a demonstrated conflicting path or invalid intermediate-state access.
- Stable private-final/`val` monitors guarding all relevant accesses, or non-final syntax without actual replacement/inconsistent protection.
- Helper extraction preserving the original monitor on all callers.
- Bounded pure computation on thread-owned inputs outside a lock when invariants, visibility, ordering, snapshots, and resource lifetimes remain valid.
- Known bounded, pure, nonblocking leaves and verified nonblocking standard-collection reads under their owner guard with no user-code dispatch; verify actual runtime targets instead of whitelisting `List`/`Map` by name.
- Truly deferred callbacks invoked after release with no caller wait, with valid state, ordering, and lifetime.
- Generic interface/abstract/overridable calls or unknown external targets without concrete local risk evidence.

Confinement, immutability, and generated-wrapper suppressions require the changed path to retain those boundaries; do not suppress a new override/callback bypass or a concrete reentrant access to partial state.
