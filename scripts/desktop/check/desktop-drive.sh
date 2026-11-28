#!/usr/bin/env bash
# usage: [DRIVE_TARGET=flatpak] desktop-drive.sh <out-dir> <settle-s> <step>... [-- WxH]; step = shot:<name> | wait:<s> | CLICK:<x>,<y> (content-area point) | <AWT VK name>[*n] | PAD:<SDL button>[*n] | PAD:<axis>=<value> | TYPE:<text> | TYPEFILE:<path> (types the file's first line, not echoed) | RUN:<command> (host command, run to completion)
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
# The test window is usually occluded; KWin then throttles its vsync to ~1 fps and Skiko's swapBuffers
# blocks the AWT thread, delaying input and repaints by seconds. Unthrottled frames keep runs deterministic.
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dskiko.vsync.enabled=false"
mkdir -p build/desktop-logs "$outdir"
log="build/desktop-logs/$(date +%Y%m%d-%H%M%S)-desktop-run.log"
app_id=io.github.stealthx3.icebreaker-client
if [ "${DRIVE_TARGET:-}" = flatpak ]; then
  # The installed Flatpak sees only its own ~/.var/app tree, so the pipe lives there.
  fifo="$HOME/.var/app/$app_id/cache/key-pipe"
  mkdir -p "$(dirname "$fifo")"
  rm -f "$fifo"; mkfifo "$fifo"
  # FLATPAK_RUN_ARGS: extra `flatpak run` options, not the data folders: flatpak run sets XDG_DATA_HOME and friends itself and ignores --env for them.
  # shellcheck disable=SC2086
  flatpak run ${FLATPAK_RUN_ARGS:-} --env=JAVA_TOOL_OPTIONS=-Dskiko.vsync.enabled=false "$app_id" --windowed --size="$size" --key-pipe="$fifo" > "$log" 2>&1 &
else
  fifo="$root/$outdir/key-pipe"
  rm -f "$fifo"; mkfifo "$fifo"
  ./gradlew --console=plain :desktopApp:run --args="--windowed --size=$size --key-pipe=$fifo" > "$log" 2>&1 &
fi
gradle=$!
win=""
for _ in $(seq 1 240); do
  win=$(xwininfo -root -tree 2>/dev/null | awk '/"IceBreaker"/ {print $1; exit}')
  [ -n "$win" ] && break
  kill -0 $gradle 2>/dev/null || break
  sleep 1
done
if [ -z "$win" ]; then
  echo "no IceBreaker window appeared; log $log"
  kill $gradle 2>/dev/null
  [ "${DRIVE_TARGET:-}" = flatpak ] && flatpak kill "$app_id" 2>/dev/null
  exit 1
fi
exec 3>"$fifo"
sleep "$settle"
for step in "${steps[@]}"; do
  case "$step" in
    shot:*) name="${step#shot:}"; tmp="$(dirname "$fifo")/shot-$name.png"; rm -f "$tmp"; echo "SHOT:$tmp" >&3
            for _ in $(seq 1 50); do [ -s "$tmp" ] && break; sleep 0.2; done
            if [ -s "$tmp" ]; then mv "$tmp" "$outdir/$name.png"; echo "screenshot $outdir/$name.png t=$(date +%s%3N)"; else echo "screenshot $name failed"; fi ;;
    wait:*) sleep "${step#wait:}" ;;
    TYPE:*) echo "$step" >&3; echo "type ${step#TYPE:} t=$(date +%s%3N)"; sleep 1 ;;
    TYPEFILE:*) echo "TYPE:$(head -n1 "${step#TYPEFILE:}")" >&3; echo "type <${step#TYPEFILE:}> t=$(date +%s%3N)"; sleep 1 ;;
    RUN:*) bash -c "${step#RUN:}"; echo "ran: exit $? t=$(date +%s%3N)" ;;
    *) key=${step%%\**}; n=1; [ "$key" != "$step" ] && n=${step#*\*}
       for _ in $(seq 1 "$n"); do echo "$key" >&3; echo "key $key t=$(date +%s%3N)"; sleep 0.6; done ;;
  esac
done
exec 3>&-
rm -f "$fifo"
if [ "${DRIVE_TARGET:-}" = flatpak ]; then flatpak kill "$app_id" 2>/dev/null; else pkill -f "com.nendo.argosy.desktop.MainKt" 2>/dev/null; fi
wait $gradle 2>/dev/null
echo "log $log"
grep -E "Exception|Error|FATAL|at com\.nendo" "$log" | head -40
