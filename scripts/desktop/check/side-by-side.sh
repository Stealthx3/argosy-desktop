#!/usr/bin/env bash
# Desktop vs Android screens for the Phase 5 side-by-side check. Walks the same screens on the check
# AVD (emulator-5556, testbed-signed) and on desktop fresh-walk data (never the owner's data), only
# moving and opening screens (A only on rows that open a section), then tiles each pair into <out>/pairs-*.png.
# usage: side-by-side.sh <out-dir> <fresh-walk-name>
set -u
out=$1; fresh=$2
cd /mnt/FileDrive/Personal/Projects/Git/argosy-desktop
mkdir -p "$out/android" "$out/desktop"
ADB="$HOME/Android/Sdk/platform-tools/adb -s emulator-5556"
android() {
  $ADB install -r --abi arm64-v8a app/build/outputs/apk/debug/app-debug.apk >/dev/null
  $ADB shell am force-stop com.nendo.argosy.debug
  $ADB shell monkey -p com.nendo.argosy.debug -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
  sleep 15
  for s in "$@"; do
    case "$s" in
      shot:*) $ADB exec-out screencap -p > "$out/android/${s#shot:}.png" ;;
      wait:*) sleep "${s#wait:}" ;;
      *) k=${s%%\**}; n=1; [ "$k" != "$s" ] && n=${s#*\*}
         case $k in A) c=96;; B) c=97;; X) c=99;; START) c=108;; R3) c=107;; DOWN) c=20;; RIGHT) c=22;; R1) c=103;; esac
         for _ in $(seq 1 "$n"); do $ADB shell input keyevent $c; sleep 0.4; done ;;
    esac
  done
}
desktop() {
  local d=()
  for s in "$@"; do
    k=${s%%\**}; r=""; [ "$k" != "$s" ] && r="*${s#*\*}"
    case $k in A) d+=("PAD:SOUTH$r");; B) d+=("PAD:EAST$r");; X) d+=("PAD:WEST$r");; START) d+=("PAD:START$r");; R3) d+=("PAD:RIGHT_STICK$r");;
      DOWN) d+=("PAD:DPAD_DOWN$r");; RIGHT) d+=("PAD:DPAD_RIGHT$r");; R1) d+=("PAD:RIGHT_SHOULDER$r");; *) d+=("$s");; esac
  done
  timeout 600 scripts/desktop/check/fresh-walk.sh "$fresh" wait:30 "${d[@]}" >/dev/null 2>&1
  cp build/desktop-check/fresh-"$fresh"/*.png "$out/desktop/"
}
common=(shot:home X wait:4 shot:details B wait:3 START wait:2 shot:drawer B wait:2 R3 wait:2 shot:quick B wait:2
  R1 wait:3 shot:page2 R1 wait:3 shot:page3 R1 wait:3 shot:page4)
sections=(shot:settings)
for s in theme interface navigation audio displays; do sections+=(A wait:3 "shot:set-$s" B wait:2 DOWN wait:1); done
sections+=(DOWN*7 wait:1 shot:settings-lower)
android "${common[@]}" START wait:2 DOWN*8 A wait:3 "${sections[@]}"
desktop "${common[@]}" START wait:2 DOWN*7 A wait:3 "${sections[@]}"
python3 -I - "$out" <<'PY'
import sys
from pathlib import Path
from PIL import Image
out = Path(sys.argv[1]); names = sorted(p.stem for p in (out / "android").glob("*.png"))
rows = []
for n in names:
    a, d = out / "android" / f"{n}.png", out / "desktop" / f"{n}.png"
    if not d.exists(): continue
    ims = [Image.open(x).convert("RGB") for x in (a, d)]
    ims = [im.resize((640, int(im.height * 640 / im.width))) for im in ims]
    h = max(im.height for im in ims); row = Image.new("RGB", (1290, h), "white")
    row.paste(ims[0], (0, 0)); row.paste(ims[1], (650, 0)); rows.append((n, row))
for i in range(0, len(rows), 3):
    chunk = rows[i:i + 3]; H = sum(r.height + 10 for _, r in chunk)
    sheet = Image.new("RGB", (1290, H), "white"); y = 0
    for _, r in chunk: sheet.paste(r, (0, y)); y += r.height + 10
    sheet.save(out / f"pairs-{i // 3}.png"); print(f"pairs-{i // 3}:", ", ".join(n for n, _ in chunk))
PY
