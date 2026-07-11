# Broker-Install Request Resume — Engineer Hand-Off

**POC author:** @Veena11 · **Date:** 2026-07-02 · **Status:** POC complete, E2E-verified (repeatable PASS)

## 0. What this POC proves

When a MAM-protected 1P app does an interactive `acquireToken` and eSTS Conditional Access blocks it
with an **"install broker" (Company Portal)** challenge, we can — **with zero 1P app change**:

1. **Park** the in-flight interactive request in-memory in `common` (keyed by correlation id), instead
   of returning `BROKER_INSTALLATION` to the app.
2. Let the user install Company Portal; CP (on first launch) reads the install referrer, **skips its
   sign-in UX**, and **deep-links back** to the originating app.
3. **Resume** the parked request **through the broker** (force-fresh discovery + `login_hint = UPN`),
   register the device, and **deliver the token to the app's original `acquireToken` callback in the
   original process** — the app never sees an error and never re-issues the call.

The original process/callback survives the whole install round-trip (verified: same pid before/after).

---

## 1. POC branches (all modules)

Branch name (new, "no 1P change"): **`veena11/broker-install-resume-no-1p-change`**
Old POC branch (kept intact for reference): **`veena11/broker-install-resume-poc`**

| Module | GitHub repo | New-branch SHA | Pushed? | Notes |
|--------|-------------|----------------|:-------:|-------|
| **common** | `AzureAD/microsoft-authentication-library-common-for-android` | `c3bb387` | ✅ | **All real POC logic lives here** (also contains `common4j`) |
| **msal** | `AzureAD/microsoft-authentication-library-for-android` | `99eaf7f` | ✅ | Test-app E2E scaffolding + **one** TEST-ONLY lib flag |
| **broker** | `AzureAD/ad-accounts-for-android` *(archived, read-only)* | `716c99d` | ⚠️ old branch only | brokerHost **test-harness** only; new branch = identical commit, **use the old branch** |

> **broker note:** the `AzureAD` broker mirror was archived after 2026-07-01, so the *new* branch name
> could not be created there. It is **byte-identical** to the old branch `veena11/broker-install-resume-poc`
> @ `716c99d`, which **is** on the remote — use that. Broker's live repo is the GHE
> `identity-authnz-teams/ad-accounts-for-android` if you need to re-home it.

### What each module actually changes

- **common (`c3bb387`) — the feature.** Park/resume coordinator, deep-link receiver, referrer builder,
  parked-cid result suppression, manifest deep-link (auto-merges into consumers), feature flag. See
  design doc §3.1 for the file-by-file table.
- **msal (`99eaf7f`) — test harness + 1 lib flag.** Everything is test-app E2E scaffolding **except**
  one library change: `PublicClientApplicationConfiguration.java` adds a `[POC TEST-ONLY]`
  `sBypassBrokerRedirectUriCheckForPoc` flag (mirrors the `-PbypassRedirectUriCheck` build arg).
  **Never ship this** — it exists only so the test app's redirect URI passes locally.
- **broker (`716c99d`) — test harness only, NO broker production code.** All changes are under
  `userapp/` (the **brokerHost** app, which stands in for **Company Portal**):
  `run-e2e.sh`, `userapp/build.gradle` (TEST-ONLY MSAL AAR fallback),
  `BrokerHostApplication.java` (`setShouldTrustDebugBrokers(true)` — debug only),
  `MainActivity.java` (`maybeResumeBrokerInstallRequest()` — **simulates the CP first-launch
  referrer-read + redirect**; in prod this belongs to Company Portal, not broker).

---

## 2. Design doc

- **POC design & alignment:** [`broker-install-resume-poc.md`](./broker-install-resume-poc.md)
  - §2 target production design (reference diagram)
  - §3 POC implementation + §3.1 component/file table
  - §4 conceptual alignment table (prod node ↔ POC, ✅/⚠️/❌)
  - §5 known divergences / production-hardening TODOs
  - §6 testing / verified invariants

Conceptually the POC matches the prod design; the deltas are enumerated in §5 and summarized in §4
below.

---

## 3. E2E test script + how to run

