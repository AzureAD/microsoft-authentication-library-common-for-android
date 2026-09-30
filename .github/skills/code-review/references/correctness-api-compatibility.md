# Correctness and API compatibility

Review changed code for:

- Null handling of Java platform types, unnecessary Kotlin `!!`, and missing fail-fast validation.
- Swallowed exceptions or overly broad catches that erase actionable errors.
- Boolean/precedence mistakes and wrong equality operators (`==` for Java strings, `===` for Kotlin value equality).
- Non-exhaustive handling of sealed/domain states.
- Internal mutable collections returned without a defensive or unmodifiable view.
- Variable shadowing, seconds/milliseconds confusion, or default charset use instead of UTF-8.
- Missing required token-response fields or silent fallback from a silent flow into interactive UI.
- Cache lookups that skip authority/tenant normalization.
- `equals` without matching `hashCode`, or array equality that does not compare contents.
- Serialization/deserialization that silently accepts missing or malformed required fields.
- Resource ownership or lifecycle behavior that changed without corresponding cleanup.

Use clear preconditions (`require`, `check`, early returns, or guard clauses). Prefer sealed result or domain error types over magic strings, and map external exceptions to domain-specific forms.

Use sets/maps for repeated membership checks and avoid recomputing invariant derived values in loops. Pair an overridden `equals` with `hashCode`, and use `contentEquals` for arrays. Return read-only views or defensive copies of internal mutable collections.

## Java, Kotlin, and nullability

- In Java, recommend `final` for locals, fields, and method parameters that are not reassigned; fields set once in the constructor should be `final`.
- In Kotlin, recommend `val` only when the reference is not reassigned.
- Never suggest `val final`, Java `final` on a Kotlin local/property, or `@NonNull` on a Kotlin declaration.
- Mention `final override` in Kotlin only when preventing further override is necessary for a concrete security, correctness, or documented-design reason. Otherwise remember that Kotlin declarations are final by default.
- Do not convert an intentional accumulator, loop variable, builder, or lazily mutated reference from `var` to `val`.
- Do not recommend immutability when a deliberate lazy-mutation performance trade-off requires mutation.
- For changed non-private Java method parameters and fields, verify appropriate `@NonNull`/`@Nullable` annotations. Use Kotlin type nullability for Kotlin APIs.
- Only comment on nullability annotations in code touched by the PR.
- Provide Java-friendly overloads when Kotlin default parameters risk Java-call ambiguity.
- Use value/inline classes or sealed types for domain-specific IDs to avoid mixing unrelated plain strings.

## Public and shared compatibility

Flag:

- Removal or renaming of `SpanName`, `AttributeName`, or other consumed enum values.
- Public method signature changes consumed by MSAL or Broker without migration.
- Cache or IPC schema changes without backward reads.
- Behavioral-default changes, including authority fallback.
- Changed shared result/command semantics that break test apps or downstream adapters.

Require a PR-summary migration note for meaningful public behavior changes and deprecation before removal unless an urgent security fix makes that impossible. Do not raise compatibility concerns for verified private/internal refactors.
