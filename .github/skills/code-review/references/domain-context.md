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
- Utilities such as clocks, RNG abstractions, URL handling, JSON adapters, and correlation IDs.
- Native code for performance or secure handling where present.

Changes here can cascade to `AzureAD/microsoft-authentication-library-for-android` and `AzureAD/ad-accounts-for-android`; review shared contracts with elevated rigor.

## Architecture and command lifecycle

The major layers are public facade, command orchestration, controllers, protocol, cache, crypto, telemetry, IPC/serialization, utilities, and error mapping.

A command normally:

1. Builds parameters.
2. Performs preflight validation of authority, scopes, and claims.
3. Resolves a controller strategy.
4. Starts `SpanName.CommandExecution` with the `correlation_id`.
5. Checks cache, then refreshes or calls network/Broker as needed.
6. Validates response integrity, including required claims, flags, and algorithms.
7. Atomically writes access-token, refresh-token, ID-token, and metadata artifacts.
8. Adapts the result to a DTO or exception.
9. Sets span status, records failures, and ends the span in `finally`.

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
- Preserve the error taxonomy:
  - Service errors represent protocol failures such as `invalid_grant` and `interaction_required`.
  - Client errors cover configuration, parsing, and network-unreachable failures.
  - UI-required errors identify flows that require interactive escalation.
  - Crypto errors cover key retrieval and generation failures.
- Do not flatten these distinct errors into a generic failure.
- Keep enums additive. Renames or removals require deprecation/migration planning.
- Keep old IPC keys readable and annotate deprecated keys.
- Document breaking public behavior with the appropriate major-change classification and migration steps.

## Cross-repository reuse

- MSAL relies on Common for command flow, token parsing, cache abstractions, telemetry enums, authority discovery, and error taxonomy. It must not redefine telemetry keys, fork token-response parsing, or introduce divergent cache-key normalization.
- Broker consumes Common IPC contracts, telemetry enums, crypto utilities, and token/cache models. Broker-specific PRT rotation and WPJ logic must still use Common-provided enums and attribute classification; new telemetry must extend Common rather than inline keys downstream.
- Test and automation apps depend on stable command/controller result semantics.
- Future platform integrations, including Linux Broker and cross-platform adapters, should reuse canonical authority normalization and cache schemas rather than fork token parsing.

Review mandates:

- Add shared telemetry names in Common before downstream use.
- Originate cache schema changes in Common with backward compatibility.
- Expand the Common exception hierarchy instead of creating parallel downstream error types.
- Do not allow downstream repositories to inline new shared IPC keys.
- Surface a required downstream change as a non-blocking `Follow-up (separate PR):` dependency, not unrelated scope for the current PR.

Centralization preserves one authoritative privacy classification, migration strategy, and cross-repository compatibility contract.

## High-impact review triggers

Treat these as high-severity candidates after verifying reachability and impact:

- Raw tokens, claims, or keys in logs.
- Inline telemetry keys that bypass the shared enums.
- Authority validation bypass.
- Cache or IPC key removal without backward support.
- A token-write race that can leave partial or inconsistent state.
- Static/reused cryptographic IVs or nonces.
- Disabled or short-circuited cryptographic verification.
- Telemetry spans that can remain open.

Medium-severity candidates include loss of specific error mapping, non-atomic multi-artifact writes, repeated authority-discovery calls, and missing correlation-ID propagation.

Prefer existing abstractions over ad hoc branches, existing crypto wrappers over duplicate primitives, explicit threading contracts, and short-lived handling of secret-bearing data.
