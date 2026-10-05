# Copilot PR Review & Domain Instructions (Common Android Auth Library)

These instructions guide AI (and human) PR reviews and code suggestions for the Common library (`AzureAD/microsoft-authentication-library-common-for-android`).  
This file is self-contained; it includes full baseline & appendices replicated from the Broker guidelines plus deeper Common-specific detail. Changes here cascade to MSAL and Broker—review with elevated rigor. ALWAYS tailor feedback to changed code.

--------------------------------------------------------------------------------

## 0. Basic Code Review Guidelines (Enforce Consistently)
- Treat each file according to its language; never mix Java and Kotlin keywords (e.g., never produce `val final`).
- Review changed code + necessary local context; do not deep-audit untouched legacy unless new change introduces or depends on a severe risk there.
- Aggregate related minor issues only when SAME contiguous snippet/function + shared remediation.
- Each comment MUST contain: Issue, Impact (why it matters), Recommendation (actionable). Provide patch suggestions for straightforward, safe fixes.
- Replacement code must compile, preserve imports/annotations/license headers, and not weaken security, nullability, synchronization.
- Do not invent unstated domain policy; if assumption needed: “Assumption: … If incorrect, disregard.”
- Do not nitpick tool-managed formatting (ktlint/Spotless/etc.).
- Avoid flagging unchanged legacy code unless the PR’s change now interacts with it in a risky way.

--------------------------------------------------------------------------------

## 1. Domain & Architecture Primer (Common Context)

### 1.1 High-Level Purpose
Common provides cross-repository primitives:
- Command architecture (TokenCommand, BrokerCommand, controllers).
- OAuth2/OIDC protocol request/response handling.
- Token cache, serialization, normalization (authority, environment, FOCI).
- Cryptography utilities (KeyStore, key wrapping, hashing, JWE/JWS support).
- Telemetry enums (SpanName, AttributeName) and instrumentation helpers.
- IPC contracts & shared data models.
- Cloud instance & regional authority discovery and validation.
- Error taxonomy & mapping.
- Utilities (Clock, RNG abstraction, JSON adapters, correlation ids).
- Potential native code for performance/secure handling.

### 1.2 Architectural Layers
1. Public Facade (parameters, builders).
2. Command Orchestrator (controller selection).
3. Controllers (cache, broker, network).
4. Protocol Layer (request construction, response parsing).
5. Cache Layer (multi-artifact atomic updates).
6. Crypto Layer (secure operations).
7. Telemetry Layer (spans, attributes, privacy classification).
8. IPC / Serialization (bundle schemas, version negotiation).
9. Utilities (time, URL, JSON).
10. Error Mapping Layer (raw → domain exceptions).

### 1.3 Command Execution Lifecycle
1. Build parameters.
2. Preflight validation (authority, scopes, claims).
3. Controller resolution (strategy chain).
4. Span start (`SpanName.CommandExecution`) with correlation_id.
5. Execution (cache check → refresh/network/broker).
6. Response integrity checks (claims presence, flags, algorithms).
7. Atomic cache write (AT/RT/ID token & metadata).
8. Result adaptation (DTO or exception).
9. Span finalization (status set, exception recorded, `span.end()` in finally).

### 1.4 Token Artifacts
- Access Token (short-lived; scope-limited).
- Refresh Token (app or family).
- ID Token (identity claims; never logged in raw form).
- Device/PRT artifacts (managed via Broker; only referenced logically).
- Derived/session keys (ephemeral cryptographic context; minimize retention).

### 1.5 Cache Model & Atomicity
- Dimensions: environment, client_id, home_account_id, tenant_id (normalized).
- FOCI fallback: family RT if app-specific RT absent.
- Atomic multi-artifact writes (avoid partial update).
- Authority canonicalization mandatory pre-keying.
- Eviction only when new artifact supersedes previous validity window.

### 1.6 Authority & Instance Discovery
- Host validated against discovery metadata.
- Regional endpoints: secure fallback path.
- Metadata caching (avoid repeated network for identical authority).
- Schema changes require migration gating.

### 1.7 IPC Schema Compatibility
- Key constants stable; additive changes maintain backward read of old keys.
- Protocol/schema/version fields preserved semantically.
- Removal/rename of existing keys without fallback = Severity: High.

### 1.8 Cryptography Details
- Approved: SHA-256/512, AES-GCM with random IV, RSA OAEP or ECDSA P-256 (where present).
- No static IV/nonce reuse; detect repeated constant patterns.
- Use `SecureRandom`.
- No plaintext private keys in SharedPreferences; KeyStore usage required.
- Key rotation atomic: old key decommission only after new key validated.

### 1.9 Telemetry Enums
- Adding attribute: uniqueness, bounded cardinality, doc comment specifying value domain & units.
- Reuse existing SpanName for similar semantics; avoid duplication.

### 1.10 Error Taxonomy
- Service: protocol-level (invalid_grant, interaction_required).
- Client: config, parsing, network unreachable.
- UI Required: flows that need interaction escalation.
- Crypto: key retrieval/generation issues.
- Avoid flattening distinct service errors.

### 1.11 Migration & Versioning Guardrails
- Enums additive; renaming/removal demands migration doc.
- Cache schema evolution introduces version field + fallback read path.
- IPC additions maintain existing keys; annotate deprecated keys.
- Breaking changes documented with MAJOR classification & migration steps.

### 1.12 High-Impact Diff Triggers
Severity: High – candidates:
- Logging raw tokens/claims/keys.
- Inline telemetry keys bypassing enums.
- Cache/IPCs key removal without backward support.
- Static IV/nonce reuse.
- Authority validation bypass.
- Token write race causing partial/inconsistent state.
- Span not ended due to early returns or missing finally.
- Crypto verification disabled or conditional short-circuit.

