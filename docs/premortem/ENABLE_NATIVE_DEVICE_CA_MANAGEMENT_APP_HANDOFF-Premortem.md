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

## Risks

1. **The wrong management app is selected or no owner is detected.** Android enterprise topology differs across work profile, fully managed, COBO, COPE, COSU, and OEM implementations. Incorrect owner detection can send remediation to the wrong package or leave a managed user on the fallback path.
2. **The fallback chain fails after native launch fails.** Package visibility, App Link verification, intent handlers, browser availability, or WebView loading can prevent every recovery route from succeeding.
3. **The flight affects an unintended flow.** Brokerless requests, non-WebCP requests, and ordinary external website redirects must not use native Device CA routing.
4. **Routing launches the management app more than once.** Repeated processing of the same Device CA request or lifecycle re-entry could open duplicate management-app activities before the authorization request ends.

## Can we measure failure?

**Yes.** The flight is responsible for selecting and launching the management app, executing the fallback chain when needed, and returning `MDM_FLOW` error code.

The `ProcessDeviceCaRequest` span records the flight gate, detected management owner, terminal routing outcome, and each fallback stage. It can detect owner-resolution changes, native launch failures, fallback concentration, and terminal broker-side failures.

The original authorization request ends when `MDM_FLOW` is returned and is converted to an `AuthorizationStatus.FAIL` / `device_needs_to_be_managed` error. What happens inside the management app after the accepted handoff, and any later authentication request, are outside this flight's monitoring contract.

Use these attributes as the monitoring contract:

| Signal | Attribute / values | Interpretation |
|---|---|---|
| Kill-switch state | `is_native_device_ca_management_app_handoff_enabled` = `true` / `false` | `true` is the normal default-on production state. `false` is expected only after the ECS kill switch is used. Also filter to brokered WebCP requests. |
| Management owner | `device_ca_management_owner` = `company_portal_profile_owner`, `google_dpc_profile_owner`, `google_dpc_device_owner`, `none`, `device_policy_manager_unavailable`, `not_evaluated` | Shows which package should receive the native handoff and exposes owner-detection gaps. |
| Terminal routing outcome | `device_ca_routing_outcome` | Primary routing health signal. |
| Native route success | `native_handoff_succeeded` | Android accepted the targeted native Re-WPJ launch. |
| Fallback success | `native_handoff_failed_app_link_fallback_succeeded`, `native_handoff_app_link_failed_generic_https_fallback_succeeded`, `webview_load_succeeded`, `browser_launch_succeeded` | The preferred route failed, but an immediate fallback was accepted. A rising fallback share is an early warning even when terminal routing succeeds. |
| Terminal broker failure | `browser_launch_failed`, `webview_load_failed`, `unexpected_routing_failure`, span status `ERROR`, or an exception | The broker could not hand the user to a recovery surface. |
| Package App Link result | `device_ca_management_app_link_outcome` = `launch_succeeded`, `launch_failed`, or `handler_not_found` | Diagnoses the first fallback after native handoff failure. |
| Generic HTTPS result | `device_ca_generic_https_launch_outcome` = `launch_succeeded`, `launch_failed`, or `handler_not_found` | Diagnoses the second fallback. |
| WebView result | `device_ca_webview_load_outcome` = `load_succeeded` or `load_failed` | Diagnoses the final in-app fallback. |
| Compatibility parsing | `device_ca_legacy_marker_parser_fallback_used` = `true` | Detects malformed or historical Device CA URLs that require substring parsing. |

## Monitoring baseline

For the default-on production release:

1. Use the six Device CA tiles in the [New Android Broker Dashboard](https://dataexplorer.azure.com/dashboards/3f6536b7-62e6-407d-8071-31575adb78bb#627f67cd-0bfd-450e-8a81-f5e03758a4c3) to monitor volume/completeness, routing success, terminal outcomes, management owner, fallback stages, and terminal failures. Use the existing Broker Host App Version parameter for release-specific filtering.
2. Establish a stable `is_native_device_ca_management_app_handoff_enabled=true` baseline for terminal routing failures, fallback outcomes, and Device CA request volume.
3. Validate that every sampled span has a terminal `device_ca_routing_outcome`.
4. Confirm that production telemetry reports the flight as `true` and appears only for brokered WebCP Device CA requests. `false` should appear only after rollback.
5. Use ad-hoc Kusto investigation to split a suspicious app-version or management-owner segment further by Android version, broker package, OEM, or management mode.
6. Do not make a rollback decision from fewer than 100 sampled Device CA spans in the selected app-version or management-owner segment. The Broker default sampling configuration currently samples `ProcessDeviceCaRequest` at 50%, so review sampled counts with every rate.

The dashboard uses the production `ad-accounts-android-otel` database. Before a build containing `ProcessDeviceCaRequest` telemetry reaches production, empty tiles are expected. Pre-deployment test spans are available only in `android-broker-otel-sandbox`.

## Monitoring plan

Review the [Device CA dashboard](https://dataexplorer.azure.com/dashboards/3f6536b7-62e6-407d-8071-31575adb78bb#627f67cd-0bfd-450e-8a81-f5e03758a4c3) at least twice daily for the first 14 days of the default-on production release. Review the six tiles in this order:

1. **Device CA span volume and telemetry completeness**
   - Compare `ProcessDeviceCaRequest` volume by app version.
   - Confirm `is_native_device_ca_management_app_handoff_enabled=true`; `false` is expected only after rollback.
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

1. Confirm new spans report `is_native_device_ca_management_app_handoff_enabled=false`.
2. Confirm native outcomes stop for the affected app versions after ECS propagation.
3. Compare terminal routing failure rates with the stable default-on baseline.
4. Preserve the affected app-version, Android-version, and management-owner dimensions plus example correlation IDs for investigation.

## Post-release criteria to keep the default enabled

- No monitoring threshold breach during the first 14 days of the production release.
- At least 5,000 sampled Device CA spans with the flight reported as `true` across supported Android versions and management modes.
- No unexplained telemetry completeness gaps.
- Terminal routing failures remain within the thresholds above.
- Native handoff and fallback distributions remain stable by management owner and app version.
- Intune-owned management-mode validation is complete.
- No Sev 2 or higher IcM attributed to native Device CA management-app routing.
