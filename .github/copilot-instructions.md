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
- Removal/rename of existing keys without fallback = Severity: High –.

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
- Secret/PII exposure (tokens, RT, claims, keys).
- Crypto misuse (weak algorithms, static IV/nonce).
- Unvalidated external inputs used in security decisions (authority, URL).
- Ignored KeyStore failures with insecure fallback.
- Non-secure RNG usage.
- Races affecting token integrity or key rotation.

Privacy:
- Hash/pseudonymize user identifiers.
- Avoid high-cardinality sensitive attributes.

Feature Flags:
- Secure defaults; no insecure code pre-flag evaluation.

--------------------------------------------------------------------------------

## 3. Concurrency & Thread Safety

Flag:
- Unsynchronized compound cache operations.
- Double-checked locking lacking `volatile` / `@Volatile`.
- Blocking I/O on main thread (if Android-facing layer).
- Unsafe publication of partially constructed objects.
- Missed cancellation propagation (`CancellationException` swallowed).

Safe Patterns:
- Mutex/synchronized around multi-step mutations.
- Immutable snapshot replace for infrequent updates.
- Atomic access for counters/flags.

Security Intersection:
- Token refresh race issuing stale AT.
- Key rotation concurrency exposing mismatched encryption/decryption key pair.

--------------------------------------------------------------------------------

## 4. Code Correctness & Business Logic

Flag:
- Missing mandatory token fields or algorithm constraints.
- Authority normalization omission.
- Broad exception swallowing.
- Nullability misuse (`!!` on platform type).
- Equality/hash mismatch (override one without the other).
- Returning mutable internals.

Immutability suggestions only if not reassigned; avoid mixing keywords.

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

Required Pattern:
```java
final Span span = OTelUtility.createSpan(SpanName.CommandExecution.name());
try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
    span.setAttribute(AttributeName.command_type.name(), command.getType().name());
    // Logic...
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

Anti-Patterns:
- Inline attribute key strings.
- Missing `finally` termination.
- Sensitive raw values recorded.
- Micro-spans for trivial getters.
- Omitted correlation_id.

Attributes:
- Use primitive types (boolean, numeric).
- Omit if absent (not empty string).
- Document time units (epoch millis vs seconds).

--------------------------------------------------------------------------------

## 7. Testing

Flag Missing:
- Branch coverage for new controller logic (success/fallback/error).
- Cache schema migration path tests.
- Key rotation (success/failure).
- FOCI fallback logic.
- Telemetry attribute emission tests.
- Concurrency race prevention (stress or simulation).
- Regression test for fixed bug scenario.

Types:
- Unit (parsing, normalization).
- Integration (command pipeline end-to-end).
- Concurrency (cache multi-thread).
- Telemetry (exporter capture).
- Crypto (signature/rotation with deterministic secure RNG).
- Regression (previous failure reproduction).

Stability:
- No `Thread.sleep`; use virtual time/latches/coroutines test utilities.
- Deterministic RNG injection; fake clock for expiration tests.

--------------------------------------------------------------------------------

## 8. Documentation

Request only if:
- Public controller/command lacks explanation of flow & error mapping.
- Migration/schema changes absent docs.
- Crypto wrappers missing algorithm constraints.
- Telemetry additions missing classification rationale.
  Quote first doc line when suggesting improvement; specify missing units, threading, lifecycle.

Skip trivial data holders with clear naming.

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

## 14. Appendix A: Comment Quality Guidelines

### A.1 Checklist
Each comment:
- References specific code (quote snippet when ambiguous).
- Provides Issue, Impact, Recommendation.
- Avoids vague hedging; assumptions explicitly stated.

### A.2 Severity Legend
- Severity: High – Exploitable vulnerability, data leak/PII, auth bypass, crypto misuse, race causing security breach, crash enabling DoS, hot-path severe perf regression.
- Severity: Medium – Logic flaw, moderate performance regression, missing critical telemetry, unhandled recoverable error.
- Low priority – Minor docs/style, clarity, non-hot path micro-optimizations.

Prefix High severity exactly: `Severity: High –`.

### A.3 Patch Suggestion Guidelines
Unified diff or focused code block with context lines.
Safety Checklist:
- Compiles
- Preserves nullability / synchronization semantics
- Does not expose sensitive data
- Maintains telemetry semantics (unless fix is telemetry)
  If any false → conceptual recommendation only.

### A.4 Examples
Security (Good):
“Severity: High – Token logged in plaintext …” (Issue, Impact, Recommendation)
Avoid non-specific “Don’t log tokens.”

Concurrency (Good):
“Race: double-checked lazy init missing volatile; add @Volatile or use lazy {}.”

Performance (Good):
“Repeated JSON parser allocation in loop of N entries; hoist parser outside loop.”

Telemetry (Good):
“Inline key ‘ipcStrategy’; replace with AttributeName.ipc_strategy for classification consistency.”

Testing (Good):
“Missing negative test for malformed token parse; add test asserting specific exception mapping.”

Documentation (Good):
“Existing doc: ‘Represents status…’ Missing expiration units & thread-safety guarantee.”

Invalid Suggestion (Suppress):
Mixed language keyword: `val final`.

--------------------------------------------------------------------------------

## 15. Appendix B: Miscellaneous Guidelines

- Not exhaustive—apply industry standards for security, correctness, performance.
- TOCTOU: Validate state at point of use.
- High-impact Performance: Focus on realistic hot-path degradation.
- Mechanical Change: Large rename/refactor with minimal semantic delta—limit commentary to introduced risks.

### What NOT To Do
- Don’t flag untouched legacy absent interaction risk.
- Don’t demand broad refactors beyond PR scope unless severe issue.
- Don’t conflict with established styling.
- Don’t mix Kotlin + Java keywords.

--------------------------------------------------------------------------------

## 16. Glossary
- FOCI: Family of Client IDs shared RT.
- Canonical Authority: Normalized host + tenant for consistent cache keying.
- Correlation ID: GUID tracking across spans & commands.
- DataClassification: Privacy tier for telemetry attributes.
- Atomic Update: Single logical commit for related token artifacts.

--------------------------------------------------------------------------------

## 17. Quick Security Hardening Checklist
(Use selectively; avoid noise.)
- No secret/claim logging.
- SecureRandom for crypto.
- Authority normalized.
- Span ends in finally.
- Correlation ID propagated.
- Atomic token write.
- No static IV/nonce reuse.
- Telemetry keys from enums only.

--------------------------------------------------------------------------------

## 18. Performance Instrumentation Guidance
- Prefer single comprehensive span + numeric attributes vs multiple micro-spans.
- Cache derived stable values within operation scope.
- Eliminate repeated object churn in tight loops (reuse builder/buffer).
- Avoid premature optimization suggestions outside hot paths.

--------------------------------------------------------------------------------

## 19. Recap of High-Impact Review Signals
Immediate focus if diff shows:
- Telemetry inline string key duplication.
- Cache key removal without migration.
- Crypto algorithm downgrade.
- Authority validation bypass or relaxed rules.
- Span missing finally termination.
- Sensitive data in exception messages.

--------------------------------------------------------------------------------

Thank you for contributing to the Common authentication library!