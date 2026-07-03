#!/bin/bash
# ============================================================================
# Broker-install request-resume  —  full E2E automation
#
# Phases:
#   [0] Clean device state: MSAL cache/accounts, WPJ (via broker uninstall), logcat
#   [1] Uninstall broker
#   [2] Launch MSAL test app, select OUTLOOK config, tap ACQUIRETOKEN
#   [3] HAND OFF -> user enters credentials, gets CA-blocked, taps "Get the app"
#   [4] On RESUME-PARKED (CA redirect -> install): install broker + fire the
#       CP redirect deep-link back to the calling app
#   [5] common retries the parked request through the broker; user types password;
#       token delivered to the app's ORIGINAL callback; app returns to foreground
#   [6] Report terminal state
# ============================================================================
set -u

ADB="$HOME/Library/Android/sdk/platform-tools/adb"
D="emulator-5554"
REPO="/Users/veenasoman/Documents/Work_repos/android-complete"

PKG="com.msft.identity.client.sample.local"
LAUNCHER="com.microsoft.identity.client.testapp.StartActivity"
MAIN="com.microsoft.identity.client.testapp.MainActivity"
BH="com.microsoft.identity.testuserapp"
BH_APK="$REPO/broker/userapp/build/outputs/apk/local/debug/brokerHost-local-debug.apk"

CP="com.microsoft.windowsintune.companyportal"
AUTHR="com.azure.authenticator"

adb(){ "$ADB" -s "$D" "$@"; }
log(){ echo -e "\n\033[1;36m$*\033[0m"; }
ok(){ echo -e "  \033[1;32m$*\033[0m"; }
warn(){ echo -e "  \033[1;33m$*\033[0m"; }

ui_dump(){ adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1; }

# find_node <id|text> <value> [onscreen]  -> prints "x y" of the node center (first match)
find_node(){ python3 - "$1" "$2" "${3:-0}" <<'PY'
import re,sys
kind,val,onscreen=sys.argv[1],sys.argv[2],sys.argv[3]=='1'
try: xml=open('/tmp/ui.xml').read()
except: sys.exit(0)
H=2400
for m in re.finditer(r'<node[^>]*>',xml):
    t=m.group(0)
    rid=re.search(r'resource-id="([^"]*)"',t)
    txt=re.search(r'text="([^"]*)"',t)
    b=re.search(r'bounds="\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]"',t)
    if not b: continue
    rid_s=rid.group(1).split('/')[-1] if rid else ''
    txt_s=txt.group(1) if txt else ''
    hit=(kind=='id' and rid_s==val) or (kind=='text' and txt_s==val)
    if not hit: continue
    x1,y1,x2,y2=map(int,b.groups())
    cx,cy=(x1+x2)//2,(y1+y2)//2
    if onscreen and not (120<=cy<=H-120): continue
    print(cx,cy); break
PY
}

tap_id(){ ui_dump; local c; c=$(find_node id "$1" "${2:-0}"); [ -n "$c" ] && { adb shell input tap $c; return 0; }; return 1; }
tap_text(){ ui_dump; local c; c=$(find_node text "$1" "${2:-0}"); [ -n "$c" ] && { adb shell input tap $c; return 0; }; return 1; }

# Robustly select an option from a Spinner dropdown.
# select_spinner <spinner_id> <option_text>
# Opens the spinner, then retries dump+find (popup renders async); if the option
# is not yet laid out, flings the popup list to reveal lower items. Handles the
# dump-timing race + bottom-of-list items that broke the naive tap.
select_spinner(){
  local sid="$1" opt="$2" i c
  tap_id "$sid" 1 || return 1
  sleep 1
  for i in 1 2 3 4 5 6 7 8; do
    ui_dump
    c=$(find_node text "$opt" 0)
    if [ -n "$c" ]; then adb shell input tap $c; sleep 3; return 0; fi
    adb shell input swipe 540 1900 540 900 200   # scroll popup down to reveal lower items
    sleep 0.5
  done
  return 1
}

# ---------------------------------------------------------------------------
log "===== [0/1] CLEAN DEVICE STATE + UNINSTALL BROKER ====="
adb uninstall "$BH" >/dev/null 2>&1 && ok "broker ($BH) uninstalled (WPJ/device-reg removed)" || warn "broker not installed"
for p in "$CP" "$AUTHR"; do
  if adb shell pm list packages 2>/dev/null | grep -q "package:$p$"; then
    adb uninstall "$p" >/dev/null 2>&1 && ok "removed $p" || warn "could not uninstall $p (system?)"
  fi
done
adb shell pm clear "$PKG" >/dev/null 2>&1 && ok "MSAL app data cleared (token cache + accounts)" || warn "pm clear failed"
adb logcat -c 2>/dev/null; ok "logcat cleared"

