#!/usr/bin/env bash
# Launches the desktop app windowed, waits for its window, screenshots it and closes it.
# usage: scripts/desktop/check/desktop-shot.sh <out.png> [settle-seconds] [WxH]
# The app's own output goes to build/desktop-logs/<time>-desktop-run.log.
set -u
out=$1
settle=${2:-20}
size=${3:-1920x1080}
root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$root"
export JAVA_HOME=${JAVA_HOME:-$HOME/.jdks/jdk-21.0.12.1+1}
mkdir -p build/desktop-logs "$(dirname "$out")"
log="build/desktop-logs/$(date +%Y%m%d-%H%M%S)-desktop-run.log"
./gradlew --console=plain :desktopApp:run --args="--windowed --size=$size" > "$log" 2>&1 &
gradle=$!
win=""
for _ in $(seq 1 240); do
  win=$(xwininfo -root -tree 2>/dev/null | awk '/"Argosy"/ {print $1; exit}')
  [ -n "$win" ] && break
  kill -0 $gradle 2>/dev/null || break
  sleep 1
done
if [ -z "$win" ]; then
  echo "no Argosy window appeared; log $log"
  kill $gradle 2>/dev/null
  exit 1
fi
sleep "$settle"
import -window "$win" "$out" && echo "screenshot $out (window $win)"
pkill -f "com.nendo.argosy.desktop.MainKt" 2>/dev/null
wait $gradle 2>/dev/null
echo "log $log"
grep -E "Exception|Error|FATAL|at com\.nendo" "$log" | head -40
