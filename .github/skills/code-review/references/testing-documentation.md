# Testing and documentation

## Test coverage

Request tests when changed behavior:

- Adds branches that need positive and negative coverage.
- Adds retry, error, fallback, or feature-flag paths without coverage for each state.
- Parses or serializes data and lacks malformed/missing-input coverage.
- Adds concurrency primitives and lacks race/cancellation coverage.
- Adds or changes a public API without expected-behavior coverage.
- Fixes a bug without a regression test reproducing the previous failure.

Use unit tests for pure logic and edge cases; integration tests for IPC, cache updates, and multi-layer acquisition; deterministic stress/coordination tests for concurrency; and security tests for invalid credentials, revoked tokens, or key rotation. Reserve end-to-end/UI tests for critical flows.

For higher-risk telemetry lifecycle logic, tests can assert span creation, status, conditional emission, and exception-path behavior with a mock or capture exporter. Apply the instrumentation-only exemption in [false-positive suppression](false-positive-suppression.md).

Prefer descriptive names such as `methodName_condition_expectedResult`, and group related tests by feature in classes/files. Use fake clocks, deterministic seedable RNGs, latches or coroutine test dispatchers instead of `Thread.sleep`, and `runTest`/`advanceUntilIdle()` for flows. Avoid brittle over-mocking, assertions that test only non-contractual log text, and timing-based tests without synchronization or virtual time. Critical end-to-end/UI flows may use a real or mocked backend.

## Documentation

Before requesting Javadoc/KDoc, inspect the block immediately above the changed declaration and determine whether it is already adequate.

Request documentation only when:

- A non-private declaration has no documentation.
- A non-trivial declaration lacks a useful summary, behavior/side-effect details, threading/lifecycle/error semantics, or non-obvious parameter/return/exception information.
- A complex flow lacks contextual usage guidance, such as telemetry wiring or a cryptographic contract.
- Existing documentation is inaccurate after the change.
- A meaningful public API behavior change lacks migration/usage guidance.

Do not request documentation that merely restates names or code. Class-level KDoc is enough for a Kotlin data class with self-explanatory properties; request property docs only for ambiguous domain meaning, formats, units, or constraints.

When commenting, quote the existing first line and state exactly what is missing. Document units, formats, ownership, lifecycle, and threading only when relevant. Avoid vague requests such as "Add proper documentation."

Only mention these style rules when the changed documentation violates them:

- Start with a noun phrase or imperative summary that ends with a period.
- Do not merely duplicate the class or method name.

Bad:

```java
/**
 * Acquire token.
 */
public BrokerResult acquireToken(TokenRequest request, String correlationId) { ... }
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
public BrokerResult acquireToken(TokenRequest request, String correlationId) { ... }
```

For new source files, verify the standard license header is present and well formed. Do not comment on unchanged or tool-managed headers.
