#!/usr/bin/env bash
# Runs a noisy command with its full output kept on disk, not in the agent's context.
# Prints the exit status, the elapsed time and only the lines that matter: Gradle task failures,
# compiler errors, test failures, exceptions. Read the full log with grep or an offset when needed.
#
# usage: scripts/desktop/quiet.sh <label> <command> [args...]
#   QUIET_TAIL=n   also print the last n lines (default 0)
set -u
label=$1
shift
root="$(cd "$(dirname "$0")/../.." && pwd)"
logdir="$root/build/desktop-logs"
mkdir -p "$logdir"
log="$logdir/$(date +%Y%m%d-%H%M%S)-$label.log"
start=$(date +%s)
"$@" > "$log" 2>&1
status=$?
elapsed=$(( $(date +%s) - start ))
echo "== $label: exit $status in ${elapsed}s, log $log"
grep -E '^e: |FAILED|BUILD (SUCCESSFUL|FAILED)|What went wrong|tests completed|[0-9]+ tests? (failed|passed)|FATAL EXCEPTION|Exception:|error:|ERROR' "$log" \
  | grep -v -E '^\s+at ' | head -40
if [ "${QUIET_TAIL:-0}" -gt 0 ]; then
  echo "-- last ${QUIET_TAIL} lines"
  tail -n "$QUIET_TAIL" "$log"
fi
exit $status
