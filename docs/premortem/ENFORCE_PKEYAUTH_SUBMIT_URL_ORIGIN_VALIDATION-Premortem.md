<!-- Copyright (c) Microsoft Corporation. All rights reserved. Licensed under the MIT License. -->

# Premortem - `ENFORCE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION`

**Owner:** Shahzaib Jameel  |  **Approver:** TBD

**Common PR:** [AzureAD/microsoft-authentication-library-common-for-android#3226](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/pull/3226)  |  **Broker telemetry PR:** [security/ad-accounts-for-android#288](https://msft.ghe.com/security/ad-accounts-for-android/pull/288)  |  **Work item:** [AB#3706623](https://identitydivision.visualstudio.com/Engineering/_workitems/edit/3706623)

`ENFORCE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION` defaults to `false`. Set
`EnforcePKeyAuthSubmitUrlOriginValidation` to `true` in ECS to block a WebView PKeyAuth
challenge when its `SubmitUrl` is not an absolute HTTPS URL with the same host and
normalized port as the origin that issued the challenge.

The separate master flight,
`ENABLE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION`
(`EnablePKeyAuthSubmitUrlOriginValidation`), defaults to `true`. It records the challenging
origin, evaluates the `SubmitUrl`, and emits telemetry. With the master flight on and
enforcement off, the feature operates in shadow mode: non-`ALLOWED` verdicts are measured
but the challenge continues.

| Flight state | Behavior |
|---|---|
| Master off | Restore the pre-fix behavior: no origin recording, validation, enforcement, or feature telemetry. |
| Master on, enforcement off | Shadow mode: evaluate and emit the verdict, but allow the challenge to continue. This is the current default. |
| Master on, enforcement on | Reject every non-`ALLOWED` verdict before the device key signs or submits a response. |

## Intent

PKeyAuth challenges arrive through an untrusted WebView redirect and include a `SubmitUrl`.
Without origin validation, an attacker-controlled challenge could cause the client to sign
a PKeyAuth response with the device key and submit that response to an attacker-selected
destination.

Enforcement is intended to ensure that:

1. The `SubmitUrl` and challenging origin are both absolute HTTPS URLs.
2. Their hosts match case-insensitively.
3. Their ports match after normalizing the implicit HTTPS port to 443.
4. Parser-differential input such as a backslash authority is rejected.
5. A rejected challenge is abandoned before device-key signing or network submission.

## Current production baseline

The following baseline was queried from production `android_spans` on October 26, 2026,
using a rolling 30-day window capped at query time. It includes only
`ProcessPKeyAuthChallenge` spans with a populated origin-validation verdict.

| Measure | Observed |
|---|---:|
| All distinct observed Broker device IDs | approximately 744,877,641 |
| Distinct observed device IDs on Broker 16.6 or newer | approximately 257,874,077 |
| PKeyAuth challenge spans | 1,434,286 |
| Devices exercising PKeyAuth | 735,019 (0.099% of all observed devices; 0.285% of Broker 16.6+ devices) |
| `ALLOWED` verdicts | 1,405,210 |
| `REJECTED_ORIGIN_MISMATCH` verdicts | 29,076 (2.027%) |
| Devices with an origin mismatch | 19,768 (2.689% of PKeyAuth devices; 0.0077% of Broker 16.6+ devices) |
| Enforcement enabled | 0 |
| Non-main-frame challenges | 0 |

Device counts are approximate distinct telemetry identifiers, not the installed-device
population. The overall and Broker-version denominators use merged daily HLL sketches from
`BrokerAdoptionStats`; the PKeyAuth populations use sampled `ProcessPKeyAuthChallenge`
spans. A device that exercised PKeyAuth only in a trace that was not sampled is therefore
not represented in the PKeyAuth numerator.

Only `ALLOWED` and `REJECTED_ORIGIN_MISMATCH` were observed. There were no sampled
`REJECTED_SUBMIT_NOT_HTTPS`, `REJECTED_ORIGIN_UNRESOLVABLE`, or
`REJECTED_BACKSLASH_AUTHORITY` verdicts.

### Origin-mismatch classification

| Challenging host classification | Submit host classification | Observed mismatches |
|---|---|---:|
| AAD cloud | Non-AAD | 16,202 |
| Non-AAD | AAD cloud | 12,012 |
| AAD cloud | AAD cloud | 604 |
| Non-AAD | Non-AAD | 258 |

The
[MS-PKAP response specification](https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-pkap/3eb27db4-5335-4e3e-acd4-2354e23be472)
states that the server uses the same URL to which the client submitted its request as the
`SubmitUrl`. The
[request specification](https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-pkap/74c43da3-e97e-4e35-a34a-024054c0f42e)
likewise requires the client response URL to be the same service URL that issued the
challenge. The observed rows are therefore the exact cross-origin shape the enforcement
flight is designed to prevent.

The shadow rollout is working as intended:

- 98% of sampled PKeyAuth challenges already satisfy strict same-origin validation.
- 28,214 of the 29,076 mismatches (97.03%) cross the AAD/non-AAD trust boundary.
- Mismatches affect only 0.0077% of observed Broker 16.6+ devices.
- No malformed, cleartext, unresolvable-origin, or backslash-authority verdict appeared in
  the sampled production population.

Raw hosts are intentionally not emitted. The cloud classifications provide a privacy-safe
view of the security boundary without exposing the challenge or submission URLs.

### Caller concentration

The following high-volume or high-rate callers were observed in the seven-day window ending
October 26, 2026:

| Calling package | Challenges | Mismatches | Mismatch rate |
|---|---:|---:|---:|
| `com.microsoft.teams` | 133,375 | 2,508 | 1.880% |
| `com.microsoft.office.outlook` | 98,389 | 1,542 | 1.567% |
| `com.microsoft.windowsintune.companyportal` | 9,822 | 1,174 | 11.953% |
| `com.azure.authenticator` | 113,538 | 914 | 0.805% |
| `com.microsoft.msapps` | 3,267 | 242 | 7.407% |
| `com.microsoft.intune` | 2,910 | 113 | 3.883% |
| `com.microsoft.office.word` | 1,067 | 56 | 5.248% |

Smaller caller populations also have very high mismatch rates. For example,
`com.samsung.android.knox.kam` recorded 281 mismatches in 297 sampled challenges, and
`com.microsoft.launcher.enterprise` recorded 147 in 224. A high mismatch rate is not by
itself evidence of a compatibility problem: these are exactly the requests the flight is
designed to block. These populations should be treated as priority canary-monitoring
segments because enforcement will affect a large share of their PKeyAuth traffic.

## Risks and mitigations

1. **The expected cross-origin population is blocked**
   - **Likelihood:** Certain when enforcement is enabled. Shadow telemetry shows that
     approximately 2.0% of PKeyAuth challenges receive
     `REJECTED_ORIGIN_MISMATCH`.
   - **Impact:** Those challenges are abandoned before signing or submission, preventing
     the signed device assertion from being sent across origins.
   - **Why it is bounded:** Approximately 98% of sampled PKeyAuth challenges are already
     same-origin, and mismatch devices represent only 0.0077% of observed Broker 16.6+
     devices.
   - **Mitigation and response:** Roll out by percentage, verify that blocked volume tracks
     the shadow baseline, and monitor higher-rate caller segments separately.

2. **Impact is concentrated in callers with a higher mismatch rate**
   - **Likelihood:** Certain based on the shadow distribution.
   - **Impact:** Company Portal, MSApps, Intune, Word, Samsung Knox KAM, Microsoft Launcher
     Enterprise, and other concentrated populations will see a higher enforcement rate than
     the global Broker population.
   - **Why it is bounded:** The affected population is directly measurable by caller, app
     version, broker version, Android version, OEM, and device count.
   - **Mitigation and response:** Review each concentrated caller during the canary and do
     not use the global average as the only rollout signal.

3. **Enforcement surfaces as cancellation or retry instead of an obvious validation
   error**
   - **Likelihood:** Medium. Calling applications may retry an interactive request or
     translate the broker error into cancellation.
   - **Impact:** Increased repeated prompts, `auth_cancelled_by_sdk`, timeout, or user
     cancellation rather than a clearly attributable PKeyAuth failure.
   - **Why it is bounded:** The challenge span records the verdict and enforcement state
     before throwing, allowing affected traces to be identified.
   - **Mitigation and response:** Compare exposed and shadow-mode cohorts for parent
     `AcquireTokenInteractive` and `ATIInteractively` outcomes, repeated requests per
     device, cancellation, timeout, and support incidents. Do not rely only on the
     PKeyAuth span's own status.

4. **Sampling obscures the downstream impact**
   - **Likelihood:** Medium.
   - **Impact:** An outcome rate is misread because absent traces or incomplete parent
     spans are treated as complete evidence.
   - **Why it is bounded:** Every evaluated challenge emits the validation verdict,
     enforcement state, main-frame state, origin source, and broad AAD-cloud
     classifications.
   - **Remaining limitation:** `android_spans` uses head-based sampling. Missing terminal
     spans and zero-row correlation lookups are not proof of success or absence. Raw hosts
     are intentionally unavailable.
   - **Mitigation and response:** Use request and device counts, compare exposed and control
     cohorts, require minimum sample sizes, inspect customer/client logs for reported
     failures, and treat trace joins as directional evidence only.

5. **Rollback disables useful observation**
   - **Likelihood:** Low.
   - **Impact:** Investigation becomes harder if the master flight is disabled when only
     enforcement needed to be rolled back.
   - **Why it is bounded:** Enforcement and observation are separate flights. Turning only
     enforcement off returns to shadow mode while retaining verdict telemetry.
   - **Mitigation and response:** Use
     `EnforcePKeyAuthSubmitUrlOriginValidation=false` as the primary rollback. Disable the
     master flight only if origin tracking or evaluation itself causes a regression.

## Rollout decision

**Proceed with a controlled production canary; do not jump directly to broad enforcement.**

The shadow-mode result is encouraging: approximately 98% of sampled PKeyAuth challenges
already satisfy strict same-origin validation. The remaining 2% is the population the
flight is expected to block. That rate is not evidence of a defect or a reason by itself to
delay enforcement.

The reason to stage the rollout is impact control, not an assumption that the mismatches
are legitimate. At full exposure, the current sampled volume represents roughly 29,000
blocked challenges across about 20,000 devices per rolling month, with some callers much
more concentrated than the aggregate. A canary confirms that authentication health remains
stable while the intended security blocking is introduced.

Production canary rollout can begin after these operational prerequisites are ready:

1. A production monitor compares enforced and shadow cohorts by caller, app version,
   broker version, Android version, OEM, verdict, origin source, parent outcome, and
   repeated requests per device.
2. High-rate callers are included as explicit monitoring segments rather than hidden by the
   aggregate.
3. Available controlled validation and the existing automated validation suite are green.
4. Rollback ownership and ECS propagation expectations are confirmed before each stage.

## Proposed rollout stages

After the operational prerequisites are met, use a percentage-based rollout. Each stage
requires an explicit review rather than automatic expansion.

| Stage | Proposed exposure | Minimum observation |
|---|---:|---|
| Internal validation | Microsoft test accounts and devices only | 48 hours and successful controlled PKeyAuth completion |
| Production canary | 1% | 48 hours and at least 500 enforced challenge spans |
| Limited production | 5% | 48 hours |
| Broad production | 25% | 72 hours |
| Majority production | 50% | 72 hours |
| Full enforcement | 100% | Expand after 72 healthy hours at 50%; continue daily monitoring for at least 72 hours |

At the 1% stage, review every sampled blocked challenge. At later stages, review every new
caller, verdict, origin-source, app-version, Android-version, or OEM concentration.

## Monitoring plan

Review production telemetry at least twice daily through the 25% stage and daily through
the first two weeks at 100%.

1. **Flight exposure and telemetry completeness**
   - Filter `span_name == "ProcessPKeyAuthChallenge"`.
   - Verify exposed spans report
     `pkeyauth_submit_url_origin_validation_enforced=true`.
   - Verify every evaluated span has a verdict, origin source, main-frame state, and both
     AAD-cloud classifications.
   - Treat missing feature spans as telemetry-incomplete, not proof that no challenge
     occurred.
2. **Blocked challenge volume**
   - A non-`ALLOWED` verdict with enforcement `true` is an actual blocked challenge.
   - Break down blocked requests and devices by caller, active broker, app version, broker
     version, Android version, OEM, verdict, origin source, and AAD-cloud classifications.
   - During canary stages, investigate every blocked challenge.
3. **Exposed-versus-control authentication outcomes**
   - Join to direct parent spans by `trace_id` and `parent_span_id`.
   - Compare `AcquireTokenInteractive`, `ATIInteractively`, and `WorkplaceJoin` errors,
     cancellation, timeout, and clean `OK` outcomes between enforced and shadow cohorts.
   - Track repeated interactive attempts per device as a retry-loop signal.
4. **Caller-specific guardrails**
   - Monitor Company Portal, MSApps, Teams, Outlook, Authenticator, Intune, Word, Samsung
     Knox KAM, Microsoft Launcher Enterprise, and any newly observed caller separately.
   - Do not let a healthy aggregate rate hide a caller-specific regression.
5. **Support and incidents**
   - Search for interactive sign-in failure, device authentication failure, repeated
     prompts, Company Portal or Intune enrollment failure, ADFS failure, and PKeyAuth
     errors.
   - Preserve affected app version, broker version, Android version, OEM, caller, verdict,
     origin source, and example `correlation_id_v2` values.

## Stage gates and rollback thresholds

Pause expansion and investigate when any of the following occurs:

- An exposed cohort shows a confirmed authentication regression attributable to
  enforcement.
- A caller, origin-source, or platform segment shows an unexpected authentication-outcome
  regression relative to its shadow-mode baseline.
- The exposed cohort's interactive authentication error or cancellation rate increases by
  more than 0.1 percentage points or 1.25x relative to the comparable shadow cohort,
  sustained for one hour with at least 1,000 exposed parent spans.
- Repeated interactive attempts per device increase by more than 25% relative to control.
- Any high-priority customer impact or Sev 2-or-higher incident is attributed to
  enforcement.

Roll back immediately rather than merely pausing when:

- A reproducible authentication flow regresses because enforcement is enabled.
- Company Portal, Intune, Workplace Join, or device-management completion regresses.
- Authentication cannot recover by retrying or switching account.
- The enforcement cohort cannot be distinguished reliably in telemetry.

## Rollback

Set `EnforcePKeyAuthSubmitUrlOriginValidation` to `false` in ECS.

Expected behavior after propagation:

- Origin recording, validation, and telemetry continue because the master flight remains
  enabled.
- Spans report
  `pkeyauth_submit_url_origin_validation_enforced=false`.
- Non-`ALLOWED` verdicts return to shadow mode and no longer throw.
- Device-key signing and response submission again proceed as they did before enforcement.
- No app release is required.

After rollback:

1. Confirm the effective enforcement attribute is `false` for new spans in affected app
   versions.
2. Confirm non-`ALLOWED` verdicts continue to be emitted, proving that observation remains
   active.
3. Confirm the reported sign-in or management failure no longer reproduces after ECS
   propagation.
4. Compare parent error, cancellation, timeout, and retry rates with the pre-enforcement
   shadow baseline.
5. Preserve the affected dimensions and example correlation IDs for root-cause analysis.

If validation or origin tracking itself is causing the regression, set
`EnablePKeyAuthSubmitUrlOriginValidation` to `false` as a secondary full-feature rollback.
This removes validation and its telemetry and should not be the first response to an
enforcement-only regression.

## Post-rollout criteria to keep enforcement enabled

- No confirmed authentication regression is attributed to enforcement.
- No regression remains concentrated in a caller, origin-source, Android-version, OEM, or
  app-version segment.
- Exposed-versus-control parent error, cancellation, and retry rates remain within the
  stage gates.
- Controlled interactive PKeyAuth validation remains successful for the supported broker,
  first-party app, federated, and device-management scenarios.
- At least 100,000 enforced challenge spans have been observed across seven consecutive
  days, including meaningful traffic from Company Portal, Teams, Outlook, Authenticator,
  and Intune.
- No Sev 2-or-higher incident is attributed to PKeyAuth `SubmitUrl` origin enforcement.
