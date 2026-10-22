#!/usr/bin/env bash
# usage: pad-walk.sh android|desktop <outdir> shot:<name>|wait:<s>|TYPE:<text>|A|B|X|Y|START|SELECT|UP|DOWN|LEFT|RIGHT|ENTER[*n]...
set -u
mode=$1; out=$2; shift 2
steps=("$@")
cd /mnt/FileDrive/Personal/Projects/Git/argosy-desktop
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
      TYPE:*) $ADB shell input text "${s#TYPE:}"; sleep 1 ;;
      *) k=${s%%\**}; n=1; [ "$k" != "$s" ] && n=${s#*\*}
         case $k in A) c=96;; B) c=97;; X) c=99;; Y) c=100;; START) c=108;; SELECT) c=109;;
           UP) c=19;; DOWN) c=20;; LEFT) c=21;; RIGHT) c=22;; ENTER) c=66;; *) echo "unknown step $s"; exit 2;; esac
         for _ in $(seq 1 $n); do $ADB shell input keyevent $c; sleep 0.4; done ;;
    esac
  done
else
  d=()
  for s in "${steps[@]}"; do
    k=${s%%\**}; r=""; [ "$k" != "$s" ] && r="*${s#*\*}"
    case $k in A) d+=("PAD:SOUTH$r");; B) d+=("PAD:EAST$r");; X) d+=("PAD:WEST$r");; Y) d+=("PAD:NORTH$r");;
      START) d+=("PAD:START$r");; SELECT) d+=("PAD:BACK$r");; UP) d+=("PAD:DPAD_UP$r");; DOWN) d+=("PAD:DPAD_DOWN$r");;
      LEFT) d+=("PAD:DPAD_LEFT$r");; RIGHT) d+=("PAD:DPAD_RIGHT$r");; *) d+=("$s");; esac
  done
  scripts/desktop/check/desktop-drive.sh "$out" 40 "${d[@]}"
fi
