---
name: code-review
description: Guide GitHub Copilot pull-request reviews for the Common Android Auth library using repository-specific correctness, security, compatibility, telemetry, performance, testing, and comment-quality rules.
---

# GitHub Copilot PR review

Use this skill only for pull-request review in `AzureAD/microsoft-authentication-library-common-for-android`.

## Workflow

1. Read the complete diff and enough local context to understand each changed path. Review changed code and directly affected behavior; do not deep-audit untouched legacy code.
2. Classify the change and load the applicable references:
   - Always: [comment quality and severity](references/comment-quality-severity.md) and [false-positive suppression](references/false-positive-suppression.md).
   - Commands, controllers, OAuth/OIDC, cache, authority, IPC, errors, shared contracts, or downstream consumers: [domain context](references/domain-context.md) and [correctness and API compatibility](references/correctness-api-compatibility.md).
   - Authentication, authorization, tokens, claims, logs, telemetry data, intents, deserialization, keys, certificates, or cryptography: [security, privacy, and cryptography](references/security-privacy-crypto.md).
   - Shared mutable state, executors, threads, coroutines, flows, cancellation, cache updates, or key rotation: [concurrency and threading](references/concurrency-threading.md).
   - Cache/serialization hot paths, authority discovery, token parsing, allocations, dependencies, or resource ownership: [performance, resources, and dependencies](references/performance-resources-dependencies.md).
   - OpenTelemetry spans, attributes, status, correlation IDs, `SpanName`, or `AttributeName`: [telemetry and observability](references/telemetry-observability.md).
   - Tests, bug fixes, public APIs, Javadoc/KDoc, or new source files: [testing and documentation](references/testing-documentation.md).
3. Verify each candidate finding against actual code: trace type hierarchies, data sources, reachability, lifecycle, synchronization, existing tests, and repository conventions. Treat names as hints, not evidence.
4. Report only high-confidence, actionable findings. Each comment must identify the issue, explain its impact, and recommend a concrete fix. Include a patch only when it is safe and compilable.
5. Before submitting, remove duplicates, speculative concerns, style-only comments, unrelated scope expansion, and findings already suppressed by the references.

Prioritize security, correctness, concurrency, compatibility, and meaningful performance regressions. Tailor every comment to the changed code.