# ---------------------------------------------------------------------------
log "===== [2] LAUNCH MSAL TEST APP + OUTLOOK CONFIG ====="
adb shell am start -n "$PKG/$LAUNCHER" >/dev/null 2>&1; sleep 2
tap_id btnStartTask && ok "tapped START TASK" || warn "START TASK not found"
sleep 2
# ensure we're at top of the AcquireToken fragment so Config File spinner is visible
for i in 1 2 3 4 5; do adb shell input swipe 540 700 540 1900 120; done; sleep 1

# open Config File spinner and select OUTLOOK (robust: retries dump + scrolls popup)
if select_spinner configFile OUTLOOK; then ok "selected OUTLOOK config"; else warn "could not select OUTLOOK"; fi

# verify config value
ui_dump
CFG=$(python3 - <<'PY'
import re
xml=open('/tmp/ui.xml').read()
m=re.search(r'configFile"[\s\S]{0,400}?/text1"[^>]*text="([^"]*)"',xml)
print(m.group(1) if m else "?")
PY
)
ok "Config File currently = $CFG"

# ---------------------------------------------------------------------------
log "===== [2b] TAP ACQUIRETOKEN ====="
FOUND=0
for i in $(seq 1 8); do
  if tap_id btn_acquiretoken 1; then FOUND=1; ok "tapped ACQUIRETOKEN"; break; fi
  adb shell input swipe 540 1700 540 700 120; sleep 0.4   # scroll down
done
[ $FOUND -eq 1 ] || { warn "ACQUIRETOKEN button not reachable"; }

# ---------------------------------------------------------------------------
log "===== [3] HAND OFF TO USER ====="
echo "  >>> Sign in with the MAM account, then tap 'Get the app' at the CA block page."
echo "  >>> Everything after that (broker install + redirect + password->token) is automated."

# ---------------------------------------------------------------------------
log "===== [4] WAITING FOR RESUME-PARKED (CA redirect -> install) ====="
RESUME_ID=""
for i in $(seq 1 600); do            # up to ~10 min
  line=$(adb logcat -d -s ResumePOC 2>/dev/null | grep "RESUME-PARKED" | tail -1)
  if [ -n "$line" ]; then
    RESUME_ID=$(echo "$line" | sed -n 's/.*resumeId=\([^ ]*\).*/\1/p')
    ok "PARKED. resumeId=$RESUME_ID"
    break
  fi
  sleep 1
done
[ -z "$RESUME_ID" ] && { warn "Timed out waiting for RESUME-PARKED. Aborting."; exit 1; }

APP_PID_BEFORE=$(adb shell pidof "$PKG" | tr -d '\r')
ok "MSAL app pid (must survive) = $APP_PID_BEFORE"

# ---------------------------------------------------------------------------
log "===== [4b] INSTALL BROKER (simulates user installing Company Portal) ====="
adb install -r -d "$BH_APK" >/dev/null 2>&1 && ok "broker installed" || { warn "broker install failed"; exit 1; }
sleep 3

# ---------------------------------------------------------------------------
log "===== [4c] FIRE CP REDIRECT DEEP-LINK BACK TO CALLING APP ====="
DEEPLINK="msauth://$PKG/resume?resume=$RESUME_ID"
echo "  deep-link: $DEEPLINK"
adb shell am start -a android.intent.action.VIEW -d "$DEEPLINK" >/dev/null 2>&1 && ok "redirect fired" || warn "deep-link failed"

# ---------------------------------------------------------------------------
log "===== [5] RESUME IN BROKER CONTEXT (type your PASSWORD when prompted) ====="
for i in $(seq 1 300); do            # up to ~5 min
  if adb logcat -d -s ResumePOC 2>/dev/null | grep -q "RESUME-COMPLETED"; then
    ok "RESUME-COMPLETED — token delivered to original callback."
    break
  fi
  sleep 1
done

# ---------------------------------------------------------------------------
log "===== [6] TERMINAL STATE ====="
APP_PID_AFTER=$(adb shell pidof "$PKG" | tr -d '\r')
TOP=$(adb shell dumpsys activity activities 2>/dev/null | grep "topResumedActivity" | head -1 | sed -n 's/.*u0 \([^ ]*\).*/\1/p')
echo "  app pid: before=$APP_PID_BEFORE  after=$APP_PID_AFTER  (same = in-memory park survived)"
echo "  top activity: $TOP"
echo
echo "  --- key ResumePOC milestones ---"
adb logcat -d -s ResumePOC 2>/dev/null | grep -Eo "RESUME-[A-Z-]+" | uniq -c
echo
if echo "$TOP" | grep -q "$MAIN"; then ok "PASS: landed on MainActivity (result screen)."
elif echo "$TOP" | grep -q "StartActivity"; then warn "Landed on StartActivity (launcher), not MainActivity."
else warn "Top activity is $TOP (not the app content screen)."; fi
