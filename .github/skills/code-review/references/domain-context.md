# Common domain context

Common supplies cross-repository primitives consumed by MSAL and Broker:

- Command architecture (`TokenCommand`, `BrokerCommand`, controllers).
- OAuth2/OIDC request and response handling.
- Token cache, serialization, authority/environment normalization, and FOCI.
- Cryptography utilities, KeyStore integration, hashing, key wrapping, JWE, and JWS.
- Telemetry enums and instrumentation helpers.
- IPC contracts and shared data models.
- Cloud instance and regional authority discovery.
- Error taxonomy and mapping.
- Utilities such as clocks, RNG abstractions, JSON adapters, and correlation IDs.
- Native code where present.

Changes here can cascade to `AzureAD/microsoft-authentication-library-for-android` and `AzureAD/ad-accounts-for-android`; review shared contracts with elevated rigor.

## Architecture and command lifecycle

The major layers are public facade, command orchestration, controllers, protocol, cache, crypto, telemetry, IPC/serialization, utilities, and error mapping.

A command normally:

1. Builds parameters and validates authority, scopes, and claims.
2. Resolves a controller strategy.
3. Starts the command span with a correlation ID.
4. Checks cache, then refreshes or calls network/Broker as needed.
5. Validates response integrity.
6. Atomically writes access, refresh, ID-token, and metadata artifacts.
7. Adapts the result or exception.
8. Sets span status, records failures, and ends the span in `finally`.

Flag changes that bypass a required stage or make the stages inconsistent.

## Token cache and authority invariants

- Access tokens are short-lived and scope-limited; refresh tokens may be app or family tokens; ID tokens carry identity claims and must never be logged raw.
- Device/PRT artifacts should be referenced logically through Broker. Keep derived/session keys ephemeral and minimize retention.
- Cache keys depend on normalized environment, client ID, home account ID, and tenant ID.
- Use family refresh-token fallback only when the app-specific refresh token is absent.
- Multi-artifact cache writes must remain atomic; avoid partial AT/RT/ID-token or metadata state.
- Canonicalize authority before cache keying.
- Evict only when a new artifact supersedes the previous validity window.
- Validate authority hosts against discovery metadata. Regional endpoints require a secure fallback.
- Cache discovery metadata rather than repeating network calls for the same authority.
- Gate cache or metadata schema changes with versioning and fallback reads.

## IPC, errors, and migration

- Keep IPC key constants stable. Additive changes must continue reading old keys and preserve protocol/schema/version semantics.
- Treat removal or renaming of an existing IPC key without a fallback as high severity.
- Preserve distinct service, client, UI-required, and crypto errors; do not flatten recoverable or actionable errors into a generic failure.
- Keep enums additive. Renames or removals require deprecation/migration planning.
- Document breaking public behavior with the appropriate major-change classification and migration steps.

## Cross-repository reuse

- MSAL relies on Common for command flow, token parsing, cache abstractions, telemetry enums, authority discovery, and error taxonomy. It should not need parallel implementations.
- Broker consumes Common IPC contracts, telemetry enums, crypto utilities, and token/cache models.
- Test and automation apps depend on stable command/controller result semantics.
- Future platform adapters should reuse canonical authority normalization and token parsing rather than fork them.

Review mandates:

- Add shared telemetry names in Common before downstream use.
- Originate cache schema changes in Common with backward compatibility.
- Expand the Common exception hierarchy instead of creating parallel downstream error types.
- Do not allow downstream repositories to inline new shared IPC keys.
- Surface a required downstream change as a non-blocking `Follow-up (separate PR):` dependency, not unrelated scope for the current PR.

## High-impact review triggers

Treat these as high-severity candidates after verifying reachability and impact:

- Raw tokens, claims, or keys in logs.
- Authority validation bypass.
- Cache or IPC key removal without backward support.
- A token-write race that can leave partial or inconsistent state.
- Static/reused cryptographic IVs or nonces.
- Disabled or short-circuited cryptographic verification.
- Telemetry spans that can remain open.

Medium-severity candidates include loss of specific error mapping, non-atomic multi-artifact writes, repeated authority-discovery calls, and missing correlation-ID propagation.

Prefer existing abstractions over ad hoc branches, existing crypto wrappers over duplicate primitives, explicit threading contracts, and short-lived handling of secret-bearing data.
