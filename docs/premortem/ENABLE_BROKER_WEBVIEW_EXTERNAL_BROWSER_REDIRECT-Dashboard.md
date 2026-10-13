<!-- Copyright (c) Microsoft Corporation. All rights reserved. Licensed under the MIT License. -->

# Dashboard - `ENABLE_BROKER_WEBVIEW_EXTERNAL_BROWSER_REDIRECT`

## Data source

- **Cluster:** `https://idsharedeus2.kusto.windows.net/`
- **Database:** `ad-accounts-android-otel`
- **Table:** `android_spans`
- **Feature span:** `ProcessWebsiteRequest`
- **Sampling:** The Broker default configuration samples this span at 50%. All counts below
  are sampled spans, not total requests.

This dashboard uses Android Broker telemetry and monitors broker-hosted execution. The
client feature also applies to brokerless WebViews, but brokerless execution is not measured
by this dashboard.

The feature-specific `operation_outcome` column is not present in production until a build
containing the telemetry contract is deployed. The queries use `column_ifexists()` so the
dashboard remains valid and returns empty results before deployment.

Feature outcomes:

- `broker_external_browser_launched`: the marked URL opened externally.
- `broker_external_browser_launch_failed`: external launch failed and the URL continued in
  the WebView.

Before public preview, no more than 10 total marked requests per day are expected. Because
the feature span is sampled, do not multiply the sampled result to report an exact total or
treat an empty interval as proof of no usage.

## Tile 1: Daily sampled feature volume and launch outcomes

Use stacked columns for `launched` and `fell_back_to_webview`, with
`sampled_launch_success_pct` as a line.

```kusto
let feature_spans = android_spans
| where EventInfo_Time >= ago(30d)
| where span_name == "ProcessWebsiteRequest"
| extend operation_outcome =
    tostring(column_ifexists("operation_outcome", ""))
| where operation_outcome in (
    "broker_external_browser_launched",
    "broker_external_browser_launch_failed"
);
feature_spans
| summarize
    sampled_spans = count(),
    launched = countif(
        operation_outcome == "broker_external_browser_launched"),
    fell_back_to_webview = countif(
        operation_outcome == "broker_external_browser_launch_failed")
    by bin(EventInfo_Time, 1d)
| extend sampled_launch_success_pct =
    round(100.0 * launched / sampled_spans, 2)
| order by EventInfo_Time asc
```

## Tile 2: Outcomes by Broker host app version

Use this table to identify which Broker host versions receive marked traffic and whether
failures cluster on a version.

```kusto
let feature_spans = android_spans
| where EventInfo_Time >= ago(30d)
| where span_name == "ProcessWebsiteRequest"
| extend operation_outcome =
    tostring(column_ifexists("operation_outcome", ""))
| where operation_outcome in (
    "broker_external_browser_launched",
    "broker_external_browser_launch_failed"
);
feature_spans
| summarize
    sampled_spans = count(),
    launched = countif(
        operation_outcome == "broker_external_browser_launched"),
    fell_back_to_webview = countif(
        operation_outcome == "broker_external_browser_launch_failed")
    by AppInfo_Version
| extend sampled_launch_success_pct =
    round(100.0 * launched / sampled_spans, 2)
| order by sampled_spans desc
```

## Tile 3: Outcome and span-status consistency

The expected mappings are:

- `broker_external_browser_launched` to `OK`
- `broker_external_browser_launch_failed` to `ERROR`

Any nonzero `status_mismatches` value requires investigation.

```kusto
android_spans
| where EventInfo_Time >= ago(30d)
| where span_name == "ProcessWebsiteRequest"
| extend operation_outcome =
    tostring(column_ifexists("operation_outcome", ""))
| where operation_outcome in (
    "broker_external_browser_launched",
    "broker_external_browser_launch_failed"
)
| extend expected_status = iff(
    operation_outcome == "broker_external_browser_launched",
    "OK",
    "ERROR")
| summarize
    sampled_spans = count(),
    status_mismatches = countif(span_status != expected_status)
    by operation_outcome, span_status, expected_status
| extend mismatch_pct =
    round(100.0 * status_mismatches / sampled_spans, 2)
```

This query cannot detect a feature span that completely omitted `operation_outcome`,
because the outcome is also the attribute used to identify the feature span.

## Tile 4: Failed-launch WebView fallback drilldown

Review every row during the initial rollout. A row means external launch failed and the URL
continued in the WebView; it does not mean the authorization request was terminated.

```kusto
android_spans
| where EventInfo_Time >= ago(30d)
| where span_name == "ProcessWebsiteRequest"
| extend operation_outcome =
    tostring(column_ifexists("operation_outcome", ""))
| where operation_outcome ==
    "broker_external_browser_launch_failed"
| project
    EventInfo_Time,
    AppInfo_Version,
    calling_package_name,
    DeviceInfo_OsVersion,
    DeviceInfo_Model,
    span_status,
    correlation_id_v2,
    trace_id
| order by EventInfo_Time desc
| take 100
```

## Tile 5: Best-effort Broker-to-eSTS correlation and VID flight attribution

