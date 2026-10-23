#!/usr/bin/env bash
# Runs the installed alpha Flatpak windowed, screenshots its window, prints its WM_CLASS and stops it.
# usage: scripts/desktop/flatpak/flatpak-shot.sh <out.png> [settle-seconds] [WxH]
set -u
out=$1
settle=${2:-20}
size=${3:-1920x1080}
app_id=io.github.stealthx3.ArgosyDesktop
root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$root"
mkdir -p build/desktop-logs "$(dirname "$out")"
log="build/desktop-logs/$(date +%Y%m%d-%H%M%S)-flatpak-run.log"
flatpak run --env=JAVA_TOOL_OPTIONS=-Dskiko.vsync.enabled=false "$app_id" --windowed --size="$size" > "$log" 2>&1 &
run=$!
win=""
for _ in $(seq 1 120); do
  win=$(xwininfo -root -tree 2>/dev/null | awk '/"Argosy"/ {print $1; exit}')
  [ -n "$win" ] && break
  kill -0 $run 2>/dev/null || break
  sleep 1
done
if [ -z "$win" ]; then
  echo "no Argosy window appeared; log $log"
  flatpak kill "$app_id" 2>/dev/null
  exit 1
fi
sleep "$settle"
import -window "$win" "$out" && echo "screenshot $out (window $win)"
xprop -id "$win" WM_CLASS
flatpak kill "$app_id" 2>/dev/null
wait $run 2>/dev/null
echo "log $log"
grep -E "Exception|Error|FATAL|at com\.nendo|SDL3" "$log" | head -40
