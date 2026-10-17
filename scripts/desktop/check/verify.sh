#!/usr/bin/env bash
# Full verification of the current commit against plain upstream: Android gate (desktop build and
# tests, debug APK, unit tests), A/B screens and data from the save-baseline snapshot, and the
# save-sync live proof compared with a same-day plain proof.
#   verify.sh <label> <plain-proof-dir>
set -u
label=$1
plain=$2
out=build/desktop-check
log=$out/verify-$label.log
mkdir -p "$out"
rm -f app/build/outputs/apk/debug/app-debug.apk
./gradlew :shared:compileKotlinDesktop :shared:desktopTest :desktopApp:assemble :app:assembleDebug :app:testDebugUnitTest \
    --console=plain > "$log" 2>&1
gate=$?
echo "gate=$gate"
grep -E "What went wrong|BUILD" -A2 "$log" | head -6
[ "$gate" -ne 0 ] && exit 1
python3 -I - <<'PY'
import glob, re
for d in ["app/build/test-results/testDebugUnitTest", "shared/build/test-results/desktopTest"]:
    t = f = 0
    for p in glob.glob(d + "/*.xml"):
        m = re.search(r'tests="(\d+)" skipped="(\d+)" failures="(\d+)" errors="(\d+)"', open(p).read(3000))
        t += int(m[1])
        f += int(m[3]) + int(m[4])
    print(d, "tests", t, "failed", f)
PY
apk=build/desktop-baseline/app-debug-$label.apk
cp app/build/outputs/apk/debug/app-debug.apk "$apk"
SNAPSHOT=saves-baseline BASELINE=build/desktop-baseline/app-debug-upstream-v2.24.0.apk INSTALL_ARGS="--abi arm64-v8a" \
    scripts/desktop/check/ab-check.sh "$apk" "$out/$label-en" 2>&1 | grep -E "^(home|detail|menu|library|allgames|database|datastore|FATAL)"
scripts/desktop/check/save-proof.sh "$apk" "$out/save-proof/$label" > "$out/save-proof-$label.log" 2>&1
tail -2 "$out/save-proof/$label/run.log"
scripts/desktop/testbed/proof-diff.sh "$plain" "$out/save-proof/$label" | tail -1
~/Android/Sdk/platform-tools/adb -s emulator-5556 emu kill >/dev/null 2>&1
exit 0