Severity: Medium – examples:
- Loss of specific error mapping.
- Non-atomic multi-artifact write.
- Repeated network authority discovery calls.
- Missing correlation_id propagation.

### 1.13 Generation Guidance for Copilot
- Extend existing abstraction (new Command/Controller) vs ad-hoc branching.
- Avoid duplicating crypto primitives; reuse wrappers.
- Document threading assumptions.
- Keep secret-bearing data ephemeral; clear buffers when feasible.

### 1.14 Related Repositories & Reuse Guidance
(Explicitly listing consumers and reuse expectations for clarity.)

- `AzureAD/microsoft-authentication-library-for-android` (MSAL): Relies on Common for command pipeline, token parsing, cache abstractions, telemetry enums, authority discovery, error taxonomy. MSAL should not redefine telemetry keys, token response parsing, or introduce divergent cache key normalization logic—reuse Common.
- `AzureAD/ad-accounts-for-android` (Broker): Consumes Common IPC contracts, telemetry enums, crypto utilities, and shared token/cache models. Broker-specific logic (PRT rotation, WPJ) must still use Common-provided enumerations and attribute classification; adding telemetry must extend Common enums rather than inline new string keys in Broker.
- Test / Automation Apps (e.g., internal sample/testapps in MSAL/Broker repos): Depend on stable command & controller semantics; changes in Common that modify command result structures require coordinated updates.
- Future Platform Integrations (Linux Broker, cross-platform adapters): Should rely on Common’s canonical authority normalization and cache schema for consistency; new platform-specific code must not fork token parsing logic—extend via abstraction points (e.g., platform crypto interfaces).
  Reuse Mandates:
- Telemetry: Add new `AttributeName` / `SpanName` here first before usage downstream.
- Cache Schema: Any structural changes originate in Common with backward compatibility; MSAL/Broker must adapt not redefine.
- Error Types: Expand Common exception hierarchy rather than creating parallel, similar exceptions in downstream repos.
  Policy:
- Downstream repos must not inline new IPC key strings; propose addition here for centralization and classification first.
  Rationale:
- Centralization ensures single authoritative source for privacy classification, migration strategies, and cross-repo compatibility.

--------------------------------------------------------------------------------

## 2. Security (Umbrella)

Flag:
- Secrets/tokens/keys/PII exposure (logs, telemetry attributes, exceptions).
- Insecure authn/authz, exported Android components, weak permission checks.
- Crypto misuse (see 1.1).
- Input validation gaps (IPC, intents, network, file, deserialization).
- Race/TOCTOU affecting authorization, token issuance, key usage.
- Feature flag misuse enabling partial insecure paths.
- Improper error handling that leaks sensitive internals.

Only consolidate if same snippet/function and single remediation. Prefix severe items with `Severity: High –`.