- **Script:** [`broker-install-resume-poc-e2e.sh`](./broker-install-resume-poc-e2e.sh) (in `common/docs/`)

### Prerequisites
- macOS with `adb` at `~/Library/Android/sdk/platform-tools/adb`, `python3` on PATH.
- Running emulator **`emulator-5554`**.
- **MSAL test app** installed: `com.msft.identity.client.sample.local`
  (build: `cd msal && ./gradlew :testapps:testapp:assembleLocalDebug -PbypassRedirectUriCheck`, then `adb install`).
- **brokerHost APK built** (the script uninstalls/installs it):
  `cd broker && ./gradlew :userapp:assembleLocalDebug`
  → `broker/userapp/build/outputs/apk/local/debug/brokerHost-local-debug.apk`.
  The APK is **gitignored** (build artifact); the `userapp` **source** is committed, so build it locally.
- Test account: MAM-CA account, e.g. `idlabmamca@msidlab4.onmicrosoft.com`.

### Run
```bash
cd /path/to/android-complete
./common/docs/broker-install-resume-poc-e2e.sh
```
Zero args, interactive. Run it in a terminal you can watch — it pauses **twice** for on-device input.

### Phase map (what's automated vs. yours)
| Phase | Automated | Your action |
|-------|:---------:|-------------|
| [0-1] Clean state (MSAL cache/accounts, WPJ via broker uninstall, logcat), uninstall broker | ✅ | — |
| [2] Launch MSAL app → select **OUTLOOK** config → tap **ACQUIRETOKEN** | ✅ | — |
| **[3] HAND OFF** | ⏸️ | Sign in with the MAM account; when CA blocks, tap **"Get the app"** |
| [4] On `RESUME-PARKED`: install broker + fire CP redirect deep-link | ✅ | — |
| **[5] Resume in broker context** | ⏸️ | Type your **password** when the broker prompts |
| [6] Report terminal state | ✅ | — |

---

## 4. Expectations for a passing automation run

A PASS looks like this (from two verified runs):

```
===== [4] WAITING FOR RESUME-PARKED ... =====
  PARKED. resumeId=<cid>
  MSAL app pid (must survive) = <PID>
===== [4b] INSTALL BROKER ... =====   broker installed
===== [4c] FIRE CP REDIRECT DEEP-LINK ... =====   redirect fired
===== [5] RESUME IN BROKER CONTEXT ... =====
  RESUME-COMPLETED — token delivered to original callback.
===== [6] TERMINAL STATE =====
  app pid: before=<PID>  after=<PID>  (same = in-memory park survived)
  top activity: .../MainActivity
  --- key ResumePOC milestones ---
   1 RESUME-PARKED
   1 RESUME-DEEPLINK
   1 RESUME-CACHE-CLEARED
   1 RESUME-DISPATCH
   1 RESUME-COMPLETED
   1 RESUME-FOREGROUND
  PASS: landed on MainActivity (result screen).
```

**Invariants to assert:**
1. **Same app pid before == after** → the in-memory park survived the install + broker round-trip
   (no process death, original callback still live).
2. All six `ResumePOC` milestones fire, in order:
   `RESUME-PARKED → RESUME-DEEPLINK → RESUME-CACHE-CLEARED → RESUME-DISPATCH → RESUME-COMPLETED → RESUME-FOREGROUND`.
3. Result screen **`correlation_id` == parked `resumeId`** (same request completed, not a new one).
4. Resume dispatched with `loginHintPresent=true` → **UPN pre-populated** on the retry.
5. Top activity is the app's **`MainActivity`** (result screen) — not the launcher, Custom Tab, or broker.

**On-screen narration (POC only):** four bottom-of-screen toasts —
①/④ "Sign-in blocked → parking request, installing Company Portal", ②/④ "Company Portal installed →
resuming request", ③/④ "Retrying token in broker context", ④/④ "Token returned successfully ✅".

---

## 5. Changes needed for the production version

From design doc §5, in priority order:

