#!/usr/bin/env bash
# usage: desktop-drive.sh <out-dir> <settle-s> <step>... [-- WxH]; step = shot:<name> | wait:<s> | <AWT VK name>[*n] | PAD:<SDL button>[*n] | PAD:<axis>=<value>
set -u
outdir=$1
settle=$2
shift 2
size=1920x1080
steps=()
while [ $# -gt 0 ]; do
  if [ "$1" = "--" ]; then size=$2; break; fi
  steps+=("$1")
  shift
done
root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$root"
export JAVA_HOME=${JAVA_HOME:-$HOME/.jdks/jdk-21.0.12.1+1}
mkdir -p build/desktop-logs "$outdir"
log="build/desktop-logs/$(date +%Y%m%d-%H%M%S)-desktop-run.log"
fifo="$outdir/key-pipe"
rm -f "$fifo"; mkfifo "$fifo"
./gradlew --console=plain :desktopApp:run --args="--windowed --size=$size --key-pipe=$root/$fifo" > "$log" 2>&1 &
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
exec 3>"$fifo"
sleep "$settle"
for step in "${steps[@]}"; do
  case "$step" in
    shot:*) import -window "$win" "$outdir/${step#shot:}.png" && echo "screenshot $outdir/${step#shot:}.png" ;;
    wait:*) sleep "${step#wait:}" ;;
    *) key=${step%%\**}; n=1; [ "$key" != "$step" ] && n=${step#*\*}
       for _ in $(seq 1 "$n"); do echo "$key" >&3; sleep 0.6; done ;;
  esac
done
exec 3>&-
rm -f "$fifo"
pkill -f "com.nendo.argosy.desktop.MainKt" 2>/dev/null
wait $gradle 2>/dev/null
echo "log $log"
grep -E "Exception|Error|FATAL|at com\.nendo" "$log" | head -40
