# Performance, resources, and dependencies

Focus performance review on actual hot paths:

- Cache operations and serialization.
- Authority discovery and metadata reuse.
- Token-response parsing.
- Cryptographic operations.
- Telemetry emission, including avoidable attribute churn.

Flag repeated regex compilation/reflection, large temporary allocations in loops, repeated derivation of stable normalized values, accidental quadratic membership/lookups, unbounded parallel command launches, and executor-per-call patterns.

Reuse buffers where safe, clear secret-bearing arrays when feasible, and avoid retaining large ephemeral collections. Reuse meaningful existing spans rather than adding micro-spans; represent durations as numeric attributes instead of creating spans around trivial local work.

## Resource and lifecycle management

Flag:

- Streams or cursors not closed with try-with-resources/`use`.
- Static retention of Android `Context`-like objects.
- Long-lived secret buffers.
- Uncancelled coroutines after their owning scope is disposed.
- Executors that outlive their owning component.
- Cleanup that can be skipped by exceptions or cancellation.

## Dependencies and versioning

When a change to `common/build.gradle` or `gradle/versions.gradle` adds a dependency or changes a dependency version, include this warning:

> ⚠️ **Warning:** Changes detected ⚠️
>
> Please follow the recommendations in [Adding or Updating Gradle Dependencies](https://eng.ms/docs/microsoft-security/identity/entra-developer-application-platform/auth-client/authn-sdk-msal-android/android-auth-libraries/how-tos/adding-or-updating-gradle-dependencies).

Also flag:

- Security-library downgrades.
- Major upgrades without referenced release notes.
- Wildcard versions such as `1.+`.
- Transitive conflicts, especially duplicate telemetry libraries.
- A method-count surge that creates DEX pressure.

Recommend summarizing upgrade impact (for example, TLS behavior) and using a BOM when it genuinely improves version alignment.
