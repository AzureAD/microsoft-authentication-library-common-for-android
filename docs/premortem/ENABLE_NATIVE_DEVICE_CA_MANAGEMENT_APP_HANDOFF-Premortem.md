<!-- Copyright (c) Microsoft Corporation. All rights reserved. Licensed under the MIT License. -->

# Premortem - `ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF`

**Owner:** Sowmya Malayanur  |  **Approver:** TBD

**Common PR:** [AzureAD/microsoft-authentication-library-common-for-android#3290](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/pull/3290)  |  **Broker PR:** [security/ad-accounts-for-android#321](https://msft.ghe.com/security/ad-accounts-for-android/pull/321)  |  **Work item:** [AB#3779482](https://identitydivision.visualstudio.com/Engineering/_workitems/edit/3779482)

**Monitoring dashboard:** [New Android Broker Dashboard - Device CA native management-app handoff](https://dataexplorer.azure.com/dashboards/3f6536b7-62e6-407d-8071-31575adb78bb#627f67cd-0bfd-450e-8a81-f5e03758a4c3)  |  **Query/runbook:** [`ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF-Dashboard.md`](./ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF-Dashboard.md)

`ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF` defaults to `true`. Set it to `false` in ECS to stop native management-app routing and restore the existing Device CA WebView/browser behavior without an app release.

The router contains a separate pre-existing exception for production-signed Teams IP Phone devices (`com.microsoft.skype.teams.ipphone`) with Company Portal installed. It launches Company Portal before evaluating this flight because those devices have no browser or Back button. This branch does not execute on ordinary non-Teams devices and is not a risk introduced by this flight.

| Flight | ON does |
|---|---|
| `ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF` (`EnableNativeDeviceCaManagementAppHandoff`) | For brokered WebCP Device CA requests, detects a supported profile/device owner and targets `intune-remediation://re-wpj` to that management app. If native launch fails, routing falls back in order to a package-targeted HTTPS App Link, generic HTTPS, and the existing WebView/browser path. Brokerless and non-WebCP flows retain their existing behavior. |

## Intended routing

1. On Teams IP Phone devices only, the pre-existing compatibility branch launches Company Portal and stops before evaluating the flight.
2. On all other devices, continue through legacy WebView/browser routing when WebCP-in-WebView is disabled, the request is brokerless, or this flight is disabled.
3. When the flight is eligible, detect a supported management owner in the current Android user:
   - Company Portal profile owner -> target Company Portal.
   - Google DPC profile owner -> target Intune.
   - Google DPC device owner -> target Intune.
4. Launch the package-targeted native Re-WPJ URI.
5. If native launch fails with an expected resolution or security error, try:
   1. package-targeted HTTPS App Link;
   2. generic HTTPS handler;
   3. the existing WebView path.
6. Unexpected runtime failures remain terminal and return the existing authorization error.

## Risks and mitigations

The likelihood ratings below are qualitative assessments based on routing scope, automated tests, and manual validation. They are not production incident probabilities; a production baseline does not exist yet.

1. **The wrong management app is selected or no owner is detected**
   - **Likelihood:** Low for selecting the wrong app. A missing supported owner is possible on an unsupported or unexpected management topology, but it is a safe fallback condition rather than a terminal failure.
   - **Why it is bounded:** The router does not infer the owner from installed packages. It asks Android `DevicePolicyManager` whether Company Portal or Google DPC is the profile/device owner and uses a fixed mapping: Company Portal owner -> Company Portal; Google DPC owner -> Intune.
   - **Pre-release evidence:** Unit tests cover Company Portal profile owner, Google DPC profile owner, Google DPC device owner, no supported owner, and unavailable `DevicePolicyManager`. Manual validation results are linked below.
   - **Mitigation and response:** If no supported owner is detected, the request stays on the existing WebView/browser path. The management-owner tile exposes `none`, `device_policy_manager_unavailable`, and distribution changes. If telemetry indicates incorrect routing, disable the flight in ECS to restore legacy routing.

2. **The fallback chain fails after native launch fails**
   - **Likelihood:** Low to medium for an individual fallback failure because package visibility, App Link verification, browser policy, and OEM behavior vary. Low for a terminal failure because every recovery route must fail.
   - **Why it is bounded:** Expected native-launch failures (`ActivityNotFoundException` or `SecurityException`) proceed through package-targeted App Link, generic HTTPS, and finally the existing WebView path.
   - **Pre-release evidence:** Unit tests cover App Link success, App Link failure followed by generic HTTPS success, missing browser followed by WebView success, policy-denied launches, WebView load failure, and terminal error handling. Sandbox telemetry validated both App Link fallback success and WebView fallback success.
   - **Mitigation and response:** Dashboard tiles separately show terminal outcomes, stage-level fallback outcomes, and terminal failures. A successful fallback completes this flight's responsibility. If terminal failures breach the documented threshold, disable the flight to bypass the native-specific routes.

3. **The flight affects an unintended flow**
   - **Likelihood:** Very low.
   - **Why it is bounded:** Native routing is reachable only for a URL whose parsed `ismdmurl` query parameter equals `1`, while running in the auth service with WebCP-in-WebView enabled. Brokerless requests, non-WebCP requests, and ordinary external website redirects retain their existing paths.
   - **Pre-release evidence:** Parser tests accept the marker in any query position and reject `ismdmurl=0`, `notismdmurl=1`, and malformed substring matches. Existing browser-redirect tests verify that an ordinary `browser://` URL opens in the browser and returns `CANCELLED`.
   - **Mitigation and response:** Unexpected `ProcessDeviceCaRequest` volume is visible on the dashboard and can be inspected at trace level to confirm scope. The ECS kill switch removes native owner detection and handoff without changing the ordinary browser redirect path.

4. **Routing launches the management app more than once**
   - **Likelihood:** Low, but not directly measurable as a distinct telemetry outcome.
   - **Why it is bounded:** Each router invocation makes one native launch attempt. On accepted handoff, it stops WebView loading and returns `MDM_FLOW`, which ends the original authorization request. Recovery-path tests verify a single completion callback.
   - **Pre-release evidence:** Unit tests verify one native launch attempt per routed request. Visible duplicate activity is a manual-validation check because telemetry cannot prove how many activities the user saw.
   - **Mitigation and response:** Investigate duplicate-launch reports together with same-trace Device CA span volume and lifecycle details; telemetry alone cannot prove that two visible activities opened. Disable the flight in ECS if duplication is reproducible.

## Rollout decision

Keep the flight defaulted to `true`; do not use a percentage-based slow rollout.

- The effective cohort is already narrowly constrained to brokered WebCP Device CA requests with the exact Device CA marker. Ordinary sign-ins and ordinary `browser://` redirects do not enter native routing.
- A missing owner or expected launch failure degrades through the ordered fallbacks to the pre-existing WebView/browser behavior.
- Automated tests cover each owner decision, eligibility boundary, fallback stage, terminal failure, and flight-off behavior; manual validation is linked below.
- Six production dashboard tiles cover volume/completeness, routing success, terminal outcomes, owner detection, fallback stages, and terminal failures.
- ECS remains an immediate kill switch that restores legacy routing without an app release.

A percentage rollout would reduce the already-low sampled Device CA volume and delay useful confidence without materially reducing risk to ineligible authentication traffic. The release guardrail is therefore default-on monitoring with explicit thresholds and immediate ECS rollback, not progressive exposure.

## Can we measure failure?

**Yes.** The flight is responsible for selecting and launching the management app, executing the fallback chain when needed, and returning `MDM_FLOW` error code.

The `ProcessDeviceCaRequest` span records the flight gate, detected management owner, terminal routing outcome, and each fallback stage. It can detect owner-resolution changes, native launch failures, fallback concentration, and terminal broker-side failures.

The original authorization request ends when `MDM_FLOW` is returned and is converted to an `AuthorizationStatus.FAIL` / `device_needs_to_be_managed` error. What happens inside the management app after the accepted handoff, and any later authentication request, are outside this flight's monitoring contract.

## Monitoring baseline

For the default-on production release:

1. Use the six Device CA tiles in the [New Android Broker Dashboard](https://dataexplorer.azure.com/dashboards/3f6536b7-62e6-407d-8071-31575adb78bb#627f67cd-0bfd-450e-8a81-f5e03758a4c3) to monitor volume/completeness, routing success, terminal outcomes, management owner, fallback stages, and terminal failures. Use the existing Broker Host App Version parameter for release-specific filtering.
2. Establish a stable effective native-routing-gate baseline (`is_native_device_ca_management_app_handoff_enabled=true`) for terminal routing failures, fallback outcomes, and Device CA request volume.
3. Validate that every sampled span has a terminal `device_ca_routing_outcome`.
4. Confirm that measured brokered WebCP telemetry reports the effective native-routing gate as `true`. The Teams IP Phone compatibility path does not evaluate the gate. Within this measured scope, `false` should appear after rollback.
5. Use ad-hoc Kusto investigation to split a suspicious app-version or management-owner segment further by Android version, broker package, OEM, or management mode.
6. Do not make a rollback decision from fewer than 100 sampled Device CA spans in the selected app-version or management-owner segment. The Broker default sampling configuration currently samples `ProcessDeviceCaRequest` at 50%, so review sampled counts with every rate.

The dashboard uses the production `ad-accounts-android-otel` database. Before a build containing `ProcessDeviceCaRequest` telemetry reaches production, empty tiles are expected. Pre-deployment test spans are available only in `android-broker-otel-sandbox`.

## Monitoring plan

Review the [Device CA dashboard](https://dataexplorer.azure.com/dashboards/3f6536b7-62e6-407d-8071-31575adb78bb#627f67cd-0bfd-450e-8a81-f5e03758a4c3) at least twice daily for the first 14 days of the default-on production release. Review the six tiles in this order:

1. **Device CA span volume and telemetry completeness**
   - Compare `ProcessDeviceCaRequest` volume by app version.
   - Confirm the effective native-routing gate is `true`; `not_evaluated` is expected for the Teams IP Phone compatibility path, and `false` is expected after rollback.
   - Verify all sampled spans have a terminal routing outcome.
   - Investigate any spans that are brokerless or not in WebCP.
2. **Device CA routing success rate**
   - Count `browser_launch_failed`, `webview_load_failed`, `unexpected_routing_failure`, and `ERROR` spans.
   - Investigate an increase greater than 0.5 percentage points or 2x the stable baseline, sustained for one hour with at least 100 sampled spans.
3. **Device CA terminal routing outcomes**
   - Among requests with a supported owner, track `native_handoff_succeeded`.
   - Track the share of requests ending in App Link, generic HTTPS, WebView, or browser fallback.
   - Investigate any app-version, Android-version, or management-owner segment where native handoff success drops below 95% with at least 100 eligible sampled spans.
   - Investigate a fallback-share increase greater than 5 percentage points or 2x baseline.
4. **Device CA management owner distribution**
   - Break down `device_ca_management_owner`.
   - Investigate `device_policy_manager_unavailable`, unexpected `none`, or a sudden owner-distribution shift concentrated in an Android version, OEM, or management mode.
5. **Device CA fallback-stage outcomes**
   - Identify whether package App Link, generic HTTPS, or WebView is absorbing a native handoff failure.
   - Investigate any new `launch_failed`, `handler_not_found`, or `load_failed` concentration.
6. **Device CA terminal routing failures**
   - Inspect terminal failures by outcome, error, and Broker Host App Version.
   - A successful fallback is not a reason to ignore a native-routing regression, but only terminal broker-side failures directly indicate that no recovery surface was accepted.
7. **Support and incidents**
   - Search for the wrong management app opening, the management app not opening, duplicate launches, or all routing fallbacks failing.

## Manual validation

All manually validated tests are documented in the [Device CA manual validation results](https://microsoft.sharepoint-df.com/:fl:/g/contentstorage/CSP_1bb4c07e-c0f1-4d85-8af0-3549e849e1b6/IQDjl-wiXVX2S64cOGBm1TvvAa6uxLWfPHBbsAVm9rbOtOc?e=RFUA78&nav=cz0lMkZjb250ZW50c3RvcmFnZSUyRkNTUF8xYmI0YzA3ZS1jMGYxLTRkODUtOGFmMC0zNTQ5ZTg0OWUxYjYmZD1iJTIxZnNDMEdfSEFoVTJLOERWSjZFbmh0bnBnLTFOc2hybEVxNU9OR2VpMXJELUhEYnpmUmY5SlQ2U2lmRTJVa3VnTiZmPTAxV05PRUJPN0RTN1dDRVhLVjZaRjI0SEJZTUJUTktPN1AmYz0lMkYmYT1Mb29wQXBwJnA9JTQwZmx1aWR4JTJGbG9vcC1wYWdlLWNvbnRhaW5lcg%3D%3D).

## Rollback

Set `EnableNativeDeviceCaManagementAppHandoff` to `false` in ECS.

Expected behavior after propagation:

- The separate Teams IP Phone compatibility branch remains unchanged because it runs before the flight check.
- Brokered WebCP Device CA requests stop management-owner detection and targeted native Re-WPJ launch.
- Requests return to the existing WebView/browser routing.
- App Link and generic HTTPS fallbacks that are specific to native routing are no longer attempted.
- No app release is required.

After rollback:

1. Confirm new non-compatibility spans report `is_native_device_ca_management_app_handoff_enabled=false`.
2. Confirm native outcomes stop for the affected app versions after ECS propagation; use both signals as rollback evidence.
3. Compare terminal routing failure rates with the stable default-on baseline.
4. Preserve the affected app-version, Android-version, and management-owner dimensions plus example correlation IDs for investigation.

## Post-release criteria to keep the default enabled

- No monitoring threshold breach during the first 14 days of the production release.
- At least 5,000 sampled Device CA spans with the effective native-routing gate reported as `true` across supported Android versions and management modes.
- No unexplained telemetry completeness gaps.
- Terminal routing failures remain within the thresholds above.
- Native handoff and fallback distributions remain stable by management owner and app version.
- Intune-owned management-mode validation is complete.
- No Sev 2 or higher IcM attributed to native Device CA management-app routing.