### 2.1 Cryptography & Key Management
Flag:
- Weak/deprecated algorithms (MD5, SHA1, RSA PKCS#1 v1.5 unless mandated, ECB, static salts).
- Hard-coded/reused IVs/nonces (AEAD modes like GCM).
- Logging keys, secrets, token contents.
- Missing null/error handling retrieving keys.
- Non-secure RNG (`Random()`) for crypto; require `SecureRandom`.
- Inadequate key rotation or unchecked expired cert chains.

### 2.2 Logging, Privacy & PII
Never allow:
- Raw secrets/tokens/private keys/full identifiers in logs/telemetry.
- Full stack traces for expected validation failures (recommend redaction or summarizing).
- High-cardinality sensitive values as attributes (hash or bucket).

### 2.3 Feature Flags / Flighting (Security Impact)
Check safe defaults; flag recorded securely; no code executes insecure branch before safe evaluation.

--------------------------------------------------------------------------------

## 3. Concurrency & Thread Safety (Security Intersection Where Applicable)
Escalate to Security if a race compromises auth, tokens, or sensitive data integrity.

Route changed, added, removed, or moved `synchronized` blocks/methods, Kotlin `@Synchronized`, explicit `Lock`/`withLock`/`Mutex`, and changes to lock expressions, helpers, receivers, or shared-state access paths to deeper concurrency analysis. Include refactors whose diff has no new locking tokens. The presence of synchronization elsewhere in a file is not a finding or a reason to audit unrelated code.

Also route changed interface/abstract calls, concrete Java overridable-method calls, and synchronous callback/listener/lambda invocations while a lock is held. Include changes to injected implementations, hierarchies, overrides, or callback bodies reached by an unchanged locked caller, even without synchronization keywords in the diff. This is focused routing, not a blanket arbitrary-call-under-lock defect.

### 3.1 What to Flag (Non-Security)
- Unsynchronized mutable shared state accessed across threads/coroutine contexts (lists/maps/caches/flags).
- Lazy init races (double-checked locking lacking `volatile` / `@Volatile`).
- Visibility issues: write background → read UI without memory barrier.
- TOCTOU on permissions/files after suspension or I/O latency.
- Long/blocking operations on main/UI thread.
- Unbounded parallel launches (`repeat(1000) { launch { ... } }`) without throttling/back-pressure.
- Missing cancellation propagation (manual threads, `GlobalScope`).
- Resource closing without `try/finally`, risking leaks on cancellation.
- Flow misuse: redundant multiple cold Flow collections causing repeated expensive work; State semantics using wrong Flow type.
- Executor oversubscription (new Executor per call).
- Unsafe publication (object fully constructed but not safely published).
- Catching `CancellationException` and not rethrowing (hides cancellation).

### 3.2 Security-Relevant Concurrency
- Races bypassing auth/permission checks.
- Token invalidation/refresh race windows.
- Key rotation concurrency issues.
- Data exposure via inconsistent logging state.

### 3.3 Patterns & Fixes
Bad when callers share a mutable map without a common lock (data race and non-atomic check-then-act):
```kotlin
if (cache[key] == null) {
    cache[key] = computeValue()
}
```
`MutableMap.getOrPut` is not synchronized and does not fix this race. The concurrent-map overload can invoke its initializer more than once across competing calls, even when another call installs the value. Verify the receiver's actual type and overload. Individually thread-safe map operations do not make a compound invariant atomic.

Good under these explicit constraints: a safely published instance owns the map and one stable lock; **all** reads, writes, removals, and iterations use that lock, and the map does not escape. The illustrative initializer `key.length` is bounded, pure, nonblocking, and non-null; the returned `Int` is immutable.
```kotlin
private class LengthCache {
    private val cacheLock = Any()
    private val cache = mutableMapOf<String, Int>()

    fun lengthFor(key: String): Int = synchronized(cacheLock) {
        cache[key] ?: key.length.also { cache[key] = it }
    }
}
```
Do not replace this initializer with blocking work or move a state-dependent initialization outside the lock without preserving the invariant. Check the concrete concurrent-map implementation's contract before suggesting an alternative API.

(Java volatile double-checked & Kotlin lazy examples omitted here for brevity; use standard safe patterns.)

### 3.4 Coroutine Best Practices
- Prefer structured concurrency (`coroutineScope`, `supervisorScope`).
- Avoid `GlobalScope`; use lifecycle/viewModel/injected scopes.
- Use `withContext(Dispatchers.IO)` for blocking I/O (not inside tight hot loops repeatedly switching contexts).
- Check `isActive` in large iteration chunks.
- Avoid spin-waits; prefer Channels, Mutex, or Semaphores.

### 3.5 Synchronization Heuristics
- Atomic for simple counters/flags.
- Mutex/synchronized for compound operations.
- Immutable snapshot replace for infrequently updated shared structures.

### 3.6 Concurrency Related Annotations
Suggest adding annotations:
- `@MainThread`, `@AnyThread`, `@WorkerThread` for concurrency clarity.
- `@GuardedBy("lock")` for guarded fields.
- `@Volatile` for fields with independent readers/writers without full synchronization.

### 3.7 False Positives
Do NOT flag:
- Intentional thread confinement (single-thread dispatcher/executor) clearly enforced.
- Read-only data after construction (effectively immutable).
- Generated code with known synchronization wrappers.
- Consistent acquisition order across reachable paths, or same-monitor JVM reentry, without a demonstrated conflicting path or invalid intermediate-state access.
- Stable private `final`/`val` monitors used by all relevant accesses; non-final syntax alone is not a concurrency defect.
- Helper extraction retaining the same monitor for every caller, or bounded pure computation on thread-owned inputs moved outside a lock while preserving the protected invariant and ordering.

These suppressions require the changed path to preserve confinement, immutability, or the generated wrapper's protection. A new override/callback that bypasses that boundary is not suppressed.

### 3.8 Lock Identity and Before/After Analysis
- Trace the actual monitor object and aliases, not just lock variable names: an instance synchronized method locks `this`, while a static synchronized method locks the declaring class's `Class` object. Resolve the actual JVM owner for Kotlin `@Synchronized`, including object/companion and static bridges.
- Compare before/after held-lock sets at each affected shared read/write and call boundary. Trace relevant caller/callee acquisitions, release paths, receiver changes, state ownership, visibility, and concurrent reachability. Follow concrete callees to establish lock order, not to speculate about unknown implementations; state any unresolved call/alias boundary and do not claim a proven deadlock beyond it.
- Distinguish JVM monitor reentry on the same thread from non-reentrant Kotlin `Mutex`: reacquiring a held mutex before release can self-suspend, or fail for a repeated owner token. For explicit `Lock`, check its actual reentrancy contract and paired release (`finally`/`withLock`); `synchronized(lock)` does not acquire the lock used by `lock.lock()`.
- Nested locking is not automatically deadlock. For an order-inversion finding, show concurrently reachable paths on the same two distinct locks: T1 holds A and waits for B while T2 holds B and waits for A (ABBA). Both paths using A then B, or reentering A on one thread, are counterexamples unless another concrete conflicting path exists.

### 3.9 Monitor Stability
- Flag actual concurrent reference replacement or inconsistent protection, not a missing `final`/`val` alone. Example: T1 holds the old object from `synchronized(lock)`, a concurrent assignment replaces `lock`, and T2 enters on the new object to modify the same state. Even `volatile` replacement does not make those monitors identical.
- Prefer one stable private monitor with all relevant accesses guarded by it. A final reference alone is not proof of safety: different instances' final locks do not protect shared static state, and unguarded accesses still race. A non-final field with verified stable identity and consistent protection is not a finding.

### 3.10 Critical-Section Scope
- Flag broad/unnecessary critical sections only with a concrete blocking or contention path and a safe narrowing that preserves invariants, atomicity, visibility, ordering, snapshot consistency, and resource lifetime. Do not remove a lock for style or move guarded accesses outside it.
- Example: a blocking disk write under `stateLock` stalls a UI reader acquiring that same lock. Writing after release is safe only if the data is an immutable snapshot captured under the lock, the resource remains valid, the write API independently prevents partial/interleaved writes where required, write completion is not part of the guarded invariant, and the contract permits the resulting write order.
- Keep check-and-act atomic: checking `reserved < capacity` under one acquisition and incrementing under a later acquisition can let two callers exceed capacity, even though each access is synchronized.
- Safe counterexample: compute a bounded, pure value from immutable thread-owned input before locking, then update guarded state under the original monitor. Do not extrapolate this to shared mutable inputs, escaping snapshots, side effects, or operations whose order/lifetime requires the lock.

### 3.11 Refactors and Finding Evidence
- Compare protection before and after a refactor, including a removed `synchronized`/`@Synchronized`, class-to-instance monitor change, different receiver, or helper extraction. Replacing the original caller's monitor with a helper's own monitor does not preserve the original protection.
- Dangerous example: `synchronized nextId()` becomes an unsynchronized wrapper calling an unguarded increment helper; concurrent calls on the same instance can lose updates. Changing a static synchronized method to instance synchronization while retaining shared static state likewise splits protection across instances.
- Safe counterexample: the original synchronized entry point calls a private helper under the same monitor, and every other helper caller holds that same monitor across the entire invariant. Do not demand redundant synchronization on the helper.
- Cite a changed line (or the affected call/declaration for removed locking), the concrete monitor identity, conflicting paths/threads, before/after protection, actual impact, and minimal invariant-preserving fix. Apply the existing High/Medium impact criteria; never assign blanket severity based on nested locks, non-final references, or scope size.

### 3.12 Runtime Call Targets and Callbacks Under Locks
- Resolve the concrete held monitors/locks and possible runtime targets through construction, injection, hierarchy, overrides, and callback/lambda wiring. Trace actual callee bodies while the lock remains held, including further acquisitions, blocking/wait dependencies, and access to the caller's invariant. A declared interface/abstract type, a non-final Java method, or a collection name proves neither safety nor a defect.
- Distinguish registration/enqueueing from invocation before release. Verify the dispatcher/executor's behavior: an executor may run inline. An asynchronous callback is not safe merely because it runs elsewhere if the caller waits for it while holding a lock it needs. `join`/`Future.get`/latch waits do not release held monitors; `Object.wait` releases only its target monitor, not other held locks.
- Report runtime-target ABBA only with an actual override/callee taking a second distinct lock and a concurrently reachable reverse path on those same objects. Report a waiting-worker cycle only with the caller's wait and the worker's dependency on the held lock. Reacquiring the same non-reentrant `Mutex` through a callback can self-suspend or fail for a repeated owner token.
- Same-thread JVM monitor reentry itself is benign, but a concrete callback can observe or mutate invalid intermediate state. Name the incomplete invariant, the actual callback access, and the resulting incorrect behavior; complete-state reentry without conflicting accesses is a counterexample.
- For external/native/reflective targets or unresolved injection, stop at the exact missing implementation/contract; do not invent lock edges, assert a safe default, or call it probable/proven deadlock. At most one clearly non-blocking `Sanity check:` contract note is permitted for a changed risk already established locally, such as new uncontrolled callback execution while a shared auth-cache lock is held. Specify the missing contract and local consequence; a generic interface call alone warrants silence. Do not duplicate an existing lock-order finding.
- Suppress known bounded, pure, nonblocking leaves and verified targets. For example, an actual non-escaping standard `ArrayList`'s `size()` or indexed `get()` under its owner guard adds no lock/wait edge; still trace operations invoking element/callback code, such as `contains` calling `equals`. A declared `List`/`Map` can instead dispatch to custom code. Truly deferred callbacks after release with no caller wait are counterexamples when state, ordering, and lifetime remain valid.
- Recommend a snapshot/open call only when state transition or reservation remains atomic under the original guard, the snapshot is immutable, and callback ordering, thread affinity, visibility, resource lifetime, and auth semantics are preserved. Revalidate generation/state or reconcile results after the call where needed. Revalidation alone cannot undo an irreversible operation: establish reservation/ownership and cancellation semantics before starting it; do not introduce duplicate side effects, stale-token/key use, check-then-act splits, or unsafe `volatile` substitutions. A verified consistent lock-order contract can be an alternative; do not mechanically finalize global APIs.
- Cite the changed call or implementation/injection/override/callback line plus the held monitor, actual runtime callee, competing path or invariant access, and concrete impact. Use existing severity criteria; an interface call is not automatically High severity or a merge blocker.

See [concurrency examples and counterexamples](skills/code-review/references/concurrency-threading.md) for the corresponding review procedure and compact cases.

--------------------------------------------------------------------------------

## 4. Code Correctness & Business Logic
### 4.1 Common Pitfalls
- Null handling (platform types).
- Exception swallowing / overly broad `catch (Exception)`.
- Boolean/precedence logic errors.
- Java string comparison via `==` vs `.equals`.
- Kotlin `===` vs `==` misuse.
- Unnecessary `!!` instead of safe call + early return.
- Non-exhaustive `when` over sealed types.
- Returning internal mutable collections (expose copy or unmodifiable).
- Shadowing causing misuse.
- Time unit confusion (seconds vs ms).
- Default charset reliance; specify UTF-8.
- Incomplete validation of token response fields (e.g., ignoring missing ID token when protocol requires it).
- Cache lookups failing to normalize authority/tenant causing duplicate entries.
- Silent path erroneously triggering interactive UI fallback.

### 4.2 Validation & Precondition Patterns
Use `require`, `check`, early returns, guard clauses for clarity and fail-fast.

### 4.3 Error Modeling
- Prefer sealed result or domain error types vs magic strings.
- Map external exceptions to domain-specific forms.

### 4.4 Collections & Data Structures
- Use sets/maps for membership in loops.
- Avoid recomputing invariant derived values repeatedly in hot paths.

### 4.5 Equality & Hash
- Overridden `equals` must pair with `hashCode`.
- Use `contentEquals` for arrays.

### 4.6 Serialization / JSON
- Validate required fields; fail explicitly on missing or malformed input.

### 4.7 Defensive Copies
Return read-only or copies of internal mutable structures.

### 4.8 Immutability
When to Suggest:
- Local variable never reassigned → `val` (Kotlin) / `final` (Java).
- Fields set once in constructor → `final`.
  Skip:
- Performance trade-off where lazy mutation is deliberate.
- Variables mutated inside loop with intention (explain if questionable).

Java:
- Recommend `final` for local variables, fields, and method parameters that are not reassigned.

Kotlin:
- Recommend `val` instead of `var` when the reference is not reassigned.
- NEVER suggest adding the Java keyword `final` to a Kotlin local variable or property declaration. A Kotlin `val` already implies an immutable reference.
- Do NOT output or recommend the invalid combination `val final`.
- Only mention `final` in Kotlin if:
  * You are explicitly preventing further overrides on an overriding declaration (e.g., `final override fun foo()`) AND there is a concrete reason (security, correctness, or documented design) to forbid further extension.
  * Otherwise, omit: Kotlin declarations are `final` by default.
- Do NOT suggest converting a `var` to `val` when the code clearly reassigns it or when reassignment is an intentional part of a loop, accumulator, or builder pattern.

### 4.9 Annotations:
- Ensure non-private method params and fields have proper `@NonNull` / `@Nullable` for Java files
- For Kotlin files ensure proper Kotlin nullability.
- Only comment on code touched by the PR.
- Never suggest adding `@NonNull` to a Kotlin property or parameter, as Kotlin already enforces nullability at the type level.

--------------------------------------------------------------------------------

## 5. Performance

Hot Paths:
- Cache operations & serialization.
- Authority discovery & reuse.
- Token response parsing.
- Cryptographic operations.
- Telemetry emission (avoid attribute churn).

Red Flags:
- Repeated regex compilation or reflection.
- Large temporary buffer allocations inside loops.
- Re-deriving stable normalization values each call.
- Unbounded parallel command launches.

Memory:
- Reuse buffers; zero out secret arrays when feasible.
- Avoid leaking large ephemeral collections.

Instrumentation:
- Add or reuse meaningful spans (do not proliferate trivial micro-spans).
- Numeric attributes for duration rather than new spans for small code blocks.

--------------------------------------------------------------------------------

## 6. Telemetry & Observability

### 6.1 Core Principles
- Every new span name MUST be added to `SpanName` enum (central discoverability & consistency).
- Every attribute key MUST be one of the constants in the appropriate `AttributeName` enum (do not inline string keys).
- If a needed attribute does not exist, add it with proper data classification (privacy compliance) before use.
- Use `OTelUtility.createSpan(SpanName.<NAME>.name())` for span construction.
- Use `SpanExtension.makeCurrentSpan(span)` (NOT `span.makeCurrent()` directly) to avoid platform method issues and safely obtain a Scope.
- Always terminate spans (`span.end()`) in a `finally` block.
- Record exceptions with `span.recordException(t)` and set `StatusCode.ERROR`; set `StatusCode.OK` on success.
- Avoid trivial spans (very small/local operations that add noise without diagnostic value).
- Use constant-time attribute naming style already present (snake_case or lowerCamel as established by existing enum).
- Do NOT log/emit high-cardinality sensitive contents (raw tokens, full JSON claims) as attributes; hash, bucket, or omit.
- Use `SpanExtension.current()` to access the current span safely, avoiding direct calls to `Span.current()` which may not be compatible with older devices.
- Before adding new AttributeName: confirm classification and no overlap with existing semantics.
- Ensure correlation_id is set early and consistently for all major spans.
- Avoid micro-spans for trivial getters in token assembly path.

### 6.2 When to Create a New Span
Create a span for:
Cross-process/network boundaries, major asynchronous boundaries, performance-critical operations, key generation, token acquisition phases.

Don't create spans for:
- Small local operations (e.g., simple getters, setters, or trivial computations).
- Helper methods that do not cross significant boundaries or are not performance-critical.
- Operations that are already covered by existing spans (reuse existing span names).

### 6.3 Attribute Usage Rules
- Set attributes ONLY from enums (e.g., `span.setAttribute(AttributeName.ipc_strategy.name(), strategy.getType().name());`).
- If attribute value is optional, either omit or set only when present (avoid empty strings).
- For booleans use primitive boolean, not string "true"/"false".
- For counts/sizes use numeric attributes, not stringified numbers.
- For timestamps where a dedicated DateTime attribute is defined (marked `isDateTime`), ensure value units match expected convention (typically epoch millis).

### 6.4 Adding a New Span Name
Before adding:
- Confirm no existing `SpanName` adequately describes the operation.
- Choose concise, action-oriented name (e.g., `KeyPairGeneration` already exists; reuse rather than duplicating).
- Insert into `SpanName` enum; keep naming consistent with existing PascalCase.
- Use `.name()` when creating span to ensure continuity with existing enumeration pattern.

### 6.5 Adding a New Attribute
Checklist:
1. Does an existing `AttributeName` already cover this semantic? If yes, reuse.
2. Is the value stable, low/controlled cardinality, and privacy-compliant?
3. Each attribute added to `AttributeName.java` in Common repo MUST also be defined in the `AttributeName.java` file in the broker repo (AzureAD/ad-accounts-for-android) for cross-repo consistency. (Leave a comment reminding to do so.)
4. For times/durations: prefer separate numeric metrics (ms) or mark `isDateTime=true` when representing an instant.
5. Add Javadoc describing purpose and (if applicable) expected value set.
6. Update any downstream dashboards or processing rules if necessary.

### 6.6 Span Implementation Pattern (Java Example)
Pattern (mirrors usage in `BrokerOperationExecutor`):
```java
final Span span = OTelUtility.createSpan(SpanName.MSAL_PerformIpcStrategy.name());
try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
    span.setAttribute(AttributeName.ipc_strategy.name(), strategy.getType().name());
    span.setAttribute(AttributeName.broker_operation.name(), operation.getMethodName());
    // Perform operation logic...
    span.setStatus(StatusCode.OK);
    return result;
} catch (final Throwable t) {
    span.setStatus(StatusCode.ERROR);
    span.recordException(t);
    throw t;
} finally {
    span.end();
}
```

### 6.7 Error & Status Handling
- Success path: set `StatusCode.OK` near the end (just before returning) after all attributes set.
- Failure path: set `StatusCode.ERROR` and `recordException(throwable)` BEFORE rethrowing.
- Do not swallow exceptions purely to mark status; rethrow so calling layers can handle.
- Never leave a span open (no early `return` inside try without reaching `finally`).

### 6.8 Anti-Patterns (Flag These)
- Inline string keys (e.g., `span.setAttribute("ipcStrategy", ...)`) instead of `AttributeName.ipc_strategy`.
- Using `Span.current()` directly (risk on older devices) instead of `SpanExtension.current()` or `SpanExtension.makeCurrentSpan`.
- Creating nested micro-spans for every small helper method.
- Setting raw sensitive values (tokens, user principal names) instead of appropriately classified or redacted forms.
- Forgetting to call `span.end()` in a finally.
- Duplicating full stack trace in multiple sinks (logs + attribute). Keep one standardized error attribute; avoid redundant full stack trace duplication.

### 6.9 Allowed vs Disallowed Example
Allowed:
```java
span.setAttribute(AttributeName.http_status_code.name(), response.getCode());
```
Disallowed:
```java
span.setAttribute("httpStatus", response.getCode()); // Not using enum key
span.setAttribute(AttributeName.access_token.name(), rawAccessToken); // Sensitive secret
```

--------------------------------------------------------------------------------

## 7. Testing
### 7.1 Missing Test Heuristics
Flag when new code:
- Introduces conditional branches (if/when/switch) lacking both positive & negative coverage.
- Handles error paths with retries or fallback logic untested.
- Adds parsing/serialization logic without malformed input tests.
- Adds concurrency primitives (Mutex, atomic operations) without race / cancellation tests.
- Adds feature flag branching without tests for each state.
- Adds new public API methods without tests for expected behavior.

Do NOT flag (see §14 rule 3): code whose only new behavior is attaching attributes/values to telemetry spans, the bare `recordException(...)` attachment call itself, or populating an instrumentation field. These attachment paths are considered low-risk and a missing test for them is not a defect.

### 7.2 Test Types & Expectations
- Unit tests: pure logic & edge cases.
- Integration tests: IPC strategies, cache updates, multi-layer token acquisition.
- Concurrency tests: stress loops or use deterministic virtual time.
- Telemetry tests: for higher-risk instrumentation (span lifecycle, whether `recordException` is invoked on the correct error path / with the correct status, conditional emission) assert span creation & status (mock or capture exporter). Do NOT require tests merely for attribute/field attachment or the bare `recordException(...)` call (see §14 rule 3).
- Security tests: invalid credentials, revoked token, key rotation.
- E2E / UI tests: critical flows (login, token refresh, public API calls) with real or mocked backend.

### 7.3 Structure & Naming
- Use descriptive test names indicating method, condition, and expected result.
- Recommend naming such as: `methodName_condition_expectedResult`, e.g., `acquireToken_whenRefreshNeeded_fetchesNewToken`.
- Group related tests in classes or files by feature/module.

### 7.4 Tools & Patterns
- Use fake clocks/time providers to avoid flakiness.
- Avoid `Thread.sleep` in tests; use coroutines test dispatchers or latches.
- For randomness: inject deterministic seedable RNG.
- For flows: use `runTest` (Kotlin Coroutines Test) with `advanceUntilIdle()`.

### 7.5 Anti-Patterns
- Over-mocked tests (mocking everything; brittle).
- Assertions on logging messages only (weak), unless log semantics are contractual.
- Flaky timing-based tests without synchronization or virtual time.

### 7.6 Regression Test Guidance
If PR fixes a bug: require test reproducing previous failure and asserting new behavior.

--------------------------------------------------------------------------------

## 8. Documentation
Goal: Ensure clarity without redundant or tautological requests.

Before suggesting documentation:
1. Detect whether a Javadoc/KDoc block already exists immediately above the declaration.
2. Evaluate if it is adequate.

Only request additions or improvements if one or more apply:
- Missing entirely AND the item is non-private.
- Present but missing required elements for non-trivial declarations:
  * First-sentence summary (what it represents/does).
  * Clarification of non-obvious behavior, side effects, thread-safety, lifecycle nuances, error conditions.
  * Explanation of parameters, return value, and thrown exceptions where they are not self-explanatory.
  * Contextual usage guidance for complex flows (e.g., telemetry wiring, cryptographic contract).
- Clearly outdated or inaccurate relative to implementation.
- Public API surface changed meaningfully (new params, behavior shift) without doc update.

Do NOT request additional docs if:
- Existing docs succinctly and accurately describe purpose and there is no hidden complexity.
- The declaration is trivial (e.g., a simple data holder whose names are self-explanatory).
- Adding commentary would only restate code (“ResponseStatus: represents response status”).

Kotlin data classes:
- Class-level KDoc is sufficient when property names are obvious.
- Only suggest per-property KDoc for ambiguous names, domain-heavy semantics, or subtle units/constraints.

When requesting improvements:
- Quote the existing first line (e.g., `Existing doc: "Represents the status..."`).
- Specify exactly what is missing (e.g., “Document meaning of traceId and when time may be null.”).
- Avoid generic phrases like “Add proper documentation.”

Style guidance (only mention if violated):
- First sentence is a noun phrase or imperative summary (ends with a period).
- Avoid duplicating the class or method name verbatim.
- Document units, formats (e.g., epoch ms), threading assumptions, and ownership/lifecycle when relevant.

### 8.1 Examples (Bad vs Good)

Bad:
```java
/**
* Acquire token.
  */
  public BrokerResult acquireToken(TokenRequest request, String correlationId) { … }
```

Good:
```java
/**
* Acquires an access token via cache + network fallback.
*
* @param request Validated token request context.
* @param correlationId Optional caller correlation; generated if null.
* @return Non-null BrokerResult (success tokens or mapped error).
* @throws NetworkException On connectivity failure.
* @throws BrokerSecurityException On authority validation failure.
  */
  public BrokerResult acquireToken(TokenRequest request, String correlationId) { … }
```

--------------------------------------------------------------------------------

## 9. License Headers
Flag only if missing or malformed standard license header in new sources.

--------------------------------------------------------------------------------

## 10. Public API Stability & Migration

Flag:
- Enum value removal/rename (SpanName, AttributeName).
- Public method signature change consumed by MSAL/Broker.
- Cache or IPC schema modifications without backward read path.
- Behavioral default changes (e.g., authority fallback).

Require:
- PR summary migration note.
- Deprecation annotation before removal (unless urgent security fix).

Avoid false positives for private/internal refactors.

--------------------------------------------------------------------------------

## 11. Dependencies & Versioning

When any change is made to a `common/build.gradle` file or the shared dependency versions file at `gradle/versions.gradle` that adds a new dependency or changes a dependency version, always display the following warning message:
> ⚠️ **Warning:** Changes detected ⚠️
>
> Please follow the recommendations in [Adding or Updating Gradle Dependencies](https://eng.ms/docs/microsoft-security/identity/entra-developer-application-platform/auth-client/authn-sdk-msal-android/android-auth-libraries/how-tos/adding-or-updating-gradle-dependencies).

Flag:
- Security library downgrade.
- Major upgrade without referenced release notes.
- Wildcard versions (`1.+`).
- Transitive conflicts (duplicate telemetry libs).
- Method count surge (DEX pressure).

Recommend:
- Summarize upgrade impact (e.g., TLS changes).
- Consider BOM for version alignment.

--------------------------------------------------------------------------------

## 12. Resource & Lifecycle Management

Flag:
- Streams/cursors not closed (`use {}` / try-with-resources).
- Static retention of context-like objects (Android boundary).
- Long-lived secret buffers not cleared.
- Uncancelled coroutines after owning scope disposal.

--------------------------------------------------------------------------------

## 13. Kotlin–Java Interop & Nullability

- Avoid `!!`; prefer safe validation & early return.
- Provide Java-friendly overloads if Kotlin default params risk ambiguity.
- Use value/inline classes or sealed types for domain-specific IDs (avoid mixing plain strings).
- Defensive copies for mutable collections crossing API boundary.

--------------------------------------------------------------------------------

## 14. Recurring False-Positive Patterns — Do NOT Flag These

These rules are calibrated from the team's Copilot Code Review Effectiveness analysis: each pattern below is a comment category that engineers explicitly dismissed as *Confirmed Not Helpful* (wrong, irrelevant, or by-design). Treat them as strong suppression rules — default to staying silent. When you are tempted to post a comment matching one of these shapes, re-verify against the rule first; if the premise still holds, stay silent (a rule may carve a narrow, clearly-labeled exception). A missed nit costs the team far less than a confirmed false positive.

**Cross-cutting — don't reason from names.** A recurring root cause below is inferring behavior, type relationships, or a field's meaning/source from an *identifier name* (a class, field, attribute, or flag) instead of from the actual code. Treat names as hints, not evidence: verify the inheritance chain, population logic, or data source in the diff before raising a concern that depends on it.

1. **Exception-handler inheritance — verify the type hierarchy before flagging a "missing" catch.**
   Before suggesting an added `catch` block, or claiming a thrown type is not handled/retried, confirm the inheritance chain of the type that is already caught (don't assume coverage from the class name). A `catch` (or instanceof check) on a supertype already covers every subtype. For example (verify against current code): `UiRequiredException extends ServiceException`, so logic guarding on `ServiceException` (including retry logic) already applies to `UiRequiredException`.

2. **Hypothetical configurations / flight rollout state — at most a non-blocking sanity check.**
   When a concern's premise is a flight/configuration *combination* that may not exist in production (e.g. "if flight X is enabled together with cache implementation Y, then…"), do not raise it as a blocking issue — you have no visibility into rollout state, and the in-repo enum/flag default is not the deployed value. If the branch still looks genuinely risky, you may post **at most one** non-blocking note prefixed `Sanity check:` asking the author to confirm the combination is reachable in production, then move on — do not repeat it across the PR. Flag it normally only when the diff itself introduces an unsafe default or a real, reachable code path. (For example, the in-memory cache flights such as `ENABLE_FILTER_THEN_CLONE_IN_MEMORY_CACHE` may already be fully rolled out, making a concern about a non-memory cache implementation moot — and rollout state changes over time.)

3. **Telemetry / instrumentation attachment paths — do not demand unit tests for them.**
   Don't request a unit test whose sole purpose is to cover instrumentation — setting span attributes, attaching values, the bare act of recording an exception on a span (the `recordException(...)` attachment call itself), or populating an instrumentation field. These attachment paths are low-risk; this narrows §7.1 only, so keep asking for tests around branching logic, parsing/serialization, error/retry/fallback, concurrency, and public API behavior. (Note the boundary with §7.2: *whether* `recordException` fires on the correct error path or with the correct status is correctness logic and remains test-worthy — only the bare attachment call is exempt.)

4. **Cross-PR / cross-repo scope creep — flag genuine dependencies, not unrelated changes.**
   Don't pad a review with changes that belong in a *different, unrelated* PR or repo and aren't needed for this change to be correct (e.g. "while you're here, also update the companion ADAL feed to consume the new upstream"). That said, if the change genuinely **requires or breaks** something in another repo — a new Common `AttributeName`/`SpanName` that must be mirrored in Broker (see §6.5), a change to a shared contract such as `OneAuthSharedFunctions`, an IPC key consumed downstream — it is correct and valuable to surface it; that's exactly the kind of thing authors miss. When you do, frame it as a clearly-labeled, non-blocking follow-up/dependency note ("Follow-up (separate PR): …") rather than a required change to the current diff. The test is necessity, not location: raise cross-repo work the change actually depends on or invalidates; skip tangential "while you're here" suggestions.

5. **Field / domain semantics — don't assert meaning you can't verify; never hallucinate sources.**
   Do not assert what a field, parameter, or data source "means", or which endpoint/source it is populated from, unless the diff or quoted context supports it. If a claim depends on a domain fact you cannot confirm from the changed code, either omit it or state it explicitly as an assumption ("Assumption: … If incorrect, disregard."). For example, before describing where a value such as `clientDataInfo` / `BaseException.getClientDataInfo()` originates, verify it against the field's actual population logic rather than its name.

--------------------------------------------------------------------------------

## Appendix A: Comment Quality Guidelines

### A.1 Comment Quality Checklist (apply before posting)
For each comment, ensure:
- It references (quotes) the specific code fragment when context is not obvious.
- It states: (a) issue, (b) impact/rationale, (c) concrete recommendation.
- It avoids vague language (“might”, “maybe”, “probably”) unless uncertainty is inherent—then state assumptions.

###  A.2 Code Review Guidelines - Severity Legend (Optional)
- **Severity: High –** Exploitable vulnerability, data leak/PII exposure, authentication/authorization bypass, crypto misuse, race causing security breach, crash enabling denial-of-service, hot-path[...]
- **Severity: Medium –** Logic flaw causing incorrect results/state corruption, moderate performance regression, missing critical telemetry for a major operation, unhandled recoverable error path.
- **Low priority:** Immutability, minor docs/style, small clarity improvements, non-hot path micro-optimizations (rarely surface).

Prefix High severity comments exactly with `Severity: High –`.
For medium you may prefix `Severity: Medium –` (recommended for clarity).

### A.3 Patch Suggestion Guidelines
#### A.3.1 Patch Format
Use unified diff fenced code block or minimal code block for clarity; include sufficient context lines.
#### A.3.2 Multi-Line Replacement
If multiple identical lines: show first instance + comment listing other line numbers.
#### A.3.3 Safety Checklist (All True)
- Compiles
- Retains nullability / synchronization semantics
- Does not expose sensitive data
- Maintains telemetry span/attribute semantics (unless fix relates)
  If any false: provide conceptual change, not patch.

---

### A.2 Example Code Review Comments (Good vs Avoid)
Security:
Good: `Severity: High – Token logged in plaintext` Issue: Access token appended to log line in Error path. Impact: Leakage risk to log aggregation system. Recommendation: Remove token or replace wit[...]
Avoid: “Don’t log tokens.” (Non-specific)

Concurrency:
Good: “Race condition: double-checked lazy init missing volatile; visibility not guaranteed. Add @Volatile or use lazy {}.”
Avoid: “Maybe volatile?” (Speculative)

Performance:
Good: “Redundant JSON parser allocation in loop of 5k entries; move parser creation outside loop.”
Good: “Loop constructs O(N^2) growth when accounts list is large (list inside forEach). Consider using a hash lookup keyed by accountId.”
Avoid: “Create fewer objects.”
Avoid: “Could be faster.” (No explanation)

Telemetry:
Good: “Inline key ‘ipcStrategy’ used; replace with AttributeName.ipc_strategy to ensure classification & consistency.”
Avoid: “Attribute name should be constant.” (No location or rationale)

Testing:
Good: “Missing negative test: parse() returns null for malformed token; add test asserting error mapping for invalid header.”

Documentation:
Good: “Existing doc: ‘Represents the status of a success response.’ Missing: clarify whether time is server time or device capture time; document units (epoch ms?). Suggest: ‘… timeMillis: U[...]
Good: “Public method fetchKeys() lacks thread-safety contract; specify main-thread or safe multi-thread use + blocking behavior.”
Avoid: “Add proper documentation.” (Too generic)

Modernization:
Good: “Enum used only for type-safe wrapper of string; consider value class UserId(val value:String) to reduce accidental mixing of unrelated IDs.”

Invalid (must suppress):  
“Change to ‘val final statusMessage’” (Combines Kotlin + Java keywords incorrectly)

---

## Appendix B: Miscellaneous Guidelines

**Code Review Guidelines shouldn't be considered to be limited to the items listed here in this file.
Apply these instructions AND standard Java/Kotlin/Android secure, performant, and maintainable coding practices.
Flag real security, correctness, concurrency, performance, or API stability issues even if not explicitly listed here.
Do NOT flag style-only differences, speculative improvements, or untouched legacy unless the new change introduces risk.
Always cite specific code and give a minimal, actionable fix; use an assumption disclaimer if uncertain about High severity risks..**

### Key Terms (Quick Reference)
- TOCTOU: State validated earlier becomes stale before use.
- High-impact Performance: Likely to degrade hot-path throughput/latency or worsen complexity.
- Platform Type (Kotlin): Java-origin unknown nullability.
- Mechanical Change: Bulk rename/refactor/format/codegen with minimal semantic change.

---

### What NOT To Do
- Don’t flag unchanged legacy code unless the modification directly interacts with it AND introduces risk.
- Don’t require refactors beyond the PR’s scope unless a severe issue (security/correctness) is present.
- Don’t request style changes that contradict existing repository conventions.

---

Thank you for contributing to this project!
