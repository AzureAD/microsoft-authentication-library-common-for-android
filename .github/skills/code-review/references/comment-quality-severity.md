# Comment quality and severity

## Scope and evidence

- Review changed code and the local context required to understand it.
- Do not deep-audit untouched legacy code unless the change now interacts with it and creates a severe risk.
- Treat each file according to its language; never mix Java and Kotlin syntax.
- Do not nitpick formatter-managed output or style that follows repository conventions.
- Aggregate minor issues only when they affect the same contiguous snippet/function and have one remediation.
- Apply standard Java/Kotlin/Android security, correctness, concurrency, performance, and maintainability practices in addition to these references.

## Required comment shape

Every review comment must contain:

1. **Issue:** identify the specific changed code and verified defect.
2. **Impact:** explain the concrete security, correctness, compatibility, reliability, or meaningful performance consequence.
3. **Recommendation:** give a minimal, actionable fix.

Quote the code when the location is not self-explanatory. Avoid vague words such as "might", "maybe", and "probably" unless uncertainty is inherent. When a domain assumption cannot be verified, write `Assumption: ... If incorrect, disregard.`

## Severity

- Prefix high-severity findings exactly with `Severity: High –`. Use this for verified exploitable vulnerabilities, secrets/PII exposure, authentication/authorization bypass, crypto misuse, security-relevant races, or denial-of-service crashes.
- `Severity: Medium –` is optional but recommended for logic flaws that produce incorrect results/state corruption, moderate performance regressions, missing critical telemetry for a major operation, or unhandled recoverable errors.
- Rare low-priority comments may cover a concrete immutability, documentation, clarity, or non-hot-path optimization issue. Do not submit style-only suggestions.

## Patch suggestions

Provide a unified-diff or minimal replacement when the fix is straightforward. Include enough context and, for repeated instances, show the first replacement and list the other affected lines.

Only suggest code when it:

- Compiles in the changed language.
- Preserves imports, annotations, license headers, nullability, and synchronization.
- Does not expose sensitive data.
- Preserves telemetry semantics unless telemetry is the defect.

If any condition is uncertain, recommend the conceptual change instead of an unsafe patch.

Good comments are specific:

- `Severity: High –` A changed log statement emits the access token. Explain the exposure path and recommend removing or redacting the token.
- A parser accepts malformed required input. Explain the resulting error/state and request a negative regression test.
- A loop performs repeated linear membership checks in a hot path. Explain the complexity and recommend a keyed set/map.

Do not submit comments such as "Don't log tokens", "Could be faster", "Maybe volatile?", or "Add proper documentation" without evidence, impact, and remediation.
