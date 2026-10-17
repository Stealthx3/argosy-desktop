#!/usr/bin/env bash
# Save-sync live proof capture; steps and output layout: docs/desktop/testbed-runbook.md.
# Restores a testbed snapshot, installs <apk> over the app data, walks every save-sync door.
# usage: scripts/desktop/check/save-proof.sh <apk> <out-dir> [snapshot-name|--no-restore]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
APK="$(realpath "$1")"
OUT="$2"
SNAPSHOT="${3:-saves-baseline}"
SERIAL=emulator-5556
PKG=com.nendo.argosy.debug
APPDIR=/data/data/$PKG
BASE_URL=http://localhost:8093
SDK="$HOME/Android/Sdk"
ADB="$SDK/platform-tools/adb -s $SERIAL"
TESTBED="$ROOT/scripts/desktop/testbed"
mkdir -p "$OUT"
LOG="$OUT/run.log"
: > "$LOG"

say() { echo "[$(date +%T)] $*" | tee -a "$LOG"; }
key() { $ADB shell input keyevent "$1"; sleep "${2:-1}"; }
shot() { $ADB exec-out screencap -p > "$OUT/$1.png"; }
logcount() { $ADB logcat -d | grep -c -E "$1" || true; }

wait_log() {
    local pattern=$1 before=$2 timeout=${3:-120} waited=0
    until [ "$(logcount "$pattern")" -gt "$before" ]; do
        sleep 2
        waited=$((waited + 2))
        if [ "$waited" -ge "$timeout" ]; then
            say "timeout after ${timeout}s waiting for: $pattern"
            return 1
        fi
    done
}

if [[ "$SNAPSHOT" != "--no-restore" ]]; then
    say "restoring snapshot $SNAPSHOT"
    "$TESTBED/snapshot.sh" restore "$SNAPSHOT" >> "$LOG" 2>&1
fi
set -a
. "$ROOT/build/desktop-testbed/credentials.env"
set +a

capture() {
    local step=$1 dir="$OUT/$1"
    mkdir -p "$dir/db" "$dir/datastore"
    sleep 5
    $ADB logcat -d > "$dir/logcat.txt"
    $ADB logcat -c
    grep -E 'planReconcile:' "$dir/logcat.txt" > "$dir/plan-reconcile.txt" || true
    curl -sf -H "Authorization: Bearer $ROMM_CLIENT_TOKEN" "$BASE_URL/api/saves" > "$dir/saves-api.json"
    python3 -I "$TESTBED/save_tables.py" api "$dir/saves-api.json" > "$dir/saves-api.sorted.json"
    python3 -I "$TESTBED/save_tables.py" api "$dir/saves-api.json" --normalize > "$dir/saves-api.normalized.json"
    for f in alauncher.db alauncher.db-wal alauncher.db-shm; do
        $ADB exec-out run-as "$PKG" cat "databases/$f" > "$dir/db/$f"
    done
    python3 -I "$ROOT/scripts/desktop/check/db_summary.py" "$dir/db/alauncher.db" > "$dir/db-summary.json"
    python3 -I "$TESTBED/save_tables.py" db "$dir/db/alauncher.db" > "$dir/save-tables.json"
    python3 -I "$TESTBED/save_tables.py" db "$dir/db/alauncher.db" --normalize --part saves > "$dir/save-tables.normalized.json"
    python3 -I "$TESTBED/save_tables.py" db "$dir/db/alauncher.db" --normalize --part states > "$dir/state-tables.normalized.json"
    for f in $($ADB shell run-as "$PKG" ls files/datastore | tr -d '\r'); do
        $ADB exec-out run-as "$PKG" cat "files/datastore/$f" > "$dir/datastore/$f"
    done
    python3 -I "$ROOT/scripts/desktop/check/datastore_summary.py" "$dir/datastore" > "$dir/datastore.json"
    python3 -I "$TESTBED/save_tables.py" datastore "$dir/datastore.json" --normalize > "$dir/datastore.normalized.json"
    $ADB shell "run-as $PKG sh -c 'cd $APPDIR/files && find libretro/saves libretro/states save_cache -type f -exec sha256sum {} + 2>/dev/null | sort -k2'" \
        | tr -d '\r' > "$dir/save-files.sha256"
    $ADB shell "run-as $PKG sh -c 'cd $APPDIR/files && find libretro/saves libretro/states save_cache -type f -exec md5sum {} + 2>/dev/null | sort -k2'" \
        | tr -d '\r' > "$dir/save-files.md5"
    for f in sha256 md5; do
        grep -v '  libretro/states/' "$dir/save-files.$f" \
            | sed -E 's#/[0-9]{8}_[0-9]{6}/#/<stamp>/#; s#save_precache_[0-9]+_[0-9]+\.tmp#save_precache_<n>.tmp#' \
            | sort -k2 > "$dir/save-files.normalized.$f" || true
    done
    curl -sf -H "Authorization: Bearer $ROMM_CLIENT_TOKEN" "$BASE_URL/api/states" > "$dir/states-api.json" || true
    shot "$step"
    say "captured $step: $(tr '\n' ' ' < "$dir/plan-reconcile.txt" | grep -oE 'upload=[0-9]+ download=[0-9]+ conflict=[0-9]+ no_op=[0-9]+' | tr '\n' ';')"
}

