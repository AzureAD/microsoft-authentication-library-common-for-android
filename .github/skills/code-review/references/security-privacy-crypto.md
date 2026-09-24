# Security, privacy, and cryptography

Flag changed code that introduces:

- Exposure of secrets, tokens, claims, keys, or PII through logs, telemetry, exceptions, storage, or return values.
- Authentication or authorization bypasses, exported Android components without appropriate protection, or weak permission checks.
- Unsafe input handling at IPC, intent, network, file, or deserialization boundaries.
- Race or TOCTOU behavior that affects authorization, token issuance, invalidation, or key use.
- Feature-flag behavior that executes an insecure path before a safe default is evaluated.
- Error handling that leaks sensitive internals.

Only consolidate security concerns when they affect the same contiguous snippet/function and share one remediation. Prefix a severe security comment exactly with `Severity: High –`.

## Cryptography and key management

- Reject weak or deprecated primitives such as MD5, SHA-1, ECB, static salts, or RSA PKCS#1 v1.5 unless a verified protocol mandate requires them.
- Require random, non-reused IVs/nonces for AEAD modes such as AES-GCM.
- Require `SecureRandom` for cryptographic randomness; do not accept `Random`.
- Never log keys, secrets, raw token contents, or full claims.
- Handle nulls and failures explicitly when retrieving or generating keys.
- Keep private keys in Android KeyStore, not plaintext `SharedPreferences`.
- Verify certificate validity and chain expectations where relevant.
- Keep key rotation atomic: validate the new key before decommissioning the old key.
- Prefer approved algorithms already used by the repository, including SHA-256/512, AES-GCM, RSA-OAEP, and ECDSA P-256 where applicable.

## Privacy and logging

- Do not emit raw tokens, private keys, full identifiers, user principal names, or claim JSON.
- Avoid full stack traces for expected validation failures; redact or summarize the failure.
- Do not use sensitive or unbounded high-cardinality telemetry values. Hash, bucket, or omit them only when repository policy permits.
- Keep secret-bearing buffers and derived/session keys alive only as long as necessary; clear buffers when feasible.

## Feature flags

Verify that security-sensitive flags have safe defaults, are evaluated before protected behavior executes, and do not leave a partially enabled insecure path. Do not infer deployed flight combinations from in-repository defaults; apply the flighting suppression rules before commenting.
