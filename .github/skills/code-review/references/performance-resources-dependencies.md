# Performance, resources, and dependencies

## General memory and storage performance screen

Apply this screen to every production-code PR, across all features and layers, including entirely new paths. ATS is a motivating example, not the boundary: do not wait for changes to an existing optimization, named cache class, or known hot path. Inspect callers and callees, including unchanged helpers newly invoked or invoked more often, wherever the PR can change their cost. This is a PR-wide behavior review, not a repository-wide audit of unrelated legacy code.

### Attribution: memory-storage/v1

For every actionable finding supported by this screen or the ATS checks below, append this exact standalone line to the review comment (or the finding's details when a review tool publishes it):

```text
Review rule: memory-storage/v1
```

Keep the existing issue/impact/recommendation and severity format. Use the tag once per comment, not for unrelated findings or generic performance observations. Consolidate duplicate findings as usual; do not create comments to meet a quota.

After actually applying the screen, include these standalone lines in the review summary, even when there are zero matching findings:

```text
Review screen: memory-storage/v1
Guidance revision: unknown
```

Replace `unknown` only with the verified commit SHA containing the loaded guidance, or `local-uncommitted` when reviewing with local edits to that guidance. Do not infer the guidance revision from the PR head or emit the screen marker if the screen was not applied. The summary records coverage, not an extra finding. Tags identify rule-associated comments, not proof the rule uniquely caused their discovery.

### Review questions

1. **Work versus need:** For a lookup, existence check, count, or single-record update, does the implementation read, decrypt, parse, copy, or rebuild an entire collection/store? Trace actual wrappers and lazy/eager evaluation; prefer existing exact-key/indexed access, key filtering before expensive value work, or safe reuse of an already available result.
2. **Frequency and scaling:** How many times does this execute per request, item, callback, retry, or concurrent caller? Identify nested scans, repeated materialization/sorting/serialization, and a whole-data operation repeated for each item. Compare work with total records N, required records K, and repetition count R; a new path needs a cost assessment even without a previous implementation.
3. **Allocation and peak memory:** What size and how many arrays, strings, buffers, DTOs, collection copies, and intermediate results coexist? Check eager `map`/`filter`/`toList` chains, deep copies, growing buffers, and batch/fan-out accumulation. Distinguish transient allocation/GC pressure from retained memory; streaming, batching, or a single pass helps only when semantics and resource lifetimes remain valid.
4. **Reuse and ownership:** Is stable data recomputed or reloaded unnecessarily? Is a useful cache recreated too frequently, or does a long-lived cache/closure/listener retain objects beyond their intended lifetime? Check ownership, key cardinality, size/lifetime bounds, invalidation, and supported concurrent/external writers. Do not prescribe caching everything or a global singleton.
5. **Critical-path impact:** Does apparently cheap memory work hide disk I/O, decryption, parsing, or a full snapshot? Is it on the UI/request path or under a contended lock? Include logging/telemetry argument evaluation and workers the caller waits for; moving work off-thread does not eliminate total work, memory pressure, or awaited latency.

### Paired examples

- **Avoid:** load/decrypt every value just to test whether one key exists. **Prefer:** the existing key/existence API if it has equivalent semantics; otherwise read only the required value.
- **Avoid:** call `loadAll().filter(...)` inside a loop over requested IDs. **Prefer:** reuse one bounded snapshot or perform indexed lookups, preserving freshness, ordering, and authorization.
- **Avoid:** build several full-size lists and serialized copies of an unbounded result before returning the first item. **Prefer:** bounded batches or streaming where the contract permits it.
- **Avoid:** cache every request under a unique ID in a process-lifetime map. **Prefer:** request-scoped ownership or an explicit size/lifetime policy consistent with reuse and invalidation needs.

### Evidence, safety, and coverage

For each suspected regression or newly introduced expensive pattern, establish actual runtime behavior, reachable usage, expected frequency/data growth, and latency/throughput/peak-memory/retention impact. Cite the PR line introducing the operation or dependency, even when the cost is inside an unchanged callee. Do not assert a leak from allocation alone or infer decryption from a method name.

Check existing targeted tests for operation counts, bounded result/batch sizes, cache reuse/invalidation, and release/eviction behavior where relevant. Add unrelated records or repeated requests to expose whole-data work. Use benchmarks or allocation/heap profiling when meaningful cost remains uncertain; do not invent timing claims or demand a benchmark for every collection operation.

Preserve encryption, persistence, authorization boundaries, freshness, synchronization, ordering, defensive-copy guarantees, cancellation, and resource ownership. A small bounded copy, necessary snapshot, legitimate cold load, or infrequent allocation is not automatically a finding. Conversely, large startup/migration peak memory or a new unbounded collection is not exempt merely because it is cold. Report demonstrated material risks, not speculative micro-optimizations.

## Common hot paths

Prioritize frequent or data-size-sensitive operations:

- Cache operations and serialization.
- Authority discovery and metadata reuse.
- Token-response parsing.
- Cryptographic operations.
- Telemetry emission, including avoidable attribute churn.

Flag repeated regex compilation/reflection, large temporary allocations in loops, repeated derivation of stable normalized values, accidental quadratic membership/lookups, unbounded parallel command launches, and executor-per-call patterns.

Reuse buffers where safe, clear secret-bearing arrays when feasible, and avoid retaining large ephemeral collections. Reuse meaningful existing spans rather than adding micro-spans; represent durations as numeric attributes instead of creating spans around trivial local work.

## AcquireTokenSilent (ATS) regression checks

Preserve two established ATS optimizations: reuse in-memory account and credential records, and avoid full token-cache decryption during token save. Review both the warm access-token cache-hit path and the refresh/network path through persistence and response construction; token save is part of ATS latency.

### Change-driven trace

1. Trace the changed call through the actual cache factory, instance lifetime, account/credential implementation, storage wrappers, and encryption/serialization layer. Include indirect callers such as overlapping-token cleanup, account merging, metadata, diagnostics, and telemetry. A factory, injected implementation, or helper change can regress ATS without editing the ATS entry point.
2. Compare before/after work per request: cache construction/initialization, persistent reads, decrypted/deserialized records, allocations, and work under shared locks. Distinguish total stored records N from relevant matches K; an O(N) in-memory scan is not equivalent to decrypting/deserializing N persisted records.
3. Verify the applicable opt-in/flight and actual runtime implementation. Do not infer deployed rollout from enum defaults or assume every MSAL/OneAuth/Broker path enables memory caching. Apply the existing flight/reachability suppression rules.

### Patterns to flag when introduced or worsened

- Bypassing or recreating the reusable in-memory account/credential cache on each ATS call, lookup, or save; repeatedly clearing/reloading it; changing its identity/key so requests stop sharing the intended store. Preserve existing store/client/family isolation rather than introducing one global cache.
- Reading/decrypting/deserializing persisted account/credential records on every warm lookup when the existing coherent in-memory records suffice.
- Calling encrypted-store `getAll()` (directly or through a helper) during routine token save, then filtering, counting, checking existence, merging, or logging. This makes one token update pay for unrelated records; prefer existing memory records, exact-key access, or filtering keys before decrypting only necessary values.
- Moving filtering after decryption/deserialization, performing the same full enumeration for each AT/RT/ID token, or materializing/cloning the entire cache before selecting the relevant records.
- Adding a full-cache read under the shared cache lock, increasing both save latency and wait time for concurrent ATS requests. Moving the same read to a worker that ATS waits for does not remove its cost.

Do not optimize by removing encryption at rest, durable writes, required cleanup/merging, synchronization, defensive-copy isolation, or cache invalidation. Preserve coherence after save/remove/clear, sign-out, and any supported external/process writes; the memory implementation's single-instance assumption must still hold.

### Concrete examples and counterexamples

- **Regression:** a new token-save diagnostic calls `encryptedStore.getAll().size`. Even though it only needs a count, the actual encrypted implementation may decrypt every token before returning. Remove the enumeration or reuse an already available count without exposing token data.
- **Preferred:** merge an updated record with the existing memory record (or exact persisted key where required), encrypt/write the updated value, and update the coherent memory entry. Required overlapping-token selection may scan memory without rehydrating the persistent cache.
- **Do not trust API names:** `SharedPreferencesFileManager.getAllFilteredByKey()` filters keys before decrypting matching values, but `EncryptedNameValueStorage.getAllFilteredByKey()` currently calls `getAll()` before filtering. Resolve the concrete implementation; the latter is existing context, not a finding by itself.
- **Not automatically a defect:** one-time cold initialization, migration, explicit export/diagnostics outside ATS, encryption of new values, or a bounded exact-key read needed for correctness. Reclassify initialization if the diff now repeats it per ATS request. Raw Android `SharedPreferences.getAll()` alone does not prove decryption.

Current Common starting points (verify against the reviewed revision): `BrokerOAuth2TokenCache.getCacheToBeUsed`, `MsalOAuth2TokenCache.create`, `saveCredentialsInternal`, `deleteAccessTokensWithIntersectingScopes`, `SharedPreferencesAccountCredentialCacheWithMemoryCache`, `SharedPreferencesFileManager`, and `EncryptedNameValueStorage`.

### Evidence and regression coverage

For a finding, cite a changed line, the reachable ATS branch and concrete callee chain, before/after work (including N versus K), user-visible latency/contention impact, and a minimal invariant-preserving fix. Use existing severity criteria based on demonstrated impact; do not claim measured milliseconds, deployed flights, or blanket High severity without evidence.

For affected cache behavior, check focused regression coverage: complete cold initialization first, then assert repeated warm reads reuse the same intended cache without persistent rereads; save/replace/remove/clear remain coherent; and saving a token does not enumerate/decrypt unrelated records. Use storage/encryption call counts with unrelated records added to the fixture, not just elapsed-time assertions or mocks that bypass the storage wrapper. Exercise refresh-and-save as well as cache-hit ATS and applicable flight branches. Prefer deterministic counts; request benchmarks only when a real cost uncertainty remains, separating cold/warm and small/large-cache cases.

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