This cross-cluster query joins sampled feature spans to Android eSTS requests using
`correlation_id_v2 == CorrelationId`. It checks whether the eSTS request recorded
`EnableAndroidBrokerWebViewExternalBrowserRedirect` in `FeatureV2InRollOut` or
`FeatureV2Switches`. This tile includes only spans with an exported
`correlation_id_v2`. Successful spans without tenant context do not receive
`correlation_id_v2`, so they are intentionally absent from this tile even when the feature
span itself was exported. Empty results do not prove that no marked traffic occurred.

```kusto
let broker_feature = android_spans
| where EventInfo_Time >= ago(7d)
| where span_name == "ProcessWebsiteRequest"
| extend operation_outcome =
    tostring(column_ifexists("operation_outcome", ""))
| where operation_outcome in (
    "broker_external_browser_launched",
    "broker_external_browser_launch_failed"
)
| where isnotempty(correlation_id_v2)
    and correlation_id_v2 != "Scrubbed"
| project
    broker_time = EventInfo_Time,
    operation_outcome,
    AppInfo_Version,
    calling_package_name,
    trace_id,
    broker_correlation_id = tostring(correlation_id_v2);

let ests_android =
    cluster("https://estswus2.kusto.windows.net/")
        .database("ESTS").AllPerRequestTable
| where env_time >= ago(7d)
| where DevicePlatformForUI == "Android"
| extend vid_redirect_flight_present =
    FeatureV2InRollOut contains
        "EnableAndroidBrokerWebViewExternalBrowserRedirect"
    or FeatureV2Switches contains
        "EnableAndroidBrokerWebViewExternalBrowserRedirect"
| project
    ests_time = env_time,
    ests_correlation_id = tostring(CorrelationId),
    RequestId,
    Call,
    Result,
    ErrorCode,
    ScenarioId,
    vid_redirect_flight_present;

broker_feature
| join kind=leftouter hint.remote=right ests_android
    on $left.broker_correlation_id == $right.ests_correlation_id
| summarize
    ests_rows = countif(isnotempty(RequestId)),
    vid_redirect_flight_rows = countif(vid_redirect_flight_present),
    ests_calls = make_set(Call, 20),
    ests_results = make_set(Result, 20),
    ests_error_codes = make_set_if(
        ErrorCode,
        isnotempty(ErrorCode),
        20),
    ests_scenarios = make_set_if(
        ScenarioId,
        isnotempty(ScenarioId),
        20),
    first_ests_time = min(ests_time),
    last_ests_time = max(ests_time)
    by
        broker_time,
        operation_outcome,
        AppInfo_Version,
        calling_package_name,
        trace_id,
        broker_correlation_id
| extend vid_attribution = case(
    ests_rows == 0,
        "no_ests_match",
    vid_redirect_flight_rows > 0,
        "vid_redirect_flight_present",
    "ests_match_without_vid_redirect_flight")
| order by broker_time desc
| take 100
```

The query was validated against the production clusters before the feature was deployed.
No Android eSTS rows from the preceding seven days contained the new flight name, so empty
feature results are currently expected. After rollout:

- `vid_redirect_flight_present` confirms that at least one correlated eSTS request recorded
  the server flight.
- `ests_match_without_vid_redirect_flight` means the correlation ID matched eSTS, but the
  flight was not present in the queried feature fields.
- `no_ests_match` means no Android eSTS request was returned for the sampled correlation ID
  and time window.

## Tile 6: Directional authentication outcome after successful handoff

This query joins successful external launches to `AcquireTokenInteractive` by `trace_id`.
It is directional only: `missing_terminal_span` means telemetry-incomplete and is not
evidence that the authorization session was lost.

```kusto
let successful_handoffs = android_spans
| where EventInfo_Time >= ago(30d)
| where span_name == "ProcessWebsiteRequest"
| extend operation_outcome =
    tostring(column_ifexists("operation_outcome", ""))
| where operation_outcome ==
    "broker_external_browser_launched"
| summarize
    handoff_time = min(EventInfo_Time),
    broker_host_app_version = take_any(AppInfo_Version)
    by trace_id;

let terminal_auth = android_spans
| where EventInfo_Time >= ago(30d)
| where span_name == "AcquireTokenInteractive"
| summarize arg_max(
    EventInfo_Time,
    span_status,
    error_code,
    error_message)
    by trace_id
| project
    trace_id,
    auth_time = EventInfo_Time,
    auth_span_status = span_status,
    auth_error_code = error_code,
    auth_error_message = error_message;

successful_handoffs
| join kind=leftouter terminal_auth on trace_id
| extend auth_outcome = case(
    isempty(auth_time),
        "missing_terminal_span",
    auth_error_code in (
        "User cancelled",
        "auth_cancelled_by_sdk",
        "Broker request cancelled",
        "CANCELED")
        or tolower(auth_error_message) contains "cancel",
        "cancelled",
    auth_span_status == "ERROR",
        "error",
    auth_span_status == "OK",
        "success",
    "other")
| summarize sampled_traces = count()
    by bin(handoff_time, 1d), auth_outcome
| order by handoff_time asc
```

## Interpretation and response

- Inspect every sampled `broker_external_browser_launch_failed` row during the initial
  rollout.
- Use `correlation_id_v2`, when present, to confirm sampled requests against eSTS or VID
  telemetry.
- Do not classify `missing_terminal_span` as session loss.
- Do not report sampled counts as total devices or requests.
- If a sign-in regression is attributed to the client path, set
  `EnableBrokerWebViewExternalBrowserRedirect` to `false` in ECS.
