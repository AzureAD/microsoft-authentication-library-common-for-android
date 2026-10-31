<!-- Copyright (c) Microsoft Corporation. All rights reserved. Licensed under the MIT License. -->

# PKeyAuth SubmitUrl enforcement rollout dashboard

**Live dashboard:** [Android Broker - PKeyAuth SubmitUrl Enforcement Rollout](https://dataexplorer.azure.com/dashboards/29fd2473-feec-4410-8d8a-4a54bf9e5708)

**Premortem:** [Enforcement rollout](./ENFORCE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION-Premortem.md)

**Importable dashboard and exact KQL:** [Dashboard JSON](./ENFORCE_PKEYAUTH_SUBMIT_URL_ORIGIN_VALIDATION-Dashboard.json)

**Source:** `https://idsharedeus2.kusto.windows.net/` /
`ad-accounts-android-otel` / `android_spans`.

The dashboard is published, with Viewer access for the Android Broker Telemetry and Azure
Identity Android SDK groups. It contains two pages, nine query tiles, and two explanatory
tiles. It does not configure ECS, enable enforcement, or approve a rollout stage.

## Parameters and operation

| Parameter | Meaning |
|---|---|
| Time range | Defaults to the last 48 hours but can be changed freely. Every panel uses this selected range, with its end capped at `now()` to exclude future-dated device events. |
| Calling package / Broker host / Host app version | Apply identically to the current and reference populations. All means `dynamic(null)` and does not filter rows out. |

There is no Stage or Stage start control. The dashboard does not assume a rollout
percentage, enforce a minimum elapsed time, or truncate a longer review to 48/72 hours.
Choose the period to investigate, whether that is the last 15 minutes, several days, or
a custom interval. The premortem's rollout schedule is planning guidance, not a dependency
of these queries. A range spanning multiple rollout percentages reports the observed
enforced/shadow populations over that whole interval.

The comparison automatically uses contemporaneous shadow traffic from that same selected
range when any is present. If there is none, it uses the frozen pre-enforcement dataset
below, restricted to app-version / broker-host / caller keys present in the enforced
cohort. This also handles full enforcement without a manual 100% setting or a permanent
holdout. The reference source and coverage are shown in the result. A historical reference
later than the selected period is flagged rather than presented as a valid comparison.

This is a matched-segment operational comparison, not a causal or statistical equivalence
test. Inspect caller segments before expansion rather than relying only on the aggregate.

## Sample context, independent of rollout schedule

| Comparison | Sample guide for each current/reference population |
|---|---:|
| Rejection percentage | 500 evaluated challenges |
| Allowed-only parent errors, cancellations, and repeat proxy | 200 matched allowed-only parents |

Rates and counts remain visible even below these guides; `LOW_SAMPLE` warns against
overinterpreting a small population. It does not stop charts from rendering or require
waiting a preset number of hours. Missing references, incomplete attributes, ambiguous
parent cohorts, and low join/reference coverage are likewise reported as data-quality
context, not disguised as healthy results.

The comparison tile is a review aid, not a promotion engine. Actual rollout timing and
go/no-go decisions remain with the operator.

## Explicit rejection-rate gate

For the operator-selected review window and filters:

- `R = 100 * enforced REJECTED_* challenges / enforced evaluated challenges`.
- `B` is the same rate in the selected reference population.
- **Pause expansion for review when `R > max(B + 0.5 percentage points, 1.5 * B)`**,
  using at least 500 challenges in each population. The tile labels this
  `ABOVE_THRESHOLD`; it does not change exposure or enforce a waiting period.
- The October 26 descriptive 30-day rate was 2.027%; using that rate illustrates a
  threshold of 3.0405%. The live gate uses its actual selected reference, not this
  illustrative constant.

A rejection-rate breach alone means **pause and investigate**, not automatic rollback:
blocking an origin mismatch is the intended security behavior. Roll back enforcement for
a confirmed, attributable authentication or management regression, or when exposure
cannot be measured reliably. Set only `EnforcePKeyAuthSubmitUrlOriginValidation=false`
to preserve shadow observation.

## Parent outcomes and repeat proxy

Parent links use both `trace_id` and `parent_span_id`, deduplicate each parent, and match
`AcquireTokenInteractive`, `ATIInteractively`, or `WorkplaceJoin`. The parent lookup has
one hour of timing slack around the child window; links without a matching parent stay
visible as unmatched.

Expected security blocks are not themselves a compatibility regression. The quantitative
outcome gate therefore compares parents whose observed PKeyAuth children are **all
`ALLOWED`**, excluding parents with a rejected challenge. The diagnostic tile retains
both populations so intentional blocks and other errors can be examined separately.

The operational review thresholds are:

| Metric | Pause expansion when current rate exceeds |
|---|---|
| Error status or nonempty error code, among allowed-only parents | `max(reference + 0.1 percentage points, 1.25 * reference)` |
| Cancellation, among allowed-only parents | `max(reference + 0.1 percentage points, 1.25 * reference)` |
| Repeat share among sampled allowed-only parents | `max(reference + 5 percentage points, 1.25 * reference)` |

The outcome/repeat rows show a sample warning below 200 parents and a coverage warning
below 95% matched-parent coverage. When using the historical reference, less than 95% of
enforced challenges with a matching frozen-baseline key is also flagged. Observed rates
stay visible; missing references are not treated as a zero error or rejection rate.

Repeat share is `(parents - sampled device-days) / parents`, calculated within each
app-version / broker-host / caller segment. It is a same-day repeated-request proxy, not
proof that an app retried or a measurement of every physical-device attempt. Sampling and
partial days affect this proxy. The parent status/error metrics are likewise directional
signals, not a claim of end-to-end sign-in reliability.

## Frozen historical reference when no shadow traffic is selected

**Window:** `2026-10-28T00:00:00Z <= EventInfo_Time < 2026-10-29T00:00:00Z`.

This completed UTC day was captured on October 29 while enforcement was off. The
aggregate-only `FrozenBaseline` datatable is embedded directly in query 05 and the
importable JSON, so it remains usable after raw-table retention expires. No ingestion or
new server-side table is required.

| Aggregate before app/host/caller filtering | Count |
|---|---:|
| App-version / broker-host / caller keys | 286 |
| Evaluated shadow challenges | 93,099 |
| Rejected challenges | 1,715 (1.842%) |
| Matched, allowed-only parents | 65,465 |
| Allowed-only parents with error status or code | 49,390 |
| Allowed-only parents with cancellation code | 23,418 |
| Sampled device-days within those segments | 56,398 |
| Repeat parents within those device-days | 9,067 |

These are observed sampled counts. The high parent error/code share describes this
PKeyAuth-selected, sampled population; it is not the global Broker failure rate. The
historical comparison uses only matching keys, so its filtered rates can differ from these totals.
New versions or callers without adequate reference coverage require review rather than
an invented historical baseline.

## Panels

| Query | Panel | Primary purpose |
|---|---|---|
| 01 | Flight exposure and attribute completeness | Cohorts, sampled devices, missing attributes, unexpected verdicts, actual versus would-block events |
| 02 | Sampled challenge requests by hour | Request traffic and rejected/blocked volume |
| 03 | Sampled challenge devices by hour | Device traffic, separate from request counts |
| 04 | Validation verdicts | Every verdict split by enforcement state |
| 05 | Health comparison | Selected-window rates, reference source, thresholds, sample counts, and data-quality context; no rollout clock |
| 06 | Caller/app/platform breakdown | Caller, broker host/version, host app version, Android version, and manufacturer concentration |
| 07 | Origin shapes | Recorded/fallback origin source, main-frame flag, and validated/not-validated cloud flags |
| 08 | Deduplicated parent outcomes | Parent outcome codes, intentional-mismatch exclusion, cohort ambiguity, and unmatched links |
| 09 | Repeat proxy | Sampled allowed-only repeat share by day and cohort |

## Validation and query use

The nine panel queries and three remaining query-backed filters use the same time and
population bindings. The health-comparison revision is checked with short and multi-day
production windows and synthetic inputs for threshold boundaries, visible low-sample
rates, missing parents/attributes, shadow-only data, and no-shadow historical fallback.
Synthetic checks only execute in-memory KQL; they do not ingest events.

The earlier October 29 validation observed 184,231 sampled challenges in approximately 48 hours:
all were in shadow mode, 3,509 had an origin mismatch, and none had missing feature
attributes. Consequently, no actual enforced-cohort health result is claimed before ECS
exposure begins.

The exact panel queries appear below and match the checked-in JSON. In the ADX query
editor, prepend these bindings and choose the desired time range:

```kusto
let _startTime = ago(48h);
let _endTime = now();
let _caller = dynamic(null);
let _brokerHost = dynamic(null);
let _appVersion = dynamic(null);
```

<!-- BEGIN GENERATED QUERIES -->

### 01-exposure: Flight exposure and attribute completeness

```kusto
let WindowStart = _startTime;
let WindowEnd = min_of(_endTime, now());
let Base = materialize(
    android_spans
    | where EventInfo_Time between (WindowStart .. WindowEnd)
    | where span_name == "ProcessPKeyAuthChallenge"
    | where isnull(_appVersion) or AppInfo_Version in (_appVersion)
| where isnull(_brokerHost) or active_broker_package_name in (_brokerHost)
| where isnull(_caller) or calling_package_name in (_caller)
    | project EventInfo_Time, trace_id, ParentId = parent_span_id, DeviceInfo_Id,
        AppVersion = AppInfo_Version, BrokerHost = active_broker_package_name,
        Caller = calling_package_name, BrokerVersion = broker_version,
        AndroidVersion = DeviceInfo_OsVersion, Make = DeviceInfo_Make,
        Verdict = pkeyauth_submit_url_origin_validation_result,
        Enforced = pkeyauth_submit_url_origin_validation_enforced,
        OriginSource = pkeyauth_challenging_origin_source,
        MainFrame = pkeyauth_challenge_is_main_frame,
        SubmitValidated = pkeyauth_submit_host_is_aad_cloud,
        ChallengerValidated = pkeyauth_challenging_host_is_aad_cloud
    | extend Cohort = case(Enforced == true, "enforced", Enforced == false, "shadow", "unknown"),
        IsRejected = Verdict startswith_cs "REJECTED_",
        Complete = isnotempty(Verdict) and isnotnull(Enforced)
            and isnotempty(OriginSource) and isnotnull(MainFrame)
            and isnotnull(SubmitValidated) and isnotnull(ChallengerValidated)
);
Base
| summarize Challenges = count(), Devices = dcountif(DeviceInfo_Id, isnotempty(DeviceInfo_Id)),
    Rejected = countif(IsRejected), Blocked = countif(IsRejected and Enforced == true),
    MissingAttributes = countif(not(Complete)),
    UnexpectedVerdicts = countif(Verdict !in ("ALLOWED", "REJECTED_ORIGIN_MISMATCH",
        "REJECTED_SUBMIT_NOT_HTTPS", "REJECTED_ORIGIN_UNRESOLVABLE", "REJECTED_BACKSLASH_AUTHORITY"))
    by Cohort
| extend RejectedPct = round(100.0 * Rejected / Challenges, 3)
| order by Cohort asc
```

### 02-request-trend: Sampled challenge requests by hour

```kusto
let WindowStart = _startTime;
let WindowEnd = min_of(_endTime, now());
let Base = materialize(
    android_spans
    | where EventInfo_Time between (WindowStart .. WindowEnd)
    | where span_name == "ProcessPKeyAuthChallenge"
    | where isnull(_appVersion) or AppInfo_Version in (_appVersion)
| where isnull(_brokerHost) or active_broker_package_name in (_brokerHost)
| where isnull(_caller) or calling_package_name in (_caller)
    | project EventInfo_Time, trace_id, ParentId = parent_span_id, DeviceInfo_Id,
        AppVersion = AppInfo_Version, BrokerHost = active_broker_package_name,
        Caller = calling_package_name, BrokerVersion = broker_version,
        AndroidVersion = DeviceInfo_OsVersion, Make = DeviceInfo_Make,
        Verdict = pkeyauth_submit_url_origin_validation_result,
        Enforced = pkeyauth_submit_url_origin_validation_enforced,
        OriginSource = pkeyauth_challenging_origin_source,
        MainFrame = pkeyauth_challenge_is_main_frame,
        SubmitValidated = pkeyauth_submit_host_is_aad_cloud,
        ChallengerValidated = pkeyauth_challenging_host_is_aad_cloud
    | extend Cohort = case(Enforced == true, "enforced", Enforced == false, "shadow", "unknown"),
        IsRejected = Verdict startswith_cs "REJECTED_",
        Complete = isnotempty(Verdict) and isnotnull(Enforced)
            and isnotempty(OriginSource) and isnotnull(MainFrame)
            and isnotnull(SubmitValidated) and isnotnull(ChallengerValidated)
);
Base
| summarize Challenges = count(), Rejected = countif(IsRejected),
    Blocked = countif(IsRejected and Enforced == true) by Hour = bin(EventInfo_Time, 1h), Cohort
| order by Hour asc
```

### 03-device-trend: Sampled challenge devices by hour

```kusto
let WindowStart = _startTime;
let WindowEnd = min_of(_endTime, now());
let Base = materialize(
    android_spans
    | where EventInfo_Time between (WindowStart .. WindowEnd)
    | where span_name == "ProcessPKeyAuthChallenge"
    | where isnull(_appVersion) or AppInfo_Version in (_appVersion)
| where isnull(_brokerHost) or active_broker_package_name in (_brokerHost)
| where isnull(_caller) or calling_package_name in (_caller)
    | project EventInfo_Time, trace_id, ParentId = parent_span_id, DeviceInfo_Id,
        AppVersion = AppInfo_Version, BrokerHost = active_broker_package_name,
        Caller = calling_package_name, BrokerVersion = broker_version,
        AndroidVersion = DeviceInfo_OsVersion, Make = DeviceInfo_Make,
        Verdict = pkeyauth_submit_url_origin_validation_result,
        Enforced = pkeyauth_submit_url_origin_validation_enforced,
        OriginSource = pkeyauth_challenging_origin_source,
        MainFrame = pkeyauth_challenge_is_main_frame,
        SubmitValidated = pkeyauth_submit_host_is_aad_cloud,
        ChallengerValidated = pkeyauth_challenging_host_is_aad_cloud
    | extend Cohort = case(Enforced == true, "enforced", Enforced == false, "shadow", "unknown"),
        IsRejected = Verdict startswith_cs "REJECTED_",
        Complete = isnotempty(Verdict) and isnotnull(Enforced)
            and isnotempty(OriginSource) and isnotnull(MainFrame)
            and isnotnull(SubmitValidated) and isnotnull(ChallengerValidated)
);
Base
| summarize Devices = dcountif(DeviceInfo_Id, isnotempty(DeviceInfo_Id)),
    MismatchDevices = dcountif(DeviceInfo_Id, isnotempty(DeviceInfo_Id) and IsRejected)
    by Hour = bin(EventInfo_Time, 1h), Cohort
| order by Hour asc
```

### 04-verdicts: Validation verdicts (shadow versus enforced)

```kusto
let WindowStart = _startTime;
let WindowEnd = min_of(_endTime, now());
let Base = materialize(
    android_spans
    | where EventInfo_Time between (WindowStart .. WindowEnd)
    | where span_name == "ProcessPKeyAuthChallenge"
    | where isnull(_appVersion) or AppInfo_Version in (_appVersion)
| where isnull(_brokerHost) or active_broker_package_name in (_brokerHost)
| where isnull(_caller) or calling_package_name in (_caller)
    | project EventInfo_Time, trace_id, ParentId = parent_span_id, DeviceInfo_Id,
        AppVersion = AppInfo_Version, BrokerHost = active_broker_package_name,
        Caller = calling_package_name, BrokerVersion = broker_version,
        AndroidVersion = DeviceInfo_OsVersion, Make = DeviceInfo_Make,
        Verdict = pkeyauth_submit_url_origin_validation_result,
        Enforced = pkeyauth_submit_url_origin_validation_enforced,
        OriginSource = pkeyauth_challenging_origin_source,
        MainFrame = pkeyauth_challenge_is_main_frame,
        SubmitValidated = pkeyauth_submit_host_is_aad_cloud,
        ChallengerValidated = pkeyauth_challenging_host_is_aad_cloud
    | extend Cohort = case(Enforced == true, "enforced", Enforced == false, "shadow", "unknown"),
        IsRejected = Verdict startswith_cs "REJECTED_",
        Complete = isnotempty(Verdict) and isnotnull(Enforced)
            and isnotempty(OriginSource) and isnotnull(MainFrame)
            and isnotnull(SubmitValidated) and isnotnull(ChallengerValidated)
);
Base
| summarize Challenges = count(), Devices = dcountif(DeviceInfo_Id, isnotempty(DeviceInfo_Id))
    by Cohort, Verdict
| order by Challenges desc
```

### 05-health-comparison: Health comparison - selected time range

```kusto
let WindowStart = _startTime;
let WindowEnd = min_of(_endTime, now());
let Base = materialize(
    android_spans
    | where EventInfo_Time between (WindowStart .. WindowEnd)
    | where span_name == "ProcessPKeyAuthChallenge"
    | where isnull(_appVersion) or AppInfo_Version in (_appVersion)
| where isnull(_brokerHost) or active_broker_package_name in (_brokerHost)
| where isnull(_caller) or calling_package_name in (_caller)
    | project EventInfo_Time, trace_id, ParentId = parent_span_id, DeviceInfo_Id,
        AppVersion = AppInfo_Version, BrokerHost = active_broker_package_name,
        Caller = calling_package_name, BrokerVersion = broker_version,
        AndroidVersion = DeviceInfo_OsVersion, Make = DeviceInfo_Make,
        Verdict = pkeyauth_submit_url_origin_validation_result,
        Enforced = pkeyauth_submit_url_origin_validation_enforced,
        OriginSource = pkeyauth_challenging_origin_source,
        MainFrame = pkeyauth_challenge_is_main_frame,
        SubmitValidated = pkeyauth_submit_host_is_aad_cloud,
        ChallengerValidated = pkeyauth_challenging_host_is_aad_cloud
    | extend Cohort = case(Enforced == true, "enforced", Enforced == false, "shadow", "unknown"),
        IsRejected = Verdict startswith_cs "REJECTED_",
        Complete = isnotempty(Verdict) and isnotnull(Enforced)
            and isnotempty(OriginSource) and isnotnull(MainFrame)
            and isnotnull(SubmitValidated) and isnotnull(ChallengerValidated)
);
let Links = Base
    | where isnotempty(trace_id) and isnotempty(ParentId)
    | summarize EnforcedCount = countif(Cohort == "enforced"),
        ShadowCount = countif(Cohort == "shadow"), UnknownCount = countif(Cohort == "unknown"),
        HasMismatch = countif(IsRejected) > 0,
        arg_max(EventInfo_Time, DeviceInfo_Id, AppVersion, BrokerHost, Caller)
        by trace_id, ParentId
    | extend Cohort = case(UnknownCount > 0, "unknown",
        EnforcedCount > 0 and ShadowCount > 0, "mixed",
        EnforcedCount > 0, "enforced", "shadow");
let Parents = android_spans
    | where EventInfo_Time between (WindowStart - 1h .. min_of(WindowEnd + 1h, now()))
    | where span_name in ("AcquireTokenInteractive", "ATIInteractively", "WorkplaceJoin")
    | project EventInfo_Time, trace_id, ParentId = span_id, ParentName = span_name,
        ParentStatus = span_status, ParentError = error_code
    | summarize arg_max(EventInfo_Time, ParentName, ParentStatus, ParentError) by trace_id, ParentId;
let Linked = materialize(Links
    | join kind=leftouter hint.shufflekey=trace_id Parents on trace_id, ParentId
    | extend Matched = isnotempty(ParentName),
        ErrorOrCode = ParentStatus =~ "ERROR" or isnotempty(ParentError),
        Cancelled = tolower(ParentError) contains "cancel"
);
let ChallengeCounts = Base
    | summarize Challenges = count(), Rejected = countif(IsRejected) by Cohort;
let ParentCounts = Linked
    | summarize LinkedParents = count(), MatchedParents = countif(Matched),
        AllowedParents = countif(Matched and not(HasMismatch)),
        ErrorParents = countif(Matched and not(HasMismatch) and ErrorOrCode),
        CancelParents = countif(Matched and not(HasMismatch) and Cancelled)
        by Cohort;
let RetryCounts = Linked
    | where Matched and not(HasMismatch) and isnotempty(DeviceInfo_Id)
    | summarize Attempts = count() by Cohort, Day = bin(EventInfo_Time, 1d), DeviceInfo_Id,
        AppVersion, BrokerHost, Caller
    | summarize DeviceDays = count(), RepeatParents = sum(Attempts - 1) by Cohort;
let Observed = ChallengeCounts
    | join kind=leftouter ParentCounts on Cohort
    | join kind=leftouter RetryCounts on Cohort;
let FrozenBaseline = datatable(
    AppVersion:string, BrokerHost:string, Caller:string, Challenges:long, Rejected:long,
    AllowedParents:long, ErrorParents:long, CancelParents:long, DeviceDays:long, RepeatParents:long
)[
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.teams", 22944, 351, 17337, 13237, 7015, 14393, 2944,
    "6.2609.6214", "com.azure.authenticator", "com.azure.authenticator", 19012, 144, 10360, 8619, 4094, 8724, 1636,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.office.outlook", 15632, 252, 11999, 8139, 3413, 10781, 1218,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.teams", 7495, 157, 5535, 4166, 2335, 4509, 1026,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.office.outlook", 7133, 144, 5415, 3246, 1233, 4978, 437,
    "1.26101.103.0", "com.microsoft.appmanager", "com.azure.authenticator", 2602, 24, 1677, 1445, 824, 1371, 306,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.teams", 1747, 42, 1290, 987, 522, 1082, 208,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.office.outlook", 1697, 31, 1310, 810, 294, 1213, 97,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.windowsintune.companyportal", 1548, 149, 1237, 1235, 122, 1173, 64,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.emmx", 1404, 37, 972, 838, 546, 831, 141,
    "6.2610.7075", "com.azure.authenticator", "com.azure.authenticator", 890, 12, 492, 409, 200, 427, 65,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.teams", 868, 11, 634, 465, 243, 545, 89,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.office.outlook", 633, 8, 505, 332, 128, 448, 57,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.intune", 632, 16, 574, 569, 38, 549, 25,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.windowsintune.companyportal", 625, 40, 530, 530, 47, 509, 21,
    "1.26101.106.0", "com.microsoft.appmanager", "com.azure.authenticator", 617, 10, 397, 346, 195, 325, 72,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.office.officehubrow", 612, 7, 371, 314, 200, 339, 32,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.msapps", 595, 35, 426, 356, 160, 345, 81,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.skydrive", 577, 12, 334, 283, 181, 297, 37,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.emmx", 518, 11, 309, 265, 198, 238, 71,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.skydrive", 344, 14, 224, 174, 107, 203, 21,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.office.officehubrow", 287, 10, 193, 165, 98, 178, 15,
    "6.2609.6214", "com.azure.authenticator", "com.getvymo.android.intune", 285, 0, 237, 37, 27, 212, 25,
    "6.2609.6214", "com.azure.authenticator", "com.infosys.it.infosysitapp", 245, 0, 202, 202, 186, 119, 83,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.office.excel", 219, 3, 143, 119, 70, 136, 7,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.office.word", 204, 13, 128, 104, 60, 118, 10,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.rdc.androidx", 198, 5, 157, 44, 17, 134, 23,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.office.word", 183, 11, 127, 89, 54, 112, 15,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.msapps", 179, 9, 136, 109, 64, 100, 36,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.office.excel", 127, 4, 88, 74, 45, 81, 7,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.windowsintune.companyportal", 120, 7, 103, 103, 8, 101, 2,
    "6.2609.6214", "com.azure.authenticator", "com.samsung.android.email.provider", 119, 3, 85, 49, 23, 79, 6,
    "6.2609.6214", "com.azure.authenticator", "xdk.intel.cordova.template.eTMSBuddy", 112, 0, 62, 56, 49, 57, 5,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.scmx", 111, 5, 82, 75, 19, 81, 1,
    "1.26101.103.0", "com.microsoft.appmanager", "com.samsung.android.email.provider", 94, 4, 65, 38, 22, 58, 7,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.emmx", 92, 1, 68, 57, 41, 59, 9,
    "6.2609.6214", "com.azure.authenticator", "com.zebra.workcloud.shift", 82, 0, 58, 26, 16, 49, 9,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.windowsintune.companyportal", 79, 7, 65, 65, 5, 62, 3,
    "1.26101.103.0", "com.microsoft.appmanager", "com.sec.android.app.myfiles", 78, 0, 29, 25, 21, 28, 1,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.skydrive", 75, 1, 44, 29, 20, 42, 2,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.sharepoint", 71, 2, 50, 42, 19, 46, 4,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.powerbim", 71, 0, 53, 41, 18, 45, 8,
    "6.2609.6214", "com.azure.authenticator", "com.promobitech.mobilock.pro", 68, 0, 63, 63, 0, 59, 4,
    "6.2609.6214", "com.azure.authenticator", "com.airwatch.androidagent", 68, 2, 64, 64, 6, 62, 2,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.office.officehubrow", 67, 1, 37, 29, 18, 31, 6,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.todos", 64, 0, 42, 33, 16, 36, 6,
    "6.2609.6214", "com.azure.authenticator", "com.rim.mobilefusion.client", 60, 14, 37, 37, 6, 35, 2,
    "6.2609.6214", "com.azure.authenticator", "com.sec.android.app.myfiles", 56, 0, 29, 25, 21, 29, 0,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.emmx", 52, 0, 44, 41, 28, 35, 9,
    "6.2609.6214", "com.azure.authenticator", "com.samsung.android.knox.kam", 50, 49, 1, 0, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.sharepoint", 50, 2, 27, 23, 11, 23, 4,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.office.word", 45, 4, 28, 21, 12, 28, 0,
    "6.2609.6214", "com.azure.authenticator", "com.mobileiron.anyware.android", 43, 0, 42, 42, 3, 35, 7,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.skydrive", 42, 0, 23, 19, 8, 22, 1,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.msapps", 38, 0, 32, 26, 17, 21, 11,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.office.officehubrow", 37, 2, 21, 18, 12, 17, 4,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.office.excel", 37, 1, 26, 25, 21, 21, 5,
    "6.2609.6214", "com.azure.authenticator", "com.yammer.v1", 36, 0, 18, 17, 10, 13, 5,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.launcher.enterprise", 36, 16, 19, 11, 10, 17, 2,
    "1.26101.103.0", "com.microsoft.appmanager", "com.zebra.workcloud.shift", 31, 0, 30, 15, 12, 27, 3,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.office.powerpoint", 30, 1, 15, 12, 10, 13, 2,
    "6.2609.6214", "com.azure.authenticator", "com.scg.hqapp", 30, 0, 18, 2, 2, 16, 2,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.office.powerpoint", 29, 1, 18, 14, 10, 18, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.rdc.androidx", 27, 1, 21, 9, 5, 20, 1,
    "6.2609.6214", "com.azure.authenticator", "com.amazon.enterprise.access.android", 27, 0, 25, 25, 1, 25, 0,
    "6.2609.6214", "com.azure.authenticator", "ca.gc.forces.navymarine.myrcn", 26, 0, 23, 18, 1, 23, 0,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.intune", 24, 1, 23, 23, 1, 23, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.samsung.android.email.provider", 24, 1, 18, 16, 8, 14, 4,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.outlooklite", 23, 1, 10, 10, 7, 9, 1,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.scmx", 23, 2, 20, 20, 5, 20, 0,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.office.onenote", 22, 0, 19, 14, 3, 19, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.powerbim", 21, 2, 17, 14, 7, 15, 2,
    "1.26101.103.0", "com.microsoft.appmanager", "com.aisense.otter", 20, 0, 4, 4, 4, 3, 1,
    "6.2609.6214", "com.azure.authenticator", "com.nml.fxelectrodetest", 20, 0, 1, 1, 1, 1, 0,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.msapps", 16, 0, 14, 13, 4, 11, 3,
    "6.2609.6214", "com.azure.authenticator", "ca.forces.dmpap.caf.pay", 16, 0, 15, 5, 2, 12, 3,
    "6.2609.6214", "com.azure.authenticator", "com.zimperium.zips", 15, 0, 13, 13, 5, 12, 1,
    "6.2609.6214", "com.azure.authenticator", "com.axisbank.oneaxis.empappmanaged", 14, 0, 11, 11, 2, 10, 1,
    "1.26101.106.0", "com.microsoft.appmanager", "com.sec.android.app.myfiles", 14, 0, 6, 5, 5, 6, 0,
    "1.26101.106.0-beta", "com.microsoft.appmanager", "com.microsoft.teams", 13, 0, 11, 9, 6, 8, 3,
    "6.2609.6214", "com.azure.authenticator", "com.cognizant.onecognizant", 13, 0, 2, 2, 2, 1, 1,
    "1.26101.106.0-beta", "com.microsoft.appmanager", "com.azure.authenticator", 13, 0, 7, 7, 5, 6, 1,
    "6.2610.7075", "com.azure.authenticator", "com.samsung.android.email.provider", 12, 0, 5, 4, 3, 4, 1,
    "6.2609.6214", "com.azure.authenticator", "com.adobe.reader", 12, 0, 6, 5, 4, 5, 1,
    "6.2609.6214", "com.azure.authenticator", "com.talana.next", 12, 0, 2, 2, 2, 1, 1,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.scmx", 11, 0, 6, 6, 4, 4, 2,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.todos", 11, 0, 8, 5, 2, 8, 0,
    "1.26101.106.0-beta", "com.microsoft.appmanager", "com.microsoft.office.outlook", 11, 0, 11, 8, 0, 9, 2,
    "6.2609.6214", "com.azure.authenticator", "com.fiberlink.maas360.android.control", 10, 0, 10, 10, 1, 8, 2,
    "6.2609.6214", "com.azure.authenticator", "com.uhg.mobile.sparqgo", 10, 0, 8, 8, 1, 8, 0,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.crm.crmphone.fieldServices", 10, 0, 8, 2, 1, 8, 0,
    "6.2609.6214", "com.azure.authenticator", "com.zebra.mdna.els", 10, 0, 9, 2, 0, 9, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.crm.crmphone.fieldServices", 10, 0, 10, 6, 4, 9, 1,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.powerbim", 10, 0, 8, 7, 2, 8, 0,
    "6.2609.6214", "com.azure.authenticator", "com.axisbank.vision.biometric", 10, 2, 6, 6, 1, 4, 2,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.sharepoint", 9, 5, 2, 2, 0, 2, 0,
    "6.2610.7075", "com.azure.authenticator", "com.getvymo.android.intune", 9, 0, 9, 1, 1, 9, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.office.onenote", 9, 0, 7, 6, 1, 7, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.rdc.androidx", 8, 0, 8, 6, 3, 7, 1,
    "1.26101.103.0", "com.microsoft.appmanager", "com.infosys.it.infosysitapp", 8, 0, 8, 8, 8, 3, 5,
    "6.2609.6214", "com.azure.authenticator", "com.abm.prd.TeamConnectMAUI", 8, 0, 7, 3, 3, 5, 2,
    "1.26101.103.0", "com.microsoft.appmanager", "com.yammer.v1", 7, 0, 4, 3, 3, 4, 0,
    "6.2609.6214", "com.azure.authenticator", "com.statefarm.myoffice", 7, 0, 6, 6, 5, 1, 5,
    "1.26101.103.0", "com.microsoft.appmanager", "com.getvymo.android.intune", 7, 0, 7, 0, 0, 6, 1,
    "1.26101.103.0", "com.microsoft.appmanager", "ca.gc.forces.navymarine.myrcn", 7, 0, 7, 7, 0, 7, 0,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.skype.teams.ipphone", 7, 0, 6, 6, 0, 5, 1,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.office.powerpoint", 7, 1, 4, 2, 1, 4, 0,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.planner", 7, 0, 7, 3, 2, 5, 2,
    "1.26101.103.0", "com.microsoft.appmanager", "com.zimperium.zips", 6, 0, 6, 6, 1, 6, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.getvymo.android.intune", 6, 0, 3, 1, 1, 3, 0,
    "6.2609.6214", "com.azure.authenticator", "com.mobileiron", 6, 0, 6, 6, 1, 4, 2,
    "1.26101.106.0", "com.microsoft.appmanager", "com.zebra.workcloud.shift", 6, 0, 5, 3, 2, 5, 0,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.office.word", 6, 1, 4, 4, 3, 4, 0,
    "6.2610.7075", "com.azure.authenticator", "com.promobitech.mobilock.pro", 6, 0, 6, 6, 0, 6, 0,
    "6.2609.6214", "com.azure.authenticator", "com.ms.office365admin", 6, 1, 4, 3, 2, 4, 0,
    "6.2609.6214", "com.azure.authenticator", "com.ninefolders.hd3", 6, 0, 6, 1, 1, 6, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "ca.forces.dmpap.caf.pay", 6, 0, 5, 5, 0, 5, 0,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.rdc.androidx.beta", 6, 0, 4, 1, 0, 4, 0,
    "6.2609.6214", "com.azure.authenticator", "com.lumapps.android", 6, 0, 2, 2, 2, 1, 1,
    "6.2609.6214", "com.azure.authenticator", "com.axisbank.siddhi.v3", 5, 1, 4, 4, 1, 2, 2,
    "6.2609.6214", "com.azure.authenticator", "com.marksandspencer.yourmns", 5, 0, 5, 1, 0, 5, 0,
    "6.2609.6214", "com.azure.authenticator", "net.soti.mobicontrol.androidwork", 5, 0, 4, 3, 0, 4, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.yammer.v1", 5, 0, 3, 2, 0, 3, 0,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.azure", 5, 0, 3, 3, 1, 3, 0,
    "6.2609.6214", "com.azure.authenticator", "com.suth.onesutherland", 5, 0, 4, 1, 1, 4, 0,
    "6.2609.6214", "com.azure.authenticator", "com.good.gcs", 4, 0, 4, 4, 1, 4, 0,
    "6.2609.6214", "com.azure.authenticator", "com.Slack.intune", 4, 0, 4, 2, 1, 4, 0,
    "6.2610.7075", "com.azure.authenticator", "com.airwatch.androidagent", 4, 0, 4, 4, 0, 4, 0,
    "6.2609.6214", "com.azure.authenticator", "us.zoom.videomeetings4intune", 4, 0, 3, 3, 3, 2, 1,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.crm.crmphone.sales", 4, 0, 3, 3, 1, 3, 0,
    "1.26101.103.0-beta", "com.microsoft.appmanager", "com.microsoft.teams", 4, 0, 3, 3, 2, 3, 0,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.loop", 4, 0, 4, 3, 0, 4, 0,
    "6.2609.6214", "com.azure.authenticator", "ma.dista.axisbank", 4, 0, 2, 2, 0, 2, 0,
    "1.26101.106.0-beta", "com.microsoft.appmanager", "com.microsoft.emmx", 3, 0, 3, 1, 0, 3, 0,
    "6.2609.6214", "com.azure.authenticator", "com.axisbank.siddhi.cug", 3, 0, 3, 3, 3, 3, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.adani.aahluc", 3, 0, 3, 1, 1, 3, 0,
    "6.2609.6214", "com.azure.authenticator", "com.dcsg.relos", 3, 0, 3, 3, 3, 3, 0,
    "6.2609.6214", "com.azure.authenticator", "com.deloitte.dnmobile.trueview.store", 3, 0, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.lntecc.wisaworkernxt", 3, 0, 2, 0, 0, 2, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.loop", 3, 0, 2, 2, 1, 2, 0,
    "6.2609.6214", "com.azure.authenticator", "com.medtrix", 3, 0, 3, 0, 0, 3, 0,
    "6.2610.7075", "com.azure.authenticator", "com.mobileiron.anyware.android", 3, 0, 3, 3, 0, 3, 0,
    "6.2609.6214", "com.azure.authenticator", "com.hcl.hclempower", 3, 0, 3, 0, 0, 3, 0,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.dynamics.ProjectMadeira", 3, 0, 2, 0, 0, 2, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.todos", 3, 0, 3, 1, 1, 3, 0,
    "6.2609.6214", "com.azure.authenticator", "com.sidelineassist", 3, 0, 3, 2, 1, 3, 0,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.office.excel", 3, 0, 3, 1, 0, 3, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.statefarm.myoffice", 3, 0, 3, 3, 3, 2, 1,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.rdc.androidx.beta", 3, 0, 2, 0, 0, 1, 1,
    "6.2609.6214", "com.azure.authenticator", "com.pvcombank.mypvcb.production", 3, 0, 3, 0, 0, 1, 2,
    "6.2609.6214", "com.azure.authenticator", "com.samsung.android.app.reminder", 3, 0, 2, 2, 2, 2, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.office.onenote", 3, 2, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.centegy.deliveryapp", 3, 0, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.priavo.optum", 3, 0, 3, 1, 0, 3, 0,
    "6.2609.6214", "com.azure.authenticator", "net.phoneappli.people", 3, 0, 3, 3, 3, 3, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.scmx", 3, 2, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.clomo.android.mdm", 3, 2, 1, 1, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.Slack.intune", 3, 1, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.beaumanoir.rembo", 2, 0, 2, 1, 1, 2, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.reflexisinc.mywork17", 2, 0, 2, 1, 0, 2, 0,
    "6.2609.6214", "com.azure.authenticator", "au.com.axion.mywork", 2, 0, 2, 2, 1, 2, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.outlooklite", 2, 0, 2, 2, 1, 2, 0,
    "6.2609.6214", "com.azure.authenticator", "com.elearningforce.LMS", 2, 0, 2, 0, 0, 2, 0,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.rdc.androidx", 2, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.mitie.mijobsforte.internal", 2, 0, 2, 1, 1, 2, 0,
    "6.2609.6214", "com.azure.authenticator", "com.mboamersfoort.app", 2, 0, 2, 2, 1, 2, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "us.zoom.videomeetings4intune", 2, 0, 2, 2, 2, 1, 1,
    "6.2609.6214", "com.azure.authenticator", "com.fmr.mobile.workplaceexperience", 2, 0, 2, 2, 0, 2, 0,
    "6.2609.6214", "com.azure.authenticator", "com.abm.sit.TeamConnectMAUI", 2, 0, 2, 1, 1, 2, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "ca.forces.dmpap.caf.pay", 2, 0, 2, 2, 0, 2, 0,
    "6.2609.6214", "com.azure.authenticator", "com.pmi.store.PMIAPPM06901", 2, 0, 2, 1, 1, 2, 0,
    "6.2609.6214", "com.azure.authenticator", "com.parentsquare.psapp", 2, 0, 2, 0, 0, 2, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.adobe.reader", 2, 0, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.vodacom.truesa", 2, 0, 1, 1, 1, 1, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.gm.fpr", 2, 0, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "io.zygal.viewer", 2, 0, 2, 2, 2, 1, 1,
    "6.2609.6214", "com.azure.authenticator", "com.lntecc.WorkmanAnnexure", 2, 0, 2, 1, 0, 2, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.lookout.enterprise", 2, 0, 2, 2, 1, 2, 0,
    "6.2609.6214", "com.azure.authenticator", "co.switchapp", 2, 0, 2, 0, 0, 2, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.uhg.mobile.sparqgo", 2, 0, 2, 2, 0, 2, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.scg.hqapp", 2, 0, 2, 0, 0, 2, 0,
    "6.2609.6214", "com.azure.authenticator", "pe.compartamos.tcg.kallpacg", 2, 0, 2, 1, 0, 1, 1,
    "6.2609.6214", "com.azure.authenticator", "com.leadsquared.nextgen", 2, 0, 2, 0, 0, 2, 0,
    "6.2609.6214", "com.azure.authenticator", "com.app.wellable", 2, 0, 2, 2, 2, 2, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.infosys.it.infosysitapp", 2, 0, 2, 2, 0, 1, 1,
    "6.2609.6214", "com.azure.authenticator", "com.bluefletch.ems.emm.auth.ross", 2, 0, 2, 2, 1, 1, 1,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.intune.aospagent", 2, 0, 2, 2, 0, 2, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "xdk.intel.cordova.template.eTMSBuddy", 2, 0, 0, 0, 0, 0, 0,
    "6.2609.6214", "com.azure.authenticator", "com.workvivo.android", 2, 0, 0, 0, 0, 0, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.leadsquared.nextgen", 2, 0, 0, 0, 0, 0, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "xdk.intel.cordova.template.eTMSBuddy", 2, 0, 0, 0, 0, 0, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.sodexo.safetysalus", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.abi.interaction", 1, 0, 1, 1, 0, 1, 0,
    "6.2610.7075", "com.azure.authenticator", "com.clomo.android.mdm", 1, 0, 1, 1, 0, 1, 0,
    "6.2610.7075", "com.azure.authenticator", "se.awaio.awaioapp", 1, 0, 1, 1, 1, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.smartsheet.android", 1, 0, 1, 1, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.ringcentral.intune", 1, 0, 1, 1, 1, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.planner", 1, 0, 1, 1, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.axisbank.oneaxis.empappmanaged", 1, 0, 1, 1, 0, 1, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.uhg.mobile.sparqgo", 1, 0, 1, 1, 1, 1, 0,
    "6.2610.7075", "com.azure.authenticator", "com.sec.android.app.myfiles", 1, 0, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "co.switchapp.beta", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.nuraspace.app.android", 1, 0, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "br.com.zaruc.plmandroid.energisa.cloud", 1, 0, 1, 1, 0, 1, 0,
    "6.2610.7075", "com.azure.authenticator", "com.infosys.it.infosysitapp", 1, 0, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.mobileiron.android.emailplus", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.cxapp.mymutual", 1, 0, 1, 1, 0, 1, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "co.switchapp", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.earthsoft.equiscollect", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.symphony.android.messaging.intune", 1, 0, 1, 1, 1, 1, 0,
    "6.2610.7075", "com.azure.authenticator", "com.axisbank.siddhi.v3", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.mcafee.mvision", 1, 0, 1, 1, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "io.zynq.seating", 1, 0, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.bookkercorporate.b2b", 1, 0, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.sds.emm.cloud.knox.samsung", 1, 0, 1, 1, 0, 1, 0,
    "6.2610.7075", "com.azure.authenticator", "com.zebra.workcloud.shift", 1, 0, 1, 0, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.scg.hqapp", 1, 0, 1, 0, 0, 1, 0,
    "1.26101.106.0-beta", "com.microsoft.appmanager", "com.yammer.v1", 1, 0, 1, 0, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.ms.office365admin", 1, 0, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.nvoq.sayitdictationkeyboard", 1, 0, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.huawei.email", 1, 0, 1, 1, 1, 1, 0,
    "1.26101.103.0-beta", "com.microsoft.appmanager", "com.microsoft.emmx", 1, 0, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.kiem.app", 1, 0, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.moveinsync.ets", 1, 0, 1, 1, 0, 1, 0,
    "6.2610.7075", "com.azure.authenticator", "com.amazon.enterprise.access.android", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.office.officelens", 1, 0, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.reflexisinc.mywork17", 1, 0, 1, 0, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.magentatechnology.booking.emirates", 1, 0, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.lntgroup.wisastaff", 1, 0, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.android.email", 1, 0, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.dukeenergy.dukeemployee", 1, 0, 1, 0, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.emmx.canary", 1, 0, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.imprivata.imda", 1, 0, 1, 1, 1, 1, 0,
    "6.2610.7075", "com.azure.authenticator", "com.dukeenergy.dukeemployee", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.nvoq.dictationkeyboard", 1, 0, 1, 1, 0, 1, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "in.bankofbaroda.rm360", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "io.island.island.intune", 1, 0, 1, 1, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.centegy.deliveryapp2", 1, 0, 1, 1, 1, 1, 0,
    "6.2610.7075", "com.azure.authenticator", "com.microsoft.todos", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "jp.co.vinx.mdbohht_test", 1, 0, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.abb.smarttracker", 1, 0, 1, 1, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.moviuscorp.multilineforintune", 1, 0, 1, 1, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.airwatch.androidagent", 1, 0, 1, 1, 0, 1, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.crm.crmphone.fieldServices", 1, 0, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.dow.cfp.rls", 1, 0, 1, 1, 1, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.microsoft.dragoncopilot", 1, 0, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.ccamatil.id.roam.sales.oneroam", 1, 0, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.nix", 1, 0, 1, 1, 0, 1, 0,
    "6.2610.7075", "com.azure.authenticator", "com.scg.hqapp", 1, 0, 1, 0, 0, 1, 0,
    "1.26101.106.0-beta", "com.microsoft.appmanager", "com.zimperium.zips", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.nuraspace.app.android.ourspace", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.fractal.fms.production", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.squint.mobile.ar.prod", 1, 0, 1, 0, 0, 1, 0,
    "5.0.7139.0", "com.microsoft.windowsintune.companyportal", "com.microsoft.windowsintune.companyportal", 1, 0, 1, 1, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.efima.puhekirjaus.dev", 1, 0, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.gm.fpr", 1, 0, 1, 1, 1, 1, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "us.zoom.videomeetings4intune", 1, 0, 1, 1, 1, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.onfly.app", 1, 0, 1, 1, 1, 1, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.zimperium.zips", 1, 0, 1, 1, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.onfly.app", 1, 0, 1, 0, 0, 1, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.office.lync15", 1, 0, 1, 1, 0, 1, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.suth.onesutherland", 1, 0, 1, 0, 0, 1, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.dukeenergy.dukeemployee", 1, 0, 1, 0, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.ninefolders.hd3", 1, 1, 0, 0, 0, 0, 0,
    "6.2609.6214", "com.azure.authenticator", "com.exyteqor", 1, 0, 0, 0, 0, 0, 0,
    "6.2609.6214", "com.azure.authenticator", "com.talonsec.intune.talon", 1, 0, 0, 0, 0, 0, 0,
    "6.2609.6214", "com.azure.authenticator", "com.smartsheet.android", 1, 0, 0, 0, 0, 0, 0,
    "6.2609.6214", "com.azure.authenticator", "se.awaio.awaioapp", 1, 0, 1, 0, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.pmi.store.PMIAPPM06901", 1, 0, 0, 0, 0, 0, 0,
    "6.2610.7075", "com.azure.authenticator", "com.rim.mobilefusion.client", 1, 1, 0, 0, 0, 0, 0,
    "6.2610.7075", "com.azure.authenticator", "com.vodacom.truesa", 1, 0, 0, 0, 0, 0, 0,
    "6.2609.6214", "com.azure.authenticator", "com.iconicsuk.ibss.roamer", 1, 0, 0, 0, 0, 0, 0,
    "6.2609.6214", "com.azure.authenticator", "ua.com.whtech.selfie", 1, 0, 0, 0, 0, 0, 0,
    "6.2609.6214", "com.azure.authenticator", "com.unily.caterpillar.catwork", 1, 0, 1, 1, 0, 1, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.prompthrdenstsply", 1, 0, 0, 0, 0, 0, 0,
    "6.2609.6214", "com.azure.authenticator", "com.centegy.deliveryapp2", 1, 0, 0, 0, 0, 0, 0,
    "1.26101.106.0-beta", "com.microsoft.appmanager", "com.sec.android.app.myfiles", 1, 1, 0, 0, 0, 0, 0,
    "6.2610.7075", "com.azure.authenticator", "com.smartsheet.android", 1, 1, 0, 0, 0, 0, 0,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.emmx.canary", 1, 0, 0, 0, 0, 0, 0,
    "6.2609.6214", "com.azure.authenticator", "com.gobright.brightbooking", 1, 0, 0, 0, 0, 0, 0,
    "1.26101.106.0", "com.microsoft.appmanager", "com.microsoft.planner", 1, 0, 0, 0, 0, 0, 0,
    "1.26101.103.0", "com.microsoft.appmanager", "com.nuraspace.app.android", 1, 0, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.lookout.enterprise", 1, 0, 0, 0, 0, 0, 0,
    "6.2609.6214", "com.azure.authenticator", "com.microsoft.launcher", 1, 0, 1, 0, 0, 1, 0,
    "6.2609.6214", "com.azure.authenticator", "com.btg.pactual.autonomousagent.mobile", 1, 0, 0, 0, 0, 0, 0,
    "6.2609.6214", "com.azure.authenticator", "com.skyguard.s4h", 1, 0, 0, 0, 0, 0, 0
];
let ShadowChallenges = toscalar(Observed | where Cohort == "shadow" | summarize sum(Challenges));
let UseHistorical = ShadowChallenges == 0;
let FrozenSelected = FrozenBaseline
    | where isnull(_appVersion) or AppVersion in (_appVersion)
| where isnull(_brokerHost) or BrokerHost in (_brokerHost)
| where isnull(_caller) or Caller in (_caller);
let EnforcedKeys = Base | where Cohort == "enforced"
    | summarize KeyChallenges = count() by AppVersion, BrokerHost, Caller;
let Historical = FrozenSelected
    | join kind=inner EnforcedKeys on AppVersion, BrokerHost, Caller
    | summarize Challenges = sum(Challenges), Rejected = sum(Rejected),
        AllowedParents = sum(AllowedParents), ErrorParents = sum(ErrorParents),
        CancelParents = sum(CancelParents), DeviceDays = sum(DeviceDays),
        RepeatParents = sum(RepeatParents), CoveredChallenges = sum(KeyChallenges);
let Reference = union
    (Observed | where not(UseHistorical) and Cohort == "shadow"
        | project Challenges, Rejected, AllowedParents, ErrorParents, CancelParents,
            DeviceDays, RepeatParents),
    (Historical | where UseHistorical
        | project Challenges, Rejected, AllowedParents, ErrorParents, CancelParents,
            DeviceDays, RepeatParents);
let E = Observed | where Cohort == "enforced";
let EChallenges = toscalar(E | project Challenges);
let EParents = toscalar(E | project AllowedParents);
let RChallenges = toscalar(Reference | project Challenges);
let RParents = toscalar(Reference | project AllowedParents);
let ParentCoveragePct = toscalar(E | project 100.0 * MatchedParents / LinkedParents);
let HistoricalCoveragePct = 100.0 * toscalar(Historical | project CoveredChallenges) / EChallenges;
let Incomplete = toscalar(Base | summarize countif(not(Complete)));
let MixedParents = toscalar(Linked | summarize countif(Cohort in ("mixed", "unknown")));
let ERejectPct = 100.0 * toscalar(E | project Rejected) / EChallenges;
let RRejectPct = 100.0 * toscalar(Reference | project Rejected) / RChallenges;
let EErrorPct = 100.0 * toscalar(E | project ErrorParents) / EParents;
let RErrorPct = 100.0 * toscalar(Reference | project ErrorParents) / RParents;
let ECancelPct = 100.0 * toscalar(E | project CancelParents) / EParents;
let RCancelPct = 100.0 * toscalar(Reference | project CancelParents) / RParents;
let ERetryPct = 100.0 * toscalar(E | project RepeatParents) /
    toscalar(E | project DeviceDays + RepeatParents);
let RRetryPct = 100.0 * toscalar(Reference | project RepeatParents) /
    toscalar(Reference | project DeviceDays + RepeatParents);
union
    (print Metric = "Rejected challenges", CurrentPct = ERejectPct, ReferencePct = RRejectPct,
        ThresholdPct = max_of(RRejectPct + 0.5, 1.5 * RRejectPct),
        CurrentSamples = EChallenges, ReferenceSamples = RChallenges, SampleGuide = 500),
    (print Metric = "Allowed-only parent errors/codes", CurrentPct = EErrorPct, ReferencePct = RErrorPct,
        ThresholdPct = max_of(RErrorPct + 0.1, 1.25 * RErrorPct),
        CurrentSamples = EParents, ReferenceSamples = RParents, SampleGuide = 200),
    (print Metric = "Allowed-only parent cancellations", CurrentPct = ECancelPct, ReferencePct = RCancelPct,
        ThresholdPct = max_of(RCancelPct + 0.1, 1.25 * RCancelPct),
        CurrentSamples = EParents, ReferenceSamples = RParents, SampleGuide = 200),
    (print Metric = "Repeat share (sampled device-day proxy)", CurrentPct = ERetryPct, ReferencePct = RRetryPct,
        ThresholdPct = max_of(RRetryPct + 5.0, 1.25 * RRetryPct),
        CurrentSamples = EParents, ReferenceSamples = RParents, SampleGuide = 200)
| extend WindowStart, WindowEnd,
    ReferenceSource = iff(UseHistorical, "Frozen 2026-10-28 UTC shadow (matching app/host/caller keys)", "Contemporaneous shadow; same selected window and filters"),
    ParentCoveragePct,
    HistoricalCoveragePct = iff(UseHistorical, HistoricalCoveragePct, real(null)),
    State = case(isnull(EChallenges) or EChallenges == 0, "NO_ENFORCED_DATA",
        Incomplete > 0 or MixedParents > 0, "INCOMPLETE_TELEMETRY",
        isnull(ReferenceSamples) or ReferenceSamples == 0, "NO_REFERENCE_DATA",
        isnull(CurrentSamples) or CurrentSamples < SampleGuide or ReferenceSamples < SampleGuide, "LOW_SAMPLE",
        UseHistorical and WindowEnd < datetime(2026-10-29T00:00:00Z), "REFERENCE_AFTER_SELECTED_WINDOW",
        UseHistorical and (isnull(HistoricalCoveragePct) or HistoricalCoveragePct < 95.0), "LOW_REFERENCE_COVERAGE",
        Metric != "Rejected challenges" and (isnull(ParentCoveragePct) or ParentCoveragePct < 95.0), "LOW_PARENT_COVERAGE",
        isnull(CurrentPct) or isnull(ReferencePct), "NOT_EVALUABLE",
        CurrentPct > ThresholdPct, "ABOVE_THRESHOLD", "WITHIN_THRESHOLD")
| project Metric, State, WindowStart, WindowEnd, CurrentSamples, ReferenceSamples, SampleGuide,
    CurrentPct = round(CurrentPct, 3), ReferencePct = round(ReferencePct, 3),
    ThresholdPct = round(ThresholdPct, 3), ParentCoveragePct, HistoricalCoveragePct, ReferenceSource
```

### 06-caller-breakdown: Caller, app, platform and broker breakdown

```kusto
let WindowStart = _startTime;
let WindowEnd = min_of(_endTime, now());
let Base = materialize(
    android_spans
    | where EventInfo_Time between (WindowStart .. WindowEnd)
    | where span_name == "ProcessPKeyAuthChallenge"
    | where isnull(_appVersion) or AppInfo_Version in (_appVersion)
| where isnull(_brokerHost) or active_broker_package_name in (_brokerHost)
| where isnull(_caller) or calling_package_name in (_caller)
    | project EventInfo_Time, trace_id, ParentId = parent_span_id, DeviceInfo_Id,
        AppVersion = AppInfo_Version, BrokerHost = active_broker_package_name,
        Caller = calling_package_name, BrokerVersion = broker_version,
        AndroidVersion = DeviceInfo_OsVersion, Make = DeviceInfo_Make,
        Verdict = pkeyauth_submit_url_origin_validation_result,
        Enforced = pkeyauth_submit_url_origin_validation_enforced,
        OriginSource = pkeyauth_challenging_origin_source,
        MainFrame = pkeyauth_challenge_is_main_frame,
        SubmitValidated = pkeyauth_submit_host_is_aad_cloud,
        ChallengerValidated = pkeyauth_challenging_host_is_aad_cloud
    | extend Cohort = case(Enforced == true, "enforced", Enforced == false, "shadow", "unknown"),
        IsRejected = Verdict startswith_cs "REJECTED_",
        Complete = isnotempty(Verdict) and isnotnull(Enforced)
            and isnotempty(OriginSource) and isnotnull(MainFrame)
            and isnotnull(SubmitValidated) and isnotnull(ChallengerValidated)
);
Base
| summarize Challenges = count(), Rejected = countif(IsRejected),
    Devices = dcountif(DeviceInfo_Id, isnotempty(DeviceInfo_Id)),
    MismatchDevices = dcountif(DeviceInfo_Id, isnotempty(DeviceInfo_Id) and IsRejected)
    by Cohort, Caller, AppVersion, BrokerHost, BrokerVersion, AndroidVersion, Make
| extend RejectedPct = round(100.0 * Rejected / Challenges, 3)
| order by Rejected desc, Challenges desc
```

### 07-origin-shapes: Origin source, frame and cloud-validation flags

```kusto
let WindowStart = _startTime;
let WindowEnd = min_of(_endTime, now());
let Base = materialize(
    android_spans
    | where EventInfo_Time between (WindowStart .. WindowEnd)
    | where span_name == "ProcessPKeyAuthChallenge"
    | where isnull(_appVersion) or AppInfo_Version in (_appVersion)
| where isnull(_brokerHost) or active_broker_package_name in (_brokerHost)
| where isnull(_caller) or calling_package_name in (_caller)
    | project EventInfo_Time, trace_id, ParentId = parent_span_id, DeviceInfo_Id,
        AppVersion = AppInfo_Version, BrokerHost = active_broker_package_name,
        Caller = calling_package_name, BrokerVersion = broker_version,
        AndroidVersion = DeviceInfo_OsVersion, Make = DeviceInfo_Make,
        Verdict = pkeyauth_submit_url_origin_validation_result,
        Enforced = pkeyauth_submit_url_origin_validation_enforced,
        OriginSource = pkeyauth_challenging_origin_source,
        MainFrame = pkeyauth_challenge_is_main_frame,
        SubmitValidated = pkeyauth_submit_host_is_aad_cloud,
        ChallengerValidated = pkeyauth_challenging_host_is_aad_cloud
    | extend Cohort = case(Enforced == true, "enforced", Enforced == false, "shadow", "unknown"),
        IsRejected = Verdict startswith_cs "REJECTED_",
        Complete = isnotempty(Verdict) and isnotnull(Enforced)
            and isnotempty(OriginSource) and isnotnull(MainFrame)
            and isnotnull(SubmitValidated) and isnotnull(ChallengerValidated)
);
Base
| extend SubmitCloud = case(SubmitValidated == true, "validated AAD", SubmitValidated == false,
        "not validated / unknown", "missing"),
    ChallengerCloud = case(ChallengerValidated == true, "validated AAD", ChallengerValidated == false,
        "not validated / unknown", "missing")
| summarize Challenges = count(), Devices = dcountif(DeviceInfo_Id, isnotempty(DeviceInfo_Id))
    by Cohort, Verdict, OriginSource, MainFrame, SubmitCloud, ChallengerCloud
| order by Challenges desc
```

### 08-parent-outcomes: Deduplicated parent outcomes and join coverage

```kusto
let WindowStart = _startTime;
let WindowEnd = min_of(_endTime, now());
let Base = materialize(
    android_spans
    | where EventInfo_Time between (WindowStart .. WindowEnd)
    | where span_name == "ProcessPKeyAuthChallenge"
    | where isnull(_appVersion) or AppInfo_Version in (_appVersion)
| where isnull(_brokerHost) or active_broker_package_name in (_brokerHost)
| where isnull(_caller) or calling_package_name in (_caller)
    | project EventInfo_Time, trace_id, ParentId = parent_span_id, DeviceInfo_Id,
        AppVersion = AppInfo_Version, BrokerHost = active_broker_package_name,
        Caller = calling_package_name, BrokerVersion = broker_version,
        AndroidVersion = DeviceInfo_OsVersion, Make = DeviceInfo_Make,
        Verdict = pkeyauth_submit_url_origin_validation_result,
        Enforced = pkeyauth_submit_url_origin_validation_enforced,
        OriginSource = pkeyauth_challenging_origin_source,
        MainFrame = pkeyauth_challenge_is_main_frame,
        SubmitValidated = pkeyauth_submit_host_is_aad_cloud,
        ChallengerValidated = pkeyauth_challenging_host_is_aad_cloud
    | extend Cohort = case(Enforced == true, "enforced", Enforced == false, "shadow", "unknown"),
        IsRejected = Verdict startswith_cs "REJECTED_",
        Complete = isnotempty(Verdict) and isnotnull(Enforced)
            and isnotempty(OriginSource) and isnotnull(MainFrame)
            and isnotnull(SubmitValidated) and isnotnull(ChallengerValidated)
);
let Links = Base
    | where isnotempty(trace_id) and isnotempty(ParentId)
    | summarize EnforcedCount = countif(Cohort == "enforced"),
        ShadowCount = countif(Cohort == "shadow"), UnknownCount = countif(Cohort == "unknown"),
        HasMismatch = countif(IsRejected) > 0,
        arg_max(EventInfo_Time, DeviceInfo_Id, AppVersion, BrokerHost, Caller)
        by trace_id, ParentId
    | extend Cohort = case(UnknownCount > 0, "unknown",
        EnforcedCount > 0 and ShadowCount > 0, "mixed",
        EnforcedCount > 0, "enforced", "shadow");
let Parents = android_spans
    | where EventInfo_Time between (WindowStart - 1h .. min_of(WindowEnd + 1h, now()))
    | where span_name in ("AcquireTokenInteractive", "ATIInteractively", "WorkplaceJoin")
    | project EventInfo_Time, trace_id, ParentId = span_id, ParentName = span_name,
        ParentStatus = span_status, ParentError = error_code
    | summarize arg_max(EventInfo_Time, ParentName, ParentStatus, ParentError) by trace_id, ParentId;
let Linked = materialize(Links
    | join kind=leftouter hint.shufflekey=trace_id Parents on trace_id, ParentId
    | extend Matched = isnotempty(ParentName),
        ErrorOrCode = ParentStatus =~ "ERROR" or isnotempty(ParentError),
        Cancelled = tolower(ParentError) contains "cancel"
);
Linked
| summarize Parents = count(), ErrorsOrCodes = countif(ErrorOrCode),
    Cancellations = countif(Cancelled) by Cohort, Matched, HasMismatch, ParentName, ParentStatus, ParentError
| order by Parents desc
```

### 09-repeat-proxy: Allowed-only repeat share (sampled device-day proxy)

```kusto
let WindowStart = _startTime;
let WindowEnd = min_of(_endTime, now());
let Base = materialize(
    android_spans
    | where EventInfo_Time between (WindowStart .. WindowEnd)
    | where span_name == "ProcessPKeyAuthChallenge"
    | where isnull(_appVersion) or AppInfo_Version in (_appVersion)
| where isnull(_brokerHost) or active_broker_package_name in (_brokerHost)
| where isnull(_caller) or calling_package_name in (_caller)
    | project EventInfo_Time, trace_id, ParentId = parent_span_id, DeviceInfo_Id,
        AppVersion = AppInfo_Version, BrokerHost = active_broker_package_name,
        Caller = calling_package_name, BrokerVersion = broker_version,
        AndroidVersion = DeviceInfo_OsVersion, Make = DeviceInfo_Make,
        Verdict = pkeyauth_submit_url_origin_validation_result,
        Enforced = pkeyauth_submit_url_origin_validation_enforced,
        OriginSource = pkeyauth_challenging_origin_source,
        MainFrame = pkeyauth_challenge_is_main_frame,
        SubmitValidated = pkeyauth_submit_host_is_aad_cloud,
        ChallengerValidated = pkeyauth_challenging_host_is_aad_cloud
    | extend Cohort = case(Enforced == true, "enforced", Enforced == false, "shadow", "unknown"),
        IsRejected = Verdict startswith_cs "REJECTED_",
        Complete = isnotempty(Verdict) and isnotnull(Enforced)
            and isnotempty(OriginSource) and isnotnull(MainFrame)
            and isnotnull(SubmitValidated) and isnotnull(ChallengerValidated)
);
let Links = Base
    | where isnotempty(trace_id) and isnotempty(ParentId)
    | summarize EnforcedCount = countif(Cohort == "enforced"),
        ShadowCount = countif(Cohort == "shadow"), UnknownCount = countif(Cohort == "unknown"),
        HasMismatch = countif(IsRejected) > 0,
        arg_max(EventInfo_Time, DeviceInfo_Id, AppVersion, BrokerHost, Caller)
        by trace_id, ParentId
    | extend Cohort = case(UnknownCount > 0, "unknown",
        EnforcedCount > 0 and ShadowCount > 0, "mixed",
        EnforcedCount > 0, "enforced", "shadow");
let Parents = android_spans
    | where EventInfo_Time between (WindowStart - 1h .. min_of(WindowEnd + 1h, now()))
    | where span_name in ("AcquireTokenInteractive", "ATIInteractively", "WorkplaceJoin")
    | project EventInfo_Time, trace_id, ParentId = span_id, ParentName = span_name,
        ParentStatus = span_status, ParentError = error_code
    | summarize arg_max(EventInfo_Time, ParentName, ParentStatus, ParentError) by trace_id, ParentId;
let Linked = materialize(Links
    | join kind=leftouter hint.shufflekey=trace_id Parents on trace_id, ParentId
    | extend Matched = isnotempty(ParentName),
        ErrorOrCode = ParentStatus =~ "ERROR" or isnotempty(ParentError),
        Cancelled = tolower(ParentError) contains "cancel"
);
Linked
| where Matched and not(HasMismatch) and Cohort in ("enforced", "shadow") and isnotempty(DeviceInfo_Id)
| summarize Attempts = count() by Cohort, Day = bin(EventInfo_Time, 1d), DeviceInfo_Id,
    AppVersion, BrokerHost, Caller
| summarize Parents = sum(Attempts), DeviceDays = count(), RepeatParents = sum(Attempts - 1) by Cohort, Day
| extend RepeatSharePct = round(100.0 * RepeatParents / Parents, 3)
| order by Day asc, Cohort asc
```

<!-- END GENERATED QUERIES -->
