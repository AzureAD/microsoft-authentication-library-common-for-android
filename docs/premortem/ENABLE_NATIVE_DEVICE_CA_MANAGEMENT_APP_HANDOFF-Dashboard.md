<!-- Copyright (c) Microsoft Corporation. All rights reserved. Licensed under the MIT License. -->

# `ENABLE_NATIVE_DEVICE_CA_MANAGEMENT_APP_HANDOFF` dashboard setup

**Dashboard:** [New Android Broker Dashboard - Device CA native management-app handoff](https://dataexplorer.azure.com/dashboards/3f6536b7-62e6-407d-8071-31575adb78bb#627f67cd-0bfd-450e-8a81-f5e03758a4c3)

**Status:** Panels 1-6 below are configured on the dashboard.

**Cluster:** `https://idsharedeus2.kusto.windows.net/`

**Production database:** `ad-accounts-android-otel`

**Validation database:** `android-broker-otel-sandbox`

**Table:** `android_spans`

**Span:** `ProcessDeviceCaRequest`

> **Data-source note:** The production dashboard uses `ad-accounts-android-otel`. As of 2026-10-08, that database has no `ProcessDeviceCaRequest` spans, so these tiles correctly display no results. To inspect the pre-deployment test spans, temporarily run the query against `android-broker-otel-sandbox`, select Broker Host App Version `6.2610.6718`, and use a time range containing `2026-10-08 20:17 UTC`. Do not point the production monitoring tiles permanently at the sandbox database.

These query bodies were executed against the sandbox database on 2026-10-08 using local `let` values for the dashboard parameters. After correcting the version parameter to the dashboard's actual `_broker_host_app_version` name, all six queries were rerun successfully with both `["all"]` and `["6.2610.6718"]`. Panels 1-6 are now configured as tiles in the dashboard linked above.

At the time of the version-specific validation, sandbox contained two spans from app version `6.2610.6718`, both with the flight and WebCP enabled:

- `native_handoff_failed_app_link_fallback_succeeded`
- `webview_load_succeeded`

Both spans had `span_status=OK`, a terminal routing outcome, and `google_dpc_device_owner` as the detected management owner. Production contained no `ProcessDeviceCaRequest` spans at validation time, which is expected before a build containing the new telemetry is deployed.

The flight defaults to `true`. `is_native_device_ca_management_app_handoff_enabled` records the effective native-routing gate (`isRunningOnAuthService && flight enabled`), not the raw ECS value. Within this dashboard's measured brokered WebCP scope, normal production telemetry should report `true`; `false` is expected after the ECS kill switch is used. The Teams IP Phone compatibility path returns before evaluating this gate and is displayed as `not_evaluated`.

The queries use `column_ifexists()` because some newly added optional fallback columns will not be present in the production table schema until telemetry carrying those attributes is ingested.

## Dashboard parameters

Use the dashboard's existing `_startTime`, `_endTime`, and `_broker_host_app_version` parameters. `_broker_host_app_version` filters `AppInfo_Version` and already provides the Broker Host App Version selector shown on the dashboard; do not add a separate app-version parameter.

## Page description

**Device CA native management-app handoff monitoring**

This page monitors how Android Broker routes WebCP Device CA requests when `EnableNativeDeviceCaManagementAppHandoff` is enabled by default. The tiles verify that routing telemetry is complete, the correct management owner is detected, the targeted native Re-WPJ handoff is accepted, and the ordered App Link, generic HTTPS, WebView, or browser fallbacks remain available when an earlier route cannot be launched.

For this feature, a successful routing outcome means that Android accepted the intended management-app or fallback launch/load and the broker returned `MDM_FLOW`. The original authorization request then ends with `device_needs_to_be_managed`. Re-WPJ behavior inside the management app and later authentication attempts are outside this page's scope.

The page also highlights terminal broker-side routing failures, changes in fallback usage, missing terminal outcomes, and unexpected effective routing-gate state. `Flight=true` is normal for measured brokered WebCP traffic, `Flight=not_evaluated` is expected for the Teams IP Phone compatibility path, and `Flight=false` is expected after rollback.

## Panel 1 - Device CA span volume and telemetry completeness

**Visualization:** Line chart

**X axis:** `Day`

**Y axes:** `Spans`, `MissingOutcome`

**Series:** `Flight`

**Title:** `Device CA span volume and telemetry completeness`

```kusto
android_spans
| where EventInfo_Time between (_startTime .. _endTime)
| where _broker_host_app_version has "all"
    or AppInfo_Version in (_broker_host_app_version)
| where span_name == "ProcessDeviceCaRequest"
| extend Outcome = tostring(column_ifexists("device_ca_routing_outcome", "")),
         RawFlight = tolower(tostring(column_ifexists(
             "is_native_device_ca_management_app_handoff_enabled", "")))
| extend Flight = iff(
             Outcome == "legacy_company_portal_launch_succeeded",
             "not_evaluated",
             RawFlight)
| summarize Spans = count(),
            MissingOutcome = countif(isempty(Outcome))
  by Day = bin(EventInfo_Time, 1d), Flight
| order by Day asc, Flight asc
```

Expected during normal operation:

- `Flight` is `true` for measured brokered WebCP traffic.
- `Flight` is `not_evaluated` for the Teams IP Phone compatibility path.
- `MissingOutcome` is zero.
- `false` appears only after rollback.

Any other missing flight value or terminal outcome requires investigation before interpreting the other panels.

## Panel 2 - Device CA routing success rate

**Visualization:** Line chart

**X axis:** `Hour`

**Y axis:** `RoutingSuccessRatePct`

**Title:** `Device CA routing success rate`

```kusto
android_spans
| where EventInfo_Time between (_startTime .. _endTime)
| where _broker_host_app_version has "all"
    or AppInfo_Version in (_broker_host_app_version)
| where span_name == "ProcessDeviceCaRequest"
| extend Outcome = tostring(column_ifexists("device_ca_routing_outcome", ""))
| summarize Total = count(),
            TerminalFailures = countif(
                span_status =~ "ERROR"
                or Outcome in (
                    "browser_launch_failed",
                    "webview_load_failed",
                    "unexpected_routing_failure"))
  by Hour = bin(EventInfo_Time, 1h)
| extend RoutingSuccessRatePct =
    round(100.0 * (Total - TerminalFailures) / Total, 2)
| project Hour, Total, TerminalFailures, RoutingSuccessRatePct
| order by Hour asc
```

Investigate a decrease greater than 0.5 percentage points or a terminal-failure rate greater than 2x the stable baseline, sustained for one hour with at least 100 sampled spans.

## Panel 3 - Terminal routing outcome distribution

**Visualization:** Table

**Category:** `Outcome`

**Values:** `Count`, `SharePct`, `NativeHandoffSuccessPct`

**Split/filter:** `AppInfo_Version`, `Owner`

**Title:** `Device CA terminal routing outcomes`

```kusto
let Base = android_spans
| where EventInfo_Time between (_startTime .. _endTime)
| where _broker_host_app_version has "all"
    or AppInfo_Version in (_broker_host_app_version)
| where span_name == "ProcessDeviceCaRequest"
| extend Outcome = tostring(column_ifexists("device_ca_routing_outcome", "")),
         Owner = tostring(column_ifexists("device_ca_management_owner", ""));
let Counts = Base
| summarize Count = count() by Outcome, Owner, AppInfo_Version;
let Totals = Base
| summarize Total = count() by AppInfo_Version;
let Eligible = Base
| where Owner in (
    "company_portal_profile_owner",
    "google_dpc_profile_owner",
    "google_dpc_device_owner")
| summarize EligibleTotal = count(),
            NativeSuccess = countif(Outcome == "native_handoff_succeeded")
  by AppInfo_Version;
Counts
| join kind=inner Totals on AppInfo_Version
| join kind=leftouter Eligible on AppInfo_Version
| extend SharePct = round(100.0 * Count / Total, 2),
         NativeHandoffSuccessPct = iff(
             EligibleTotal > 0,
             round(100.0 * NativeSuccess / EligibleTotal, 2),
             real(null))
| project AppInfo_Version, Owner, Outcome, Count, SharePct,
          EligibleTotal, NativeSuccess, NativeHandoffSuccessPct
| order by AppInfo_Version asc, Owner asc, Count desc
```

Successful terminal outcomes include:

- `legacy_company_portal_launch_succeeded` - Teams IP Phone compatibility path only.
- `native_handoff_succeeded`
- `native_handoff_failed_app_link_fallback_succeeded`
- `native_handoff_app_link_failed_generic_https_fallback_succeeded`
- `browser_launch_succeeded`
- `webview_load_succeeded`

Failure outcomes are:

- `browser_launch_failed`
- `webview_load_failed`
- `unexpected_routing_failure`

Track the fallback share even when the terminal result succeeds. Investigate a fallback-share increase greater than 5 percentage points or 2x the stable baseline.

`NativeHandoffSuccessPct` uses only requests with a supported management owner as its denominator. Investigate a value below 95% when there are at least 100 eligible sampled spans.

## Panel 4 - Detected management owner

**Visualization:** Table or stacked bar chart

**Category:** `Owner`

**Value:** `Count`

**Split/filter:** `AppInfo_Version`

**Title:** `Device CA management owner distribution`

```kusto
android_spans
| where EventInfo_Time between (_startTime .. _endTime)
| where _broker_host_app_version has "all"
    or AppInfo_Version in (_broker_host_app_version)
| where span_name == "ProcessDeviceCaRequest"
| extend Owner = tostring(column_ifexists("device_ca_management_owner", ""))
| summarize Count = count() by Owner, AppInfo_Version
| order by AppInfo_Version asc, Count desc
```

Expected owner values:

- `company_portal_profile_owner`
- `google_dpc_profile_owner`
- `google_dpc_device_owner`
- `none`
- `device_policy_manager_unavailable`
- `not_evaluated`

`not_evaluated` is expected when the Teams IP Phone compatibility path succeeds or native routing is ineligible. Investigate unexpected `none`, `device_policy_manager_unavailable`, or a distribution shift concentrated in an app version, Android version, OEM, or management mode.

## Panel 5 - Fallback-stage outcomes

**Visualization:** Table or stacked bar chart

**Category:** `Stage`

**Series:** `Outcome`

**Value:** `Count`

**Title:** `Device CA fallback-stage outcomes`

```kusto
let Base = android_spans
| where EventInfo_Time between (_startTime .. _endTime)
| where _broker_host_app_version has "all"
    or AppInfo_Version in (_broker_host_app_version)
| where span_name == "ProcessDeviceCaRequest"
| extend AppLink = tostring(column_ifexists(
             "device_ca_management_app_link_outcome", "")),
         GenericHttps = tostring(column_ifexists(
             "device_ca_generic_https_launch_outcome", "")),
         WebView = tostring(column_ifexists(
             "device_ca_webview_load_outcome", ""));
union
    (Base
     | where isnotempty(AppLink)
     | project EventInfo_Time, Stage = "Package App Link",
               Outcome = AppLink, AppInfo_Version),
    (Base
     | where isnotempty(GenericHttps)
     | project EventInfo_Time, Stage = "Generic HTTPS",
               Outcome = GenericHttps, AppInfo_Version),
    (Base
     | where isnotempty(WebView)
     | project EventInfo_Time, Stage = "WebView",
               Outcome = WebView, AppInfo_Version)
| summarize Count = count() by Stage, Outcome, AppInfo_Version
| order by AppInfo_Version asc, Stage asc, Count desc
```

Possible package App Link outcomes:

- `launch_succeeded`
- `launch_failed`

Possible generic HTTPS outcomes:

- `handler_not_found`
- `launch_succeeded`
- `launch_failed`

Possible WebView outcomes:

- `load_succeeded`
- `load_failed`

The package App Link, generic HTTPS, and WebView attributes are stage-specific and may be empty when that stage was not attempted.

## Panel 6 - Terminal routing failures

**Visualization:** Table

**Columns:** `Count`, `Outcome`, `error_code`, `span_status_description`, `AppInfo_Version`

**Title:** `Device CA terminal routing failures`

```kusto
android_spans
| where EventInfo_Time between (_startTime .. _endTime)
| where _broker_host_app_version has "all"
    or AppInfo_Version in (_broker_host_app_version)
| where span_name == "ProcessDeviceCaRequest"
| extend Outcome = tostring(column_ifexists("device_ca_routing_outcome", ""))
| where span_status =~ "ERROR"
    or Outcome in (
        "browser_launch_failed",
        "webview_load_failed",
        "unexpected_routing_failure")
| summarize Count = count()
  by Outcome, error_code, span_status_description, AppInfo_Version
| order by Count desc
```

Any new terminal outcome or error concentration in one app version requires investigation. Do not make a rollback decision from fewer than 100 sampled spans in the selected segment unless the failure is deterministic or severe.

## Monitoring order

Review panels in this order:

1. **Volume and completeness** - verify sufficient sample size, `Flight=true`, and no missing terminal outcomes.
2. **Routing success rate** - check the broad guardrail.
3. **Terminal outcome distribution** - identify whether native launch or a fallback path changed.
4. **Management owner** - locate owner-detection changes.
5. **Fallback-stage outcomes** - identify the failing launch/load stage.
6. **Terminal failures** - inspect bounded failure categories by app version.

The Broker default sampling configuration currently samples `ProcessDeviceCaRequest` at 50%. Always display sampled counts next to percentages and avoid decisions based on sparse points.

## Rollback validation

After setting `EnableNativeDeviceCaManagementAppHandoff=false` in ECS:

1. Confirm new spans report `Flight=false`.
2. Confirm `native_handoff_succeeded` and native-specific App Link/generic HTTPS outcomes stop after ECS propagation.
3. Confirm requests use the legacy WebView/browser outcomes.
4. Compare terminal routing failures with the stable default-on baseline.
