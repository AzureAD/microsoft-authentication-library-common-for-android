---
name: code-review
description: Guide GitHub Copilot pull-request reviews for the Common Android Auth library using repository-specific correctness, security, compatibility, telemetry, performance, testing, comment-quality, and informational safe-rollout rules for material infrastructure/concurrency behavior changes.
---

# GitHub Copilot PR review

Use this skill only for pull-request review in `AzureAD/microsoft-authentication-library-common-for-android`.

## Workflow

1. Read the complete diff and enough local context to understand each changed path. Review changed code and directly affected behavior; do not deep-audit untouched legacy code.
2. Classify the change and load the applicable references:
   - Always: [comment quality and severity](references/comment-quality-severity.md) and [false-positive suppression](references/false-positive-suppression.md).
   - Commands, controllers, OAuth/OIDC, cache, authority, IPC, errors, shared contracts, or downstream consumers: [domain context](references/domain-context.md) and [correctness and API compatibility](references/correctness-api-compatibility.md).
   - Authentication, authorization, tokens, claims, logs, telemetry data, feature flags, intents, deserialization, keys, certificates, or cryptography: [security, privacy, and cryptography](references/security-privacy-crypto.md).
   - Shared mutable state, executors, threads, coroutines, flows, cancellation, cache updates, or key rotation; changed/added/removed/moved `synchronized` blocks/methods, Kotlin `@Synchronized`, explicit `Lock`/`withLock`/`Mutex`, or modified lock expressions/helpers/receivers/shared-state paths: [concurrency and threading](references/concurrency-threading.md). Follow its [change-driven analysis](references/concurrency-threading.md#lock-identity-and-beforeafter-analysis) even for refactors without new locking tokens. Also follow [runtime call targets](references/concurrency-threading.md#runtime-target-trace) for changed interface/abstract calls, concrete overridable calls in Java and Kotlin (including Java virtual and Kotlin `open` methods), injected implementations, hierarchy/override changes, or callback/listener/lambda invocations/bodies reached while a lock is held, even when that caller is unchanged. Synchronization merely present elsewhere in a file, or an interface call alone, is not a finding.
   - Production infrastructure behavior changes involving lock order/identity/scope, scheduling/executors/cancellation, cache coordination, IPC/account/registration contracts, or implementation/injection/override/hierarchy changes reached by locked callers: [informational safe rollout](references/rollout-guidance.md), alongside applicable concurrency, domain, compatibility, and security references. Assess actual changed semantics and production reach even for a "refactor"; syntax or file location alone does not justify a rollout note. Review existing gates/plans and consuming-host boundaries, including nonflightable changes and unsafe mixed paths.
   - Java/Kotlin production logic, validation, nullability, collections, serialization, error modeling, or public/shared APIs: [correctness and API compatibility](references/correctness-api-compatibility.md).
   - Cache/serialization hot paths, authority discovery, token parsing, allocations, build/dependency files, or resource ownership: [performance, resources, and dependencies](references/performance-resources-dependencies.md).
   - OpenTelemetry spans, attributes, status, correlation IDs, `SpanName`, or `AttributeName`: [telemetry and observability](references/telemetry-observability.md).
   - Tests, bug fixes, public APIs, Javadoc/KDoc, documentation changes, or new source files: [testing and documentation](references/testing-documentation.md).
   Load every reference whose trigger applies; one diff can require several.
3. Verify each candidate finding against actual code: trace type hierarchies, data sources, reachability, lifecycle, synchronization, existing tests, and repository conventions. Treat names as hints, not evidence.
4. Report only high-confidence, actionable findings. Each comment must identify the issue, explain its impact, and recommend a concrete fix. Include a patch only when it is safe and compilable. Narrow process exception: permit at most one visible, non-blocking `Rollout consideration:` per coherent material infrastructure behavior change under [rollout guidance](references/rollout-guidance.md#decision-procedure). Name the actual risk and verified safe boundary, or ask only for the exact missing deployment contract. Do not assert a defect, assign automatic High severity, demand a flag, or duplicate adequate gating/plans.
5. Before submitting, remove duplicates, speculative concerns, style-only comments, unrelated scope expansion, and findings already suppressed by the references.

Prioritize security, correctness, concurrency, compatibility, and meaningful performance regressions. Tailor every comment to the changed code.
