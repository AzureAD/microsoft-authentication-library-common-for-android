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

Use clear preconditions (`require`, `check`, or guard clauses) and preserve domain-specific error mapping rather than magic strings or generic exceptions.

## Java, Kotlin, and nullability

- In Java, recommend `final` for unchanged locals, fields, and parameters when it improves correctness/clarity.
- In Kotlin, recommend `val` only when the reference is not reassigned.
- Never suggest `val final`, Java `final` on a Kotlin local/property, or `@NonNull` on a Kotlin declaration.
- Mention `final override` in Kotlin only when preventing further override is necessary for a verified correctness/security contract.
- Do not convert an intentional accumulator, loop variable, builder, or lazily mutated reference from `var` to `val`.
- For changed non-private Java APIs, verify appropriate `@NonNull`/`@Nullable` annotations. Use Kotlin type nullability for Kotlin APIs.
- Consider Java-friendly overloads only when changed Kotlin default parameters create real Java-call ambiguity.
- Preserve defensive copies for mutable values crossing Java/Kotlin API boundaries.
- Domain-specific value/inline/sealed types can prevent accidental identifier mixing, but recommend them only when the changed code demonstrates that risk.

## Public and shared compatibility

Flag:

- Removal or renaming of `SpanName`, `AttributeName`, or other consumed enum values.
- Public method signature changes consumed by MSAL or Broker without migration.
- Cache or IPC schema changes without backward reads.
- Behavioral-default changes, including authority fallback, without explicit migration impact.
- Changed shared result/command semantics that break test apps or downstream adapters.

Require a migration note for meaningful public behavior changes and deprecation before removal unless an urgent security fix makes that impossible. Do not raise compatibility concerns for verified private/internal refactors.
