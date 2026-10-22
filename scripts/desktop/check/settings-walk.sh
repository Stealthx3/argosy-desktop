#!/usr/bin/env bash
# usage: settings-walk.sh android|desktop <outdir>; the same pad walk on the check AVD (adb keyevents) or desktop (desktop-drive.sh): Home, drawer to Settings, then open, shoot and leave each of the first 20 rows.
set -u
mode=$1; out=$2
cd /mnt/FileDrive/Personal/Projects/Git/argosy-desktop
steps=(shot:home START wait:2 DOWN*8 wait:1 A wait:3 shot:settings)
for i in $(seq -w 0 19); do steps+=(A wait:3 shot:row$i B wait:2 DOWN wait:1); done
steps+=(shot:settings-end)
if [ "$mode" = android ]; then
  ADB="$HOME/Android/Sdk/platform-tools/adb -s emulator-5556"
  mkdir -p "$out"
  $ADB shell am force-stop com.nendo.argosy.debug
  $ADB shell monkey -p com.nendo.argosy.debug -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  sleep 12
  for s in "${steps[@]}"; do
    case "$s" in
      shot:*) $ADB exec-out screencap -p > "$out/${s#shot:}.png" ;;
      wait:*) sleep "${s#wait:}" ;;
      *) k=${s%%\**}; n=1; [ "$k" != "$s" ] && n=${s#*\*}
         case $k in A) c=96;; B) c=97;; START) c=108;; DOWN) c=20;; UP) c=19;; esac
         for _ in $(seq 1 $n); do $ADB shell input keyevent $c; sleep 0.4; done ;;
    esac
  done
else
  d=()
  for s in "${steps[@]}"; do
    k=${s%%\**}; r=""; [ "$k" != "$s" ] && r="*${s#*\*}"
    case $k in A) d+=("PAD:SOUTH$r");; B) d+=("PAD:EAST$r");; START) d+=("PAD:START$r");; DOWN) d+=("PAD:DPAD_DOWN$r");; *) d+=("$s");; esac
  done
  scripts/desktop/check/desktop-drive.sh "$out" 40 "${d[@]}"
fi
