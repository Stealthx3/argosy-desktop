#!/usr/bin/env bash
# Per-desktop test (reset, alpha install, wizard, download, RetroArch launch, Close Argosy, SIGTERM).   usage: desktop-test.sh <profile> <out-dir>
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
log_wait() {
    local line
    for _ in $(seq $(( $2 / 3 ))); do
        line=$("$here/exec.sh" "$profile" --user "grep -E '$1' \$HOME/argosy-vm.log | tail -n 1" 2>/dev/null | tail -n 1)
        [ -n "$line" ] && { echo "${line:0:160}"; return 0; }
        sleep 3
    done
    echo "no '$1' in the log after $2 s"; return 1
}
walk() { "$here/walk.sh" "$profile" "$out" "$@" >> "$log" 2>&1; }

: > "$report"
note "desktop-test $profile $(date -Iseconds)"
check reset "$here/reset.sh" "$profile"
check install "$here/app.sh" "$profile" install
[ -n "${LAB_BUNDLE:-}" ] && check bundle "$here/app.sh" "$profile" bundle "$LAB_BUNDLE"
cores='d=$HOME/.var/app/org.libretro.RetroArch/config/retroarch/cores; mkdir -p $d; for c in fceumm mgba gambatte; do curl -sfLo /tmp/$c.zip https://buildbot.libretro.com/nightly/linux/x86_64/latest/${c}_libretro.so.zip && python3 -m zipfile -e /tmp/$c.zip $d || exit 1; done; flatpak info --user org.libretro.RetroArch | grep Version; ls $d | tr "\n" " "'
check retroarch "$here/exec.sh" "$profile" --long --user "flatpak install --user -y --noninteractive flathub org.libretro.RetroArch >/dev/null && $cores"
check session "$here/exec.sh" "$profile" --user 'systemctl --user show-environment | grep -E "^(XDG_SESSION_TYPE|XDG_CURRENT_DESKTOP)=" | tr "\n" " "'
check start "$here/app.sh" "$profile" start
sleep 45
walk shot:01-welcome
walk PAD:SOUTH wait:3 shot:02-display LEFT wait:1 PAD:SOUTH wait:4 \
    ENTER wait:2 "TYPE:$ROMM_FROM_GUEST" wait:1 ENTER wait:8 shot:03-pairing PAD:SOUTH wait:10
check sign-in-browser "$here/exec.sh" "$profile" --user 'for b in firefox chromium epiphany konqueror falkon; do command -v $b >/dev/null && found=1; done; [ -n "$found" ] || { echo "skipped: no browser in guest"; exit 0; }; p=$(pgrep -u tester -x -l "firefox|firefox-bin|chrome|chromium|epiphany|konqueror|falkon" | head -1); pkill -u tester -x "firefox|firefox-bin|chrome|chromium|epiphany|konqueror|falkon"; [ -n "$p" ] && echo "opened: $p" || { echo "no browser opened"; exit 1; }'
walk PAD:DPAD_DOWN wait:1 PAD:SOUTH wait:3 PAD:SOUTH wait:1 "RUN:$venv $root/scripts/desktop/testbed/pair-code.py" "TYPEFILE:$code" wait:2 ENTER wait:15 \
    shot:04-connected
walk PAD:SOUTH wait:3 PAD:SOUTH wait:3 shot:04b-emulators PAD:DPAD_DOWN wait:1 PAD:SOUTH wait:3 PAD:SOUTH wait:3 PAD:SOUTH wait:5
check sync log_wait 'syncLibrary returned' 300
walk wait:10 shot:05-home PAD:SOUTH
check download log_wait 'Download complete \|' 120
walk PAD:SOUTH
check launch log_wait 'HostGameProcess: Started' 60
walk wait:10 shot:06-game
"$here/exec.sh" "$profile" --user 'pkill -TERM -u tester -x retroarch' >/dev/null 2>&1
check session-end log_wait 'SessionEnd' 60
walk wait:3 PAD:RIGHT_STICK wait:2 PAD:DPAD_DOWN*8 wait:1 PAD:SOUTH wait:2 shot:07-close-confirm PAD:SOUTH
check close-argosy "$here/exec.sh" "$profile" --user 'for i in $(seq 20); do pgrep -u tester -f "^[/]app/lib/argosy-desktop/bin/argosy-desktop" >/dev/null || { echo "closed within ${i} s"; exit 0; }; sleep 1; done; echo "still running after 20 s"; exit 1'
"$here/exec.sh" "$profile" --user 'cat $HOME/argosy-vm.log' > "$out/argosy-vm.log" 2>&1
n=$(grep -E "Exception|FATAL|pure virtual" "$out/argosy-vm.log" | grep -c -v "Cannot create Linux GL context")
if [ "$n" = 0 ]; then note "PASS log-errors: 0 error lines in argosy-vm.log"; else note "FAIL log-errors: $n error lines in argosy-vm.log"; fail=1; fi
check restart "$here/app.sh" "$profile" start
sleep 45
check running "$here/app.sh" "$profile" status
check sigterm "$here/app.sh" "$profile" term
note "result $([ $fail = 0 ] && echo PASS || echo FAIL) (screens: $out)"
exit $fail
