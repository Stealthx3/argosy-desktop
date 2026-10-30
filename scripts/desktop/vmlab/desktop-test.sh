#!/usr/bin/env bash
# Per-desktop smoke test (reset, alpha install, wizard, Home, SIGTERM).   usage: desktop-test.sh <profile> <out-dir>
set -uo pipefail
here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../../.." && pwd)
. "$here/lab.sh"
profile=$1 out=$2
mkdir -p "$out"
report=$out/report.txt
log=$out/test.log
: > "$log"
venv=$root/build/desktop-testbed/venv/bin/python
code=$root/build/desktop-testbed/pair-code.txt
fail=0

note() { echo "$*" | tee -a "$report"; }
check() {
    local name=$1; shift
    if out_text=$("$@" 2>&1); then note "PASS $name: $(echo "$out_text" | tail -n 1)"
    else note "FAIL $name: $(echo "$out_text" | tail -n 1)"; fail=1; fi
    echo "== $name" >> "$log"; echo "$out_text" >> "$log"
}
walk() { "$here/walk.sh" "$profile" "$out" "$@" >> "$log" 2>&1; }

: > "$report"
note "desktop-test $profile $(date -Iseconds)"
check reset "$here/reset.sh" "$profile"
check install "$here/app.sh" "$profile" install
[ -n "${LAB_BUNDLE:-}" ] && check bundle "$here/app.sh" "$profile" bundle "$LAB_BUNDLE"
check session "$here/exec.sh" "$profile" --user 'systemctl --user show-environment | grep -E "^(XDG_SESSION_TYPE|XDG_CURRENT_DESKTOP)=" | tr "\n" " "'
check start "$here/app.sh" "$profile" start
sleep 45
walk shot:01-welcome
walk PAD:SOUTH wait:3 shot:02-display LEFT wait:1 PAD:SOUTH wait:4 \
    ENTER wait:2 "TYPE:$ROMM_FROM_GUEST" wait:1 ENTER wait:8 shot:03-pairing PAD:SOUTH wait:10
check sign-in-browser "$here/exec.sh" "$profile" --user 'for b in firefox chromium epiphany konqueror falkon; do command -v $b >/dev/null && found=1; done; [ -n "$found" ] || { echo "skipped: no browser in guest"; exit 0; }; p=$(pgrep -u tester -x -l "firefox|firefox-bin|chrome|chromium|epiphany|konqueror|falkon" | head -1); pkill -u tester -x "firefox|firefox-bin|chrome|chromium|epiphany|konqueror|falkon"; [ -n "$p" ] && echo "opened: $p" || { echo "no browser opened"; exit 1; }'
walk PAD:DPAD_DOWN wait:1 PAD:SOUTH wait:3 PAD:SOUTH wait:1 "RUN:$venv $root/scripts/desktop/testbed/pair-code.py" "TYPEFILE:$code" wait:2 ENTER wait:15 \
    shot:04-connected
walk PAD:SOUTH wait:3 PAD:SOUTH wait:3 PAD:SOUTH wait:3 PAD:SOUTH wait:3 PAD:SOUTH wait:3 PAD:SOUTH wait:20 shot:05-home
check running "$here/app.sh" "$profile" status
check games "$here/exec.sh" "$profile" --user 'db=$(ls $HOME/.var/app/io.github.stealthx3.ArgosyDesktop/data/argosy-desktop/*.db 2>/dev/null | head -1); [ -n "$db" ] && command -v sqlite3 >/dev/null && echo "games $(sqlite3 "$db" "select count(*) from games")" || echo "games: no sqlite3 in guest, see 05-home"'
check log-errors "$here/exec.sh" "$profile" --user 'n=$(grep -E "Exception|FATAL|pure virtual" $HOME/argosy-vm.log | grep -c -v "Cannot create Linux GL context"); echo "$n error lines in argosy-vm.log"; [ "$n" = 0 ]'
"$here/exec.sh" "$profile" --user 'cat $HOME/argosy-vm.log' > "$out/argosy-vm.log" 2>&1
check sigterm "$here/app.sh" "$profile" term
note "result $([ $fail = 0 ] && echo PASS || echo FAIL) (screens: $out; review 05-home for covers)"
exit $fail
