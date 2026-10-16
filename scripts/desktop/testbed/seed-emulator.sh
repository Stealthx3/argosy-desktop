#!/usr/bin/env bash
# Signs the Argosy debug app on the emulator in to the testbed with the minted client token,
# via the debug-only DebugSeedReceiver, then relaunches it so it routes to Home.
# Does not install, clear or uninstall anything.
#
# usage: scripts/desktop/testbed/seed-emulator.sh [serial]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
SERIAL="${1:-emulator-5554}"
ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
PKG=com.nendo.argosy.debug
set -a
. "$ROOT/build/desktop-testbed/credentials.env"
set +a

"$ADB" -s "$SERIAL" shell appops set --uid "$PKG" MANAGE_EXTERNAL_STORAGE allow
"$ADB" -s "$SERIAL" shell am broadcast -n "$PKG/com.nendo.argosy.debugtools.DebugSeedReceiver" \
    --es url "$ROMM_EMULATOR_URL" --es token "$ROMM_CLIENT_TOKEN" >/dev/null
sleep 5
"$ADB" -s "$SERIAL" shell am force-stop "$PKG"
"$ADB" -s "$SERIAL" shell am start -a android.intent.action.MAIN -n "$PKG/com.nendo.argosy.ArgosyActivity" >/dev/null
echo "seeded $SERIAL with $ROMM_EMULATOR_URL and relaunched $PKG"