1. **Relocate the park to the broker path (most important).** The POC parks on the **MSAL embedded-
   WebView** path (`AzureActiveDirectoryWebViewClient`). Real 1P (OneAuth) apps run **through the
   broker**, so the install challenge returns from the broker, not the MSAL WebView — the POC trigger
   would not fire for them. Move UPN/client_info capture + park to the **broker-side WPJ result
   handling**, i.e. design node **`[C4J] BrokerMsalController`** (`common4j`).
2. **Capture `client_info` too** (POC captures UPN only). Design captures UPN **and** client_info.
3. **Align the referrer / redirect contract with the real Company Portal.** POC uses
   `resumeCid;originPkg;redirectUri` and `?resume=<cid>`; design uses `src=mamca` and
   `?mam_resume=<cid>`. Agree the exact param names with the CP team and standardize.
4. **Company Portal change (external, different team).** "read install referrer → skip sign-in UX →
   redirect back to origin app" is **simulated** by the brokerHost harness in this POC. Production
   requires a real change in **Company Portal** (owned by the CP/Intune team). This is a hard
   dependency — track it as a separate cross-team work item.
5. **Durability decision.** POC park is **in-memory only** — a process death during the Play Store
   install loses the parked request (accepted POC trade-off, matches design's in-memory intent).
   Decide whether prod needs persistence across process death.
6. **Strip POC-only surface.** Remove `showStep` toasts, `ResumePOC` logcat markers, the
   `[POC TEST-ONLY]` `sBypassBrokerRedirectUriCheckForPoc` MSAL flag, and any orphaned POC helpers.
7. **Ship behind a flag.** Gate on `CommonFlight.ENABLE_BROKER_INSTALL_RESUME` (off ⇒ today's
   behavior) for safe rollout.

### Reminder
- The dedicated `BrokerInstallResumeActivity` (vs. reusing `BrowserTabActivity`) is a **deliberate**
  choice that enables zero-1P-change; keep or fold into `BrowserTabActivity` per your prod design.
- `returnToOriginApp` foregrounding is an Android-specific addition beyond the reference diagram.

---

## 6. Conceptual alignment snapshot (prod ↔ POC)

| Production design node | POC counterpart | Match |
|------------------------|-----------------|:-----:|
| `acquireToken(interactive)` entry | test app `acquireToken` → `InteractiveTokenCommand` | ✅ |
| CA "install broker" (app_link, UPN) | eSTS `msauth://wpj/?username&app_link` | ✅ |
| Keep sink **PENDING** | Complete command but **suppress** delivery + **retain** callback | ✅ (equiv) |
| Capture UPN / client_info | Capture **UPN** only | ⚠️ |
| PARK in-memory keyed by **cid** | `BrokerInstallResumeCoordinator.park(cid, …)` | ✅ |
| Play Store launch carries cid/originPkg/redirectUri | `referrer = resumeCid;originPkg;redirectUri` | ✅ (no `src=mamca`) |
| CP reads referrer → skip sign-in → redirect `?mam_resume=cid` | **Simulated** by harness; `?resume=cid` | ❌ (CP team) |
| Redirect caught by `BrowserTabActivity` | Dedicated `BrokerInstallResumeActivity` | ⚠️ (zero-1P) |
| Force-fresh discovery + retry via broker, `login_hint = UPN` | `invalidateBrokerDiscoveryCache()` + `brokerForcingFactory` | ✅ |
| Fire the **same** pending sink → AuthResult → 1P app | original `entry.callback` → MSAL → app | ✅ (+ foregrounding) |

---

## 7. Quick pointers (key files, in `common`)

- `common/.../internal/providers/BrokerInstallResumeCoordinator.java` — park / resume / cache-invalidate / original-callback delivery / `returnToOriginApp`
- `common/.../internal/providers/BrokerInstallResumeActivity.java` — CP-redirect deep-link receiver
- `common/.../internal/ui/webview/AzureActiveDirectoryWebViewClient.java` — POC capture/park trigger (**move to `BrokerMsalController` for prod**)
- `common4j/.../providers/BrokerInstallReferrerBuilder.kt` — referrer pointer format
- `common4j/.../controllers/CommandDispatcher.java` (+ park registry) — suppress app-facing `BROKER_INSTALLATION` for a parked cid