game_id() {
    python3 -I - "$OUT/00-startup/db/alauncher.db" "$1" <<'PY'
import sqlite3, sys
row = sqlite3.connect(sys.argv[1]).execute("SELECT id FROM games WHERE title = ?", (sys.argv[2],)).fetchone()
print(row[0] if row else "")
PY
}

play_and_quit() {
    local title=$1 step_running=$2 step_quit=$3 seconds=$4 id
    id=$(game_id "$title")
    [[ -n "$id" ]] || { say "no local game named $title"; exit 1; }
    say "opening $title (game $id) and pressing Play"
    $ADB shell am start -a android.intent.action.VIEW -d "argosy://game/$id" -n "$PKG/com.nendo.argosy.ArgosyActivity" >/dev/null
    sleep 4
    local loads ends
    loads=$(logcount 'libretrodroid: afterGameLoad')
    key KEYCODE_BUTTON_A 2
    wait_log 'libretrodroid: afterGameLoad' "$loads" 90 || { shot "$step_running-failed"; exit 1; }
    sleep "$seconds"
    capture "$step_running"
    ends=$(logcount 'Sync result:|Cancelled \(no save backup\)')
    say "quitting $title through the in-game menu"
    key KEYCODE_BACK 2
    for _ in $(seq 8); do key KEYCODE_DPAD_DOWN 0.4; done
    key KEYCODE_BUTTON_A 3
    wait_log 'Sync result:|Cancelled \(no save backup\)' "$ends" 90 || true
    $ADB logcat -d | grep -E 'Sync result:|Cancelled \(no save backup\)' | tail -1 | sed 's/^/    /' | tee -a "$LOG"
    sleep 5
    capture "$step_quit"
}

say "installing $APK over the existing app data (arm64-v8a: the built-in cores and sigil are arm64-only)"
$ADB install -r --abi arm64-v8a "$APK" >> "$LOG" 2>&1
$ADB logcat -c
$ADB shell am start -a android.intent.action.MAIN -n "$PKG/com.nendo.argosy.ArgosyActivity" >/dev/null
wait_log 'reconcileAll: plan applied|planReconcile: sessionId=[0-9]+ romIds=null' 0 120 || true
sleep 10
capture 00-startup

for n in 1 2; do
    say "Save Sync screen -> Scan RomM ($n)"
    key KEYCODE_BUTTON_B 1
    key KEYCODE_BUTTON_B 1
    $ADB shell am start -a android.intent.action.MAIN -n "$PKG/com.nendo.argosy.ArgosyActivity" >/dev/null
    sleep 2
    key KEYCODE_BUTTON_START
    for _ in $(seq 6); do key KEYCODE_DPAD_DOWN 0.4; done
    key KEYCODE_BUTTON_A 3
    before=$(logcount 'reconcileAll: plan applied')
    key KEYCODE_BUTTON_Y 2
    wait_log 'reconcileAll: plan applied' "$before" 90 || true
    sleep 8
    capture "0$n-sync-$n"
done
key KEYCODE_BUTTON_B 2

play_and_quit "Ledger Isle" 03-prelaunch-ledger 04-quit-unchanged 40
play_and_quit "Battery Bay" 05-prelaunch-battery 06-session-end 40

crashes=$(cat "$OUT"/*/logcat.txt | grep -c 'FATAL EXCEPTION' || true)
say "FATAL EXCEPTION lines across the run: $crashes"
say "done: $OUT"
