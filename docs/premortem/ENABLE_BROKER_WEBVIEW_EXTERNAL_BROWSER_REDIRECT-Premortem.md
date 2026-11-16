<!-- Copyright (c) Microsoft Corporation. All rights reserved. Licensed under the MIT License. -->

# Premortem - `ENABLE_BROKER_WEBVIEW_EXTERNAL_BROWSER_REDIRECT`

**Owner:** Sowmya Malayanur  |  **Approver:** TBD

**Common PR:** [AzureAD/microsoft-authentication-library-common-for-android#3304](https://github.com/AzureAD/microsoft-authentication-library-common-for-android/pull/3304)  |  **Work item:** [AB#3781149](https://identitydivision.visualstudio.com/Engineering/_workitems/edit/3781149)

**Monitoring dashboard:** [ENABLE_BROKER_WEBVIEW_EXTERNAL_BROWSER_REDIRECT-Dashboard.md](./ENABLE_BROKER_WEBVIEW_EXTERNAL_BROWSER_REDIRECT-Dashboard.md)

`ENABLE_BROKER_WEBVIEW_EXTERNAL_BROWSER_REDIRECT` defaults to `true`. Set
`EnableBrokerWebViewExternalBrowserRedirect` to `false` in ECS to stop marked URLs from
opening externally and restore the existing WebView behavior without an app release.

| Flight | ON does |
|---|---|
| `ENABLE_BROKER_WEBVIEW_EXTERNAL_BROWSER_REDIRECT` (`EnableBrokerWebViewExternalBrowserRedirect`) | In brokered and brokerless WebViews, recognizes an HTTPS destination whose `broker_webview_external_browser` query parameter has the literal value `1` and opens the supplied URL through an external `ACTION_VIEW` intent. Unmarked URLs and marker values other than `1` retain their existing behavior. |

## Intended routing

1. Preserve all existing special-protocol, redirect URI, install, WebCP, app-link, and
   non-HTTPS handling that runs before this feature.
2. Continue through existing WebView behavior when the client flight is disabled or the
   marker value is not the literal `1`.
3. For an eligible HTTPS URL, stop the pending WebView load.
4. Pass the supplied URL unchanged to an external `ACTION_VIEW` intent.
5. If an external handler accepts the intent, preserve the original authorization session.
   Opening the external activity does not complete or cancel authentication.
6. If no external handler can launch, continue loading the supplied URL in the WebView.

## Risks, completed validation, and rollback
This is a new client path for a new Verified ID (VID) server-side feature. The server adds
`broker_webview_external_browser=1` only to URLs for that feature, and the client changes
behavior only when that exact marker is present. Existing unmarked sign-in paths retain
their current behavior, so the likelihood of this change affecting an existing sign-in
path is very low and the overall client change is low risk.

1. **The marker affects an unintended navigation**
   - **Likelihood:** Very low.
   - **Why it is bounded:** The path requires both the default-on client flight and an HTTPS
     URL with the exact query value `broker_webview_external_browser=1`. Substring or
     non-literal values do not enter this path. Before public preview is announced, no more
     than 10 marked requests per day are expected; GA is planned for February.
   - **Production monitoring and rollback:** Sample feature spans by their feature-specific
     outcome. Where `correlation_id_v2` is present, cross-reference it with eSTS or VID
     telemetry to confirm that the request belongs to the new VID feature. The correlation
     ID alone does not identify the server scenario. If a sign-in regression is attributed
     to this client path, disable the client flight in ECS to restore existing WebView
     routing.

2. **No external handler is available or Android denies the launch**
   - **Likelihood:** Low
   - **Why it is bounded:** Before launching, the client checks whether Android can find an
     app capable of handling the HTTPS URL. It also handles cases where the app becomes
     unavailable or the launch is blocked. In either case, the client records a failed
     launch and continues loading the original URL in the WebView.
   - **Production monitoring and rollback:** Monitor
     `operation_outcome=broker_external_browser_launch_failed`. Disable the client flight
     in ECS if failures are confirmed to be caused by client-side external launch.

3. **The external activity launches but the original authorization session is lost**
   - **Likelihood:** Low to medium because lifecycle behavior varies with memory pressure,
     activity launch mode, OEM process management, and flow-specific continuation.
   - **Why it is bounded:** This path does not invoke the authorization completion callback
     after a successful launch. The original activity and WebView are not explicitly
     finished by the feature.

   - **Production monitoring and rollback:** Join redirect success spans to later
     `AcquireTokenInteractive` spans by `trace_id` for directional investigation of
     cancellation or error. A missing terminal span is telemetry-incomplete and is not
     evidence of session loss. If session loss is reproducible, disable the client flight
     in ECS.


## Rollout decision

Keep the client flight defaulted to `true`; do not use a percentage-based client rollout.

- The client and eSTS changes ship independently. The client rollout does not gate, stage,
  or approve eSTS exposure.
- On client versions containing this change, the new path applies to brokered and
  brokerless HTTPS WebView navigations whose destination URL contains
  `broker_webview_external_browser=1`.
- Unmarked URLs and non-literal marker values retain existing behavior.
- The client ECS value remains an immediate kill switch that restores existing WebView
  behavior without an app release.
- Automated tests cover eligibility boundaries, exact URI preservation, successful launch,
  failure, telemetry, and client flight rollback.

The server team independently controls when eSTS adds the query parameter. Client
monitoring begins when both a client version containing this change and marked eSTS traffic
are present in production.

## Monitoring guardrails

There is no production baseline for this feature. Existing `ProcessWebsiteRequest` traffic
cannot provide one because it contains other website-routing scenarios and does not carry
the new feature-specific outcomes. The production `android_spans` schema also does not expose `operation_outcome` until the
telemetry contract is deployed. The accompanying dashboard uses Android Broker telemetry
and therefore monitors broker-hosted execution; brokerless behavior is covered by the
client tests but is not measured by this dashboard.

1. Once the client build and marked traffic are present in production, verify that
   `operation_outcome` is queryable in `android_spans` and that spans classified by a
   recognized feature outcome have the expected status. Because there is no independent
   feature discriminator, the dashboard cannot detect feature spans that omit
   `operation_outcome`.
2. Review every sampled `broker_external_browser_launch_failed` span during the initial
   client rollout because no production failure baseline exists. Fewer than 10 marked
   requests per day are expected before public preview.
3. Disable `EnableBrokerWebViewExternalBrowserRedirect` if failures are confirmed to be
   caused by client marker handling or external intent launch.

Before a build containing this telemetry reaches production, empty production tiles are
expected. Use the local console exporter or an explicitly sandbox-configured build for
pre-deployment validation.

## Monitoring plan

Review the dashboard after a client version containing the change begins receiving marked
traffic. Review tiles in this order:

1. **Feature span volume and classified outcome consistency**
   - Count `ProcessWebsiteRequest` spans with either feature-specific outcome by Broker Host
     App Version.
   - Before public preview is announced, compare the sampled volume with the expectation of
     no more than 10 total marked requests per day. Because the span is sampled at 50%, do
     not expect telemetry for every request or treat an empty interval as proof of no usage.
   - Verify that each classified feature span has the expected outcome-to-status mapping.
     These tiles cannot measure outcome-attribute completeness because `operation_outcome`
     is also the feature discriminator.
   - For sampled feature spans where `correlation_id_v2` is present, cross-reference it with
     eSTS or VID telemetry and confirm that the request belongs to the new VID feature.
2. **External launch success rate**
   - Calculate success from `broker_external_browser_launched` divided by all feature
     outcomes.
   - Track `broker_external_browser_launch_failed` and `ERROR` spans.
   - Inspect each sampled failure during the initial rollout because no production failure
     baseline exists yet.
3. **Authentication outcomes after handoff**
   - Join successful launch spans to `AcquireTokenInteractive` by `trace_id` for directional
     investigation of later cancellation or error.
   - Treat traces without a terminal span as telemetry-incomplete, not as confirmed session
     loss; current telemetry does not provide definitive return/resume measurement.

## Rollback

To stop client interception, set
`EnableBrokerWebViewExternalBrowserRedirect` to `false` in ECS.

Expected client behavior after propagation:

- Marked URLs stop entering the new external-launch path.
- No new `broker_external_browser_launched` or
  `broker_external_browser_launch_failed` outcomes are emitted for affected app versions.
- The marker remains an ordinary query parameter and the existing WebView behavior applies.
- Marked URLs in brokered and brokerless WebViews return to existing WebView behavior;
  unmarked flows remain unchanged.
- No app release is required.

After rollback:

1. Confirm the effective ECS value is `false` for the affected audience.
2. Confirm feature-specific outcomes stop after ECS propagation, accounting for in-flight
   sessions and telemetry delay.
3. Confirm that the reported failure no longer reproduces after rollback.
4. Preserve affected app-version, Android-version, OEM, and scenario dimensions plus
   example correlation IDs for investigation.
