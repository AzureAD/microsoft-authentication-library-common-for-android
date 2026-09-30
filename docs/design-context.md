All TODO comments must start with the 🔧 emoji.

## Auth UX onboarding telemetry integration

The origin-scoped bridge reuses PR #3278. This corrective change includes that
unmerged implementation rather than introducing a second telemetry transport.
Number Matching remains on its existing broker-only legacy interface.

### Ownership and process boundary

- `LocalMSALController.acquireToken` owns Common's local interactive request through
  authorization **and token exchange**. An Android caller can supply its existing
  `AndroidInteractiveTokenCommandParameters.onboardingTelemetryRecorder`, transferring
  terminal ownership to the controller. Otherwise, a seed and Android Activity allow
  the controller to create one recorder using application context. A recorder already
  registered by an external owner is borrowed, not finalized or removed by the controller.
- `BrokerMsalController` is the calling application's IPC client, not the broker-side
  recorder owner. `MsalBrokerRequestAdapter` already transports `onboardingSeedJson`
  in `BrokerRequest`. A live recorder must **never** be serialized or assumed to cross IPC.
- External broker and OneAuth request owners are not in this repository. Their
  integration is still required; Common's local-controller wiring does not establish
  end-to-end coverage of those external hosts.

For an external owner, the required Common API sequence is:

1. In the process hosting the WebView, obtain the request's existing
   `OnboardingTelemetryRecorder`. Only create it from the propagated seed if the owner
   does not already have one. Keep that instance for all native and WebView recording.
2. Retain one `OnboardingTelemetryRequest(recorder)` for the entire request, not per
   Activity, navigation, or remediation attempt.
3. Before launch, call `register(actualAuthorizationRequestCorrelationId)`. Set
   `AuthorizationActivityParameters.requestCorrelationId` to that **same** ID when using
   the Common activity factory. The seed's `session_correlation_id` is not this lookup
   key. The embedded Common strategy supplies the actual authorization request UUID;
   existing factory callers without an explicit ID retain diagnostic-context behavior.
4. Common's `WebViewAuthorizationFragment` attaches the registered recorder and
   initializes the bridge before the first authorization load. A separate OneAuth
   WebView host must itself call `setOnboardingTelemetryRecorder(recorder)` then
   `initializeAuthUxJavaScriptApi(webView, authorizationUrl)` before `loadUrl`.
5. Stop bridge delivery (`removeAuthUxTelemetryWebMessageApi`) before terminal
   completion. Common's fragment does this before broadcasting its authorization result.
   Only when the **whole owning request** terminates, call `complete(AcquireTokenResult)`
   or `complete(BaseException)` on the retained request object. It caches one snapshot
   from that same recorder and unregisters its handoffs. Always call `close()` in
   `finally`, including launch failures. `close()` alone abandons the handoff; it does
   not emit a terminal blob.
6. A broker owner must pass the completed blob to the existing blob-aware
   `MsalBrokerResultAdapter.bundleFromAuthenticationResult` overload, or attach it to
   the exception before `bundleFromBaseException`. A OneAuth owner must forward that
   populated blob to its native result/error/cancellation telemetry path, not return
   the original seed. Those external terminal call sites still require consumer changes.

Do not complete on temporary interrupts, remediation, navigation, `onStop`, or
`onDestroyView`. Configuration recreation detaches the old bridge, preserves the
request registration, and reattaches by the saved request ID. Process death does not
preserve a live recorder; session-correlation persistence is not an event journal.
The registry admits at most 16 active IDs and rejects conflicts/capacity overflow
without evicting another live request. Failed optional registration must not stop auth.

`AcquireTokenResult.onboardingBlob` and `BaseException.onboardingBlob` are the supported
Common result/exception carriers; result-to-exception conversion preserves the blob.
For locally owned telemetry, otherwise untyped checked/runtime failures are mapped by
`ExceptionAdapter` and carried in `ExecutionException` so the existing dispatcher can
unwrap the domain error without losing the blob. Requests without an owned recorder
retain their original exception behavior; an interrupted owned request also restores
the thread's interrupt flag.
The existing `CommandDispatcher` success callback delivers `LocalAuthenticationResult`,
and its no-argument cancellation callback cannot carry this blob. Consumers using those
callbacks must integrate at the owning request's terminal result boundary; this change
does not invent a new public callback contract or claim those callbacks deliver the blob.

### Server contract and capability gate

The server sends `window.broker.postMessageToBroker(JSON.stringify(payload))` with,
for example:

```json
{
  "correlationID": "server-request-correlation",
  "action_name": "log_telemetry",
  "action_component": "host",
  "params": { "errorCode": 530003 }
}
```

The payload correlation ID is page-supplied context, **not** a recorder-selection key.
`log_telemetry` routes exclusively to `window.authUxTelemetry.postMessage`. Other
actions cannot mutate Number Matching through the telemetry receiver.

Startup delivery requires **both** `WEB_MESSAGE_LISTENER` and `DOCUMENT_START_SCRIPT`.
If either is unavailable, or telemetry registration fails, optional bridge telemetry
is disabled and authentication continues. There is deliberately no telemetry
`onPageFinished` fallback: it cannot capture initial page JavaScript reliably. Servers
must feature-detect the wrapper before calling it and must not make authentication
depend on telemetry availability. There is no `addJavascriptInterface` telemetry fallback.
Number Matching's existing late wrapper is separate and is not a startup-delivery guarantee.

Only HTTPS main-frame messages on the default port from subdomains of
`microsoftonline.com`, `microsoftonline.us`, `microsoftonline.cn`, and `microsoft.com`
are accepted. This does not add `live.com`, bare suffix domains, arbitrary ports,
HTTP origins, or subframe delivery.

Numeric codes (JSON numbers or strings) are validated before recording; nonnumeric,
malformed, and excluded codes `0`, `50058`, `50097`, `50126` do not append errors.
Qualifying duplicates are retained in order, not deduplicated. To bound page-driven
memory/blob growth, a recorder retains the **first 256 blocking-error occurrences**
(including native symbolic errors), drops later occurrences with one warning, and
sets `last_blocking_error` to the last **retained** occurrence. No synthetic error is
inserted. Session-correlation persistence is attempted only at the first block, so
repeated messages do not repeatedly read/write SharedPreferences on the UI thread;
its timestamp is the first block, not a sliding last-message timestamp.

### Coverage boundary

Common tests cover local-controller request ownership, same-recorder completion,
registry isolation/cleanup, fragment attach-before-load and recreation behavior,
bridge origin/frame filtering, ordered duplicates, feature gating, and best-effort
setup/teardown. Broker-mode lifecycle/bridge tests exercise the Common integration
contract; they are not tests of the unavailable broker or OneAuth request owners.
Real-device page-start JavaScript delivery and consumer-level IPC/native completion
still need validation in those consuming applications.