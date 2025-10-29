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
- Telemetry enums (SpanName, AttributeName, DataClassification) and instrumentation helpers.
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

### 1.9 Telemetry Enums & DataClassification
- AttributeName constants carry classification; new requires doc + rationale.
- Classification categories: SystemMetadata, OrganizationIdentifiableInformation, EndUserPseudonymousIdentifiers (never raw PII).
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
- For classification:
  - OrganizationIdentifiableInformation: tenant- / org-level identifiers.
  - EndUserPseudonymousIdentifiers: hashed or pseudonymous user correlation IDs (never raw secrets).
  - SystemMetadata: runtime/system context safe for broader aggregation.
  - EndUserIdentifiableInformation: only if absolutely necessary and privacy-compliant (e.g., hashed user IDs, not raw PII).

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
3. Determine correct `DataClassification` (e.g., `SystemMetadata`, `EndUserPseudonymousIdentifiers`).
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

### 7.2 Test Types & Expectations
- Unit tests: pure logic & edge cases.
- Integration tests: IPC strategies, cache updates, multi-layer token acquisition.
- Concurrency tests: stress loops or use deterministic virtual time.
- Telemetry tests: assert span creation & attribute presence (mock or capture exporter).
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
- Changelog classification (MAJOR/MINOR).
- Deprecation annotation before removal (unless urgent security fix).

Avoid false positives for private/internal refactors.

See [changelog.txt](../changelog.txt) for the changelog format.

--------------------------------------------------------------------------------

## 11. Dependencies & Versioning

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