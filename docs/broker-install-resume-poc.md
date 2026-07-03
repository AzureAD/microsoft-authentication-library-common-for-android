# Broker-Install Request Resume — POC Design

> **Status:** Proof of Concept (branch `veena11/broker-install-resume-poc`)
> **Scope:** `common` / `common4j` only — **zero first-party (1P) app change**, **in-memory only**, **resume goes through the broker**.

## 1. Problem

A user signs into a MAM-protected app on a device with **no broker (Company Portal) installed**. A
Conditional-Access policy blocks the sign-in and requires the broker. Today the stack returns a
`BROKER_INSTALLATION` error to the calling app and the interactive request is **lost** — after the
user installs Company Portal they must restart the whole sign-in.

**Goal:** don't fail the request. **Park** it, let Company Portal redirect the user straight back
(skipping its own sign-in), **resume** the same request through the broker with the UPN
prepopulated, and deliver the token to the app's **original `acquireToken` callback** — with **no
change to the 1P app**.

## 2. Target production design (reference)

The production design targets the **OneAuth → IPC → Broker** path:

```
[1P App] --acquireToken(interactive)--> [OA] --> [XP] InteractiveRequest
                                                     |
                            AAD CA "install broker" (app_link, UPN)
                                                     v
                                 [XP] HandleBrokerInstallation
                                      _broker->GetTokenInteractively  ---> keeps sink PENDING
                                                     |
                                                     v
                                 [MA] BrokerClient.getTokenInteractively
                                                     |
                                                     v
         +-------------------------- [C4J] BrokerMsalController --------------------------+
         |  * capture UPN/client_info from WPJ RawAuthorizationResult                     |
         |  * PARK request in-memory keyed by correlationId (cid)                         |
         |  * fire Play Store launch WITH src=mamca&originPkg=..&redirectUri=..&cid=..     |
         +--------------------------------------|----------------------------------------+
                                                v
                                        [Play Store] install CP
                                                v
                                      [CP] first launch:
                                        read referrer -> SKIP sign-in UX
                                        redirect: <redirectUri>?mam_resume=<cid>
                                                v
                           [C4J] BrowserTabActivity catches redirect (mam_resume=cid)
                                                |
                                                v
         +-------------------------- [C4J] resume handler -----------------------------+
         |  * look up parked request by cid                                            |
         |  * force-fresh discovery (CP now installed)                                 |
         |  * Request retry via broker, login_hint = parked UPN                        |
         |  * broker registers device + issues compliant token                        |
         |  * fire the SAME pending BrokerEventSink                                    |
         +---------------------------------------|-------------------------------------+
                                                 v
                           [XP] OnResponse  -->  [OA] AuthResult  -->  [1P App]
                                              (original request completes)
```

## 3. POC implementation

The POC proves the **same end-to-end contract** on the **MSAL embedded-WebView** path (so it can be
driven fully from the MSAL test app, no OneAuth build required). Conceptually every production node
has a POC counterpart:

```
[MSAL test app] --acquireToken(interactive)--> [common] InteractiveTokenCommand
   (stands in for 1P/OA)                              |
                                                      |  embedded WebView (WEBVIEW user-agent)
                             eSTS CA install redirect: msauth://wpj/?username=<UPN>&app_link=<play>
                                                      v
                      [common] AzureActiveDirectoryWebViewClient.processInstallRequest
                                                      |
         +---------------- [common] parkAndAppendResumePointer -------------------------+
         |  * capture UPN from CA redirect (username param)                             |
         |  * BrokerInstallResumeCoordinator.park(cid, command, upn)  (in-memory)       |
         |      - retains ORIGINAL callback + params + controllerFactory + publicApiId  |
         |      - BrokerInstallResumeParkRegistry.park(cid)                             |
         |  * onChallengeResponseReceived(result)                                       |
         |      -> CommandDispatcher SUPPRESSES the app-facing BROKER_INSTALLATION      |
         |         result for the parked cid (request stays pending to the app)         |
         |  * BrokerInstallReferrerBuilder.withResumePointer(appLink, cid, pkg, uri)    |
         |      referrer = resumeCid=<cid>;originPkg=<pkg>;redirectUri=<uri>            |
         |  * launch Play Store link (delayed startActivity)                            |
         +--------------------------------------|--------------------------------------+
                                                v
                                 [Play Store] install broker (Company Portal)
                                                v
                        [CP first launch — SIMULATED in POC by the E2E harness]
                          read referrer -> skip sign-in -> redirect back:
                          msauth://<originPkg>/resume?resume=<cid>
                                                v
              [common] BrokerInstallResumeActivity  (manifest deep-link, auto-merged
                        into every consumer -> zero 1P change; singleTop, foreground)
                                                |
         +---------------- [common] BrokerInstallResumeCoordinator.resume(cid) ---------+
         |  * look up parked entry by cid                                               |
         |  * invalidateBrokerDiscoveryCache()  (force-fresh discovery; clears backoff) |
         |  * rebuild params: activity + loginHint = parked UPN                         |
         |  * brokerForcingFactory -> BrokerMsalController (retry runs in BROKER)        |
         |  * CommandDispatcher.beginInteractive(resumeCommand)                         |
         |  * broker registers device + issues compliant token                         |
         |  * wrappedCallback -> entry.callback (the ORIGINAL callback)                 |
         +---------------------------------------|-------------------------------------+
                                                 v
                       original acquireToken callback fires  -->  [MSAL test app]
                       returnToOriginApp(): land the user back on the app's screen
                                     (original request completes)
```

### 3.1 Components (POC)

| Concern | File | Notes |
|---------|------|-------|
| Catch CA install redirect, capture UPN, park, append referrer | `common/.../internal/ui/webview/AzureActiveDirectoryWebViewClient.java` | `processInstallRequest`, `parkAndAppendResumePointer` |
| In-memory park + broker-forced resume + cache invalidation + original-callback delivery | `common/.../internal/providers/BrokerInstallResumeCoordinator.java` | `park`, `resume`, `invalidateBrokerDiscoveryCache`, `brokerForcingFactory`, `returnToOriginApp` |
| Deep-link receiver for the CP redirect | `common/.../internal/providers/BrokerInstallResumeActivity.java` | `singleTop`, foreground-anchored landing |
| Suppress the app-facing `BROKER_INSTALLATION` result for a parked cid | `common4j/.../controllers/CommandDispatcher.java` + `BrokerInstallResumeParkRegistry` | `returnCommandResult` checks `isParked` |
| Referrer pointer builder | `common4j/.../providers/BrokerInstallReferrerBuilder.kt` | `resumeCid;originPkg;redirectUri` |
| Install-link allowlist | `common4j/.../providers/BrokerInstallLinkValidator` | gate before parking |
| Manifest deep-link (auto-merges into consumers) | `common/src/main/AndroidManifest.xml` | `msauth://${applicationId}/resume` |
| Feature flag | `CommonFlight.ENABLE_BROKER_INSTALL_RESUME` | off = today's behavior |

### 3.2 On-screen step narration (POC only)

`BrokerInstallResumeCoordinator.showStep(Context, String)` posts bottom-of-screen `Toast`s so the
flow is observable end-to-end:

1. **①/④** Sign-in blocked → parking request, installing Company Portal
2. **②/④** Company Portal installed → resuming request
3. **③/④** Retrying token in broker context
4. **④/④** Token returned successfully ✅

Mirrored by `ResumePOC` logcat markers: `RESUME-PARKED → RESUME-DEEPLINK → RESUME-CACHE-CLEARED →
RESUME-DISPATCH → RESUME-COMPLETED → RESUME-FOREGROUND`.

## 4. Conceptual alignment (prod design ↔ POC)

| Production design node | POC counterpart | Match |
|------------------------|-----------------|:-----:|
| `acquireToken(interactive)` entry | MSAL test app `acquireToken` → `InteractiveTokenCommand` | ✅ |
| CA "install broker" (app_link, UPN) | eSTS `msauth://wpj/?username&app_link` redirect | ✅ |
| Keep sink **PENDING** | Complete original command but **suppress** app delivery + **retain callback** | ✅ (equivalent) |
| Capture UPN / client_info | Capture **UPN** (from `username`) | ⚠️ client_info not captured |
| PARK in-memory keyed by **cid** | `BrokerInstallResumeCoordinator.park(cid, …)` | ✅ |
| Play Store launch carries cid/originPkg/redirectUri | `referrer = resumeCid;originPkg;redirectUri` | ✅ (no `src=mamca`) |
| CP reads referrer, skips sign-in, redirects `?mam_resume=cid` | **Simulated** by E2E harness; redirect `?resume=cid` | ❌ CP side out of scope |
| Redirect caught by `BrowserTabActivity` | Dedicated `BrokerInstallResumeActivity` (auto-merged) | ⚠️ improved for zero-1P-change |
| Resume: look up by cid | `resume(cid)` looks up parked entry | ✅ |
| Force-fresh discovery | `invalidateBrokerDiscoveryCache()` | ✅ |
| Retry via broker, login_hint = UPN | `brokerForcingFactory` + `loginHint = upn` | ✅ |
| Broker registers device + issues token | Broker interactive command | ✅ |
| Fire the **same** pending sink | `entry.callback` (original callback) invoked | ✅ |
| Response → AuthResult → 1P app | Original callback → MSAL → app; `returnToOriginApp` lands user | ✅ (+ Android foregrounding) |

## 5. Known divergences / production-hardening TODOs

1. **Path relocation (most important).** The POC parks on the **MSAL embedded-WebView** path
   (`AzureActiveDirectoryWebViewClient`). Real 1P (OneAuth) apps run through the **broker**, so the
   install challenge returns from the broker, not the MSAL WebView — the POC's park trigger would
   not fire for them. For production, move capture/park to the **broker-side WPJ result handling**
   (`BrokerMsalController`), matching design node `[C4J] BrokerMsalController`.
2. **`client_info` capture.** Design captures UPN **and** client_info; POC captures UPN only.
3. **Param-name alignment.** POC uses `resumeCid` / `?resume=`; design uses `?mam_resume=` and
   `src=mamca`. Align the contract with the real Company Portal implementation.
4. **Company Portal side.** "read referrer → skip sign-in → redirect back" is **not** implemented;
   it is simulated by the harness. Requires the CP team's change.
5. **In-memory only.** A process death during the Play Store install loses the parked request (an
   accepted POC trade-off; matches design's in-memory intent).
6. **Strip POC-only surface for prod.** `showStep` toasts, `ResumePOC` logcat markers, and any
   orphaned POC helper classes.

## 6. Testing

End-to-end automation: [`broker-install-resume-poc-e2e.sh`](./broker-install-resume-poc-e2e.sh). Cleans device state (cache/WPJ),
uninstalls the broker, launches the MSAL test app with the **OUTLOOK** config (WEBVIEW user-agent),
hands off for credentials, then on `RESUME-PARKED` auto-installs the broker and fires the CP
redirect deep-link; the resume completes in broker context and the token is delivered to the
original callback. Verified invariants:

- Same app **pid** before/after → in-memory park survived the install + broker round-trip.
- Resume dispatched with `loginHintPresent=true` → UPN prepopulated.
- Result screen `correlation_id` equals the parked `resumeId`.
- App lands on `MainActivity` (result screen), not the launcher / Custom Tab / broker.
