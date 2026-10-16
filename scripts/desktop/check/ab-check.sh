#!/usr/bin/env bash
# A/B screen and database check of the Android debug app against the pre-Phase-1 baseline APK.
# Runs on AVD argosy_handheld_check (emulator-5556), which must already be signed in to the RomM
# testbed (scripts/desktop/testbed/seed-emulator.sh emulator-5556). Each build is installed over
# the same app data, launched, and walked through the same gamepad sequence; screenshots and a
# database and DataStore summary are compared with both clocks masked. Never clears app data.
#
# usage: scripts/desktop/check/ab-check.sh [candidate.apk] [out-dir]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
CANDIDATE="${1:-$ROOT/app/build/outputs/apk/debug/app-debug.apk}"
OUT="${2:-$ROOT/build/desktop-check/$(date +%Y%m%d-%H%M%S)}"
BASELINE="$ROOT/build/desktop-baseline/app-debug-3ad8281f6.apk"
SERIAL=emulator-5556
PKG=com.nendo.argosy.debug
SDK="$HOME/Android/Sdk"
ADB="$SDK/platform-tools/adb -s $SERIAL"
mkdir -p "$OUT"

if ! $ADB get-state >/dev/null 2>&1; then
    ANDROID_SDK_ROOT="$SDK" nohup "$SDK/emulator/emulator" -avd argosy_handheld_check -port 5556 \
        -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot-save \
        > "$OUT/emulator.log" 2>&1 &
fi
until [ "$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 2; done

key() { $ADB shell input keyevent "$1"; sleep "${2:-2}"; }
shot() { $ADB exec-out screencap -p > "$OUT/$1-$2.png"; }

run() {
    local tag=$1 apk=$2
    $ADB install -r "$apk" >/dev/null
    $ADB shell am start -a android.intent.action.MAIN -n "$PKG/com.nendo.argosy.ArgosyActivity" >/dev/null
    sleep 45
    shot "$tag" home
    key KEYCODE_BUTTON_X 4; shot "$tag" detail
    key KEYCODE_BUTTON_B 4
    key KEYCODE_BUTTON_START; shot "$tag" menu
    key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_DOWN
    key KEYCODE_BUTTON_A 4; shot "$tag" library
    key KEYCODE_BUTTON_A 5; shot "$tag" allgames
    $ADB shell am force-stop "$PKG"
    mkdir -p "$OUT/$tag-db"
    for f in alauncher.db alauncher.db-wal alauncher.db-shm; do
        $ADB exec-out run-as "$PKG" cat "databases/$f" > "$OUT/$tag-db/$f"
    done
    python3 -I "$ROOT/scripts/desktop/check/db_summary.py" "$OUT/$tag-db/alauncher.db" > "$OUT/$tag-db.json"
    mkdir -p "$OUT/$tag-datastore"
    for f in $($ADB shell run-as "$PKG" ls files/datastore | tr -d '\r'); do
        $ADB exec-out run-as "$PKG" cat "files/datastore/$f" > "$OUT/$tag-datastore/$f"
    done
    python3 -I "$ROOT/scripts/desktop/check/datastore_summary.py" "$OUT/$tag-datastore" > "$OUT/$tag-datastore.json"
}

$ADB logcat -c
run baseline "$BASELINE"
run candidate "$CANDIDATE"
crashes=$($ADB logcat -d | grep -c "FATAL EXCEPTION" || true)
python3 -I "$ROOT/scripts/desktop/check/compare.py" "$OUT" home detail menu library allgames
echo "FATAL EXCEPTION in logcat: $crashes"
echo "output: $OUT"
