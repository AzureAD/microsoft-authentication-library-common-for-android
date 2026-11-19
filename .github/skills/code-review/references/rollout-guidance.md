<!--
Copyright (c) Microsoft Corporation.
All rights reserved.

This code is licensed under the MIT License.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files(the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions :

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
-->

# Informational safe-rollout guidance

## Contents

- [Decision procedure](#decision-procedure)
- [Common and consuming-host boundaries](#common-and-consuming-host-boundaries)
- [Mixed-path safety](#mixed-path-safety)
- [Bugs, deployment, and recovery](#bugs-deployment-and-recovery)
- [Comment shape](#comment-shape)
- [Textual decision cases](#textual-decision-cases)

Assess deployment risk separately from correctness. This narrow process exception permits an informational note without a proven bug; it does not weaken concurrency evidence, compatibility, security, severity, or [noise suppression](false-positive-suppression.md).

## Decision procedure

1. Trace the changed production semantics and affected callers: lock order/identity/scope, scheduling/executors/cancellation, cache coordination, IPC/account/registration contracts, and changed injection/implementation/override/hierarchy behavior reached by locked callers. Apply the [before/after analysis](concurrency-threading.md#lock-identity-and-beforeafter-analysis) and [runtime-target trace](concurrency-threading.md#runtime-target-trace). "Refactor" is not proof of equivalent behavior; a path or locking token alone proves neither a defect nor deployment risk.
2. Establish a material infrastructure behavior change with broad production reach and a concrete potential failure, latency, hang, or invariant impact. Name the changed behavior and affected operations/consumers rather than claim all users are exposed because this is Common. Stay silent when the change is mechanical with verified identical behavior, unrelated non-infrastructure work, or merely near unchanged locks. Tests, samples, documentation, and nonshipped generated output do not need rollout advice.
3. Read the actual gate coverage and PR rollout description. Suppress already adequate gates or documented plans, including safe staged-version deployment for nonflightable changes. If a changed path escapes the actual gate/plan, cite that path and consequence; do not request a duplicate flag. Repository defaults are not deployed cohort state. Preserve the bounded `Sanity check:` rule and do not repeat the same uncertainty in a rollout note.
4. Identify a known safe gated/reversible boundary using the host and mixed-path checks below. A flag is only one option. For a nonflightable change, consider a safe staged consuming-version deployment with available monitoring and feasible rollback/recovery. If the boundary is unknown but material risk is established, ask only for the exact missing deployment/compatibility contract; do not invent a flag or claim safe mixing.
5. Emit at most ONE visible, non-blocking `Rollout consideration:` per coherent change, not one per file, lock, callback, or subsystem. Name the actual risk and boundary (or exact missing contract). Keep the recommendation bounded and separate from proven defects; no automatic High severity, missing-flag merge blocker, percentage threshold, private incident reference, new telemetry requirement, or generic operational checklist.

## Common and consuming-host boundaries

`CommonFlightsManager` delegates to a consumer-provided `IFlightsManager`; without one, its provider returns `IFlightConfig` defaults. `CommonFlight` documents current Broker initialization and warns that MSAL/OneAuth app-process decisions need the host SDK. These are library abstractions, not proof that Common controls global cohorts, remote settings, or every consumer's initialization.

Trace the actual consuming host, process, selected implementation, gate, and affected library path. Do not impose Broker-only flags on Common public APIs or claim that adding an enum gives app processes a remotely reversible rollout. A host release/version boundary can be more appropriate than a library flag; verify [old/new cache and IPC compatibility](correctness-api-compatibility.md#public-and-shared-compatibility) before recommending it.

Configuration reads themselves can block, reenter, or acquire locks through host/provider code. `IFlightsManager` permits waiting for initial configuration; even its zero-timeout overload is not a verified cheap/thread-safe contract for every implementation. Do not introduce network/provider lookups under a critical section. Resolve the concrete lookup contract and evaluate outside protected work only when selection still preserves synchronization, visibility, ordering, cancellation, and lifetime. Do not supply an invented flag API or configuration engine.

## Mixed-path safety

- Never recommend per-request selection between paths with incompatible shared-state protocols, lock order, or lock identity. Old A -> B and new B -> A can deadlock when concurrent operations use the same locks; old/new monitors can split protection for the same state.
- Reading a flag once per operation prevents only an intra-operation reread; concurrent operations can still choose different strategies. It is NOT sufficient protection against incompatible mixing.
- Stable selection for the actual shared-state owner, process, or cohort may avoid local concurrent mixing, but still requires cross-process coordination, persistent-cache/schema compatibility, and old/new Broker-client IPC/version compatibility. Different owners/processes can share state. A restart does not erase persistent state or stop another process.
- Runtime flips require verified quiescence or an atomic transition protocol covering in-flight work, ownership, shared state, and related readers/writers. Do not recommend dynamic swapping without that protocol.
- If compatibility or transition safety is unknown, identify that missing contract rather than propose a per-operation switch. A known incompatible mixed protocol is not a feature-flag candidate; assess a safe version deployment only if feasible.

## Bugs, deployment, and recovery

Report proven deadlocks, authentication defects, or cache corruption using the existing evidence/severity rules and require a correctness fix. A flight limits exposure only when safe; it does not fix the defect. Do not duplicate a bug comment with "add a flag" instead of repairing the invariant.

Never recommend keeping a known broken/vulnerable old path or delaying an urgent fix for unnecessary flag infrastructure. Urgent fixes are not blanket exemptions from deployment assessment: a corrected implementation can still warrant safe staged-version deployment and monitoring when the diff establishes material infrastructure risk. Do not demand a new feature flag.

Check reversibility before promising rollback. Cache migrations, key changes, persistent side effects, and protocol/version changes can make downgrade unsafe even when process-local strategy selection is stable. Confirm old/new Broker-client and cache readers can coexist; otherwise identify the required compatibility/forward-recovery plan, not an imaginary off switch.

Point only to available signals tied to the change, such as affected-operation failures, acquisition latency, hangs/timeouts, or cache invariant failures, and feasible rollback/recovery. Do not mandate new telemetry, rollout percentages, or unrelated operational work.

## Comment shape

Use the visible prefix `Rollout consideration:` without severity/blocking labels. Cite the changed behavior, explain concrete potential impact, and name the verified safe boundary or exact missing deployment contract. Ask for one bounded action, not a checklist. Keep uncertainty explicit; a process note is not a claim that a bug or deployed cohort state is proven.

Synthetic examples, valid only with the stated evidence:

> Rollout consideration: This executor replacement changes ordering and cancellation for concurrent token refreshes across the host's acquisition path. The documented startup selection isolates shared-state owners across processes, persisted formats and Broker-client protocols remain compatible across supported versions, and no in-flight strategy flip occurs. Consider a staged host release using that boundary and the existing refresh failure/latency signals, with downgrade only while cache compatibility is preserved.

> Rollout consideration: This registration protocol change affects clients on older Broker versions, but the PR does not establish their compatibility after the persistent migration. Can you clarify the supported mixed-version deployment and recovery boundary? A per-request flag or process restart would not establish that contract.

Do not emit either example verbatim without its evidence. Suppress the first if the PR already documents an adequate release plan; use the second only when that material changed compatibility risk is established. Do not also post a `Sanity check:` for the same missing contract.

## Textual decision cases

These are synthetic review decisions, not executable flag engines or claims about deployed Common behavior.

| Changed behavior and established evidence | Review decision |
|---|---|
| Shared refresh executor changes ordering/cancellation; stable host startup selection, cross-process/state/version compatibility, and safe downgrade are verified; deployment plan missing. | At most one informational note naming that host boundary and existing refresh failure/latency signals. |
| Broad cache-coordination change is not safely flightable; version compatibility and staged host release/recovery boundary are verified. | One bounded safe-version deployment note if the PR lacks an adequate plan; no new flag demand. |
| Same changes already have adequate gate coverage or a documented compatible staged-version plan. | No duplicate rollout advice. |
| Formatting, rename, or helper move retains the same monitor, caller coverage, ordering, and behavior. | No rollout note merely because the file contains locks or is called infrastructure. |
| Changed field attaches a telemetry value; synchronization elsewhere is untouched. | No rollout note from locking tokens/file location or instrumentation alone. |
| Old/new cache strategies use reversed lock order on the same objects; a flag is captured once per request. | Do not recommend this mixed-path flag. Report any proven cycle separately; assess only feasible safe deployment after correctness/transition repair. |
| Strategy is frozen per process, but multiple processes share persistent cache and IPC compatibility is unknown. | No claim of safe process isolation; one bounded deployment-contract clarification only for established material risk. |
| Urgent fix removes a vulnerable fallback and changes broad registration behavior. | Require the fix; never retain that fallback. Assess feasible version rollout without delaying remediation for unnecessary flags or exempting urgency wholesale. |
| Common enum default is false; the consuming host's production cohort state is unavailable. | Do not infer rollout state or report a missing-flight blocker. Missing cohort data alone warrants no note. |
| A migration irreversibly rewrites persisted cache that old readers cannot consume. | No off-switch/downgrade promise; report proven compatibility defects separately and clarify a feasible compatibility/forward-recovery plan. |
| PR adds an actual path bypassing the documented host gate into changed shared infrastructure. | Cite the escape and impact; assess the actual safe boundary rather than request another flag or assume production cohorts. |
| Only tests/samples/docs/nonshipped generated output or unrelated non-infrastructure logic changes. | No infrastructure rollout note. |
