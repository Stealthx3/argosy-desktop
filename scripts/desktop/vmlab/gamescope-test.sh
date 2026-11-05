#!/usr/bin/env bash
set -uo pipefail
here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../../.." && pwd)
. "$here/lab.sh"
profile=$1 out=$2
mkdir -p "$out"
report=$out/report.txt
: > "$report"
fail=0
venv=$root/build/desktop-testbed/venv/bin/python
code=$root/build/desktop-testbed/pair-code.txt
app=io.github.stealthx3.ArgosyDesktop
base="\$HOME/.var/app/$app"
pipe="$base/cache/key-pipe"

note() { echo "$*" | tee -a "$report"; }
check() {
    local name=$1; shift
    local text
    if text=$("$@" 2>&1); then note "PASS $name: $(echo "$text" | tail -n 1 | cut -c1-140)"
    else note "FAIL $name: $(echo "$text" | tail -n 1 | cut -c1-140)"; fail=1; fi
}
guest() { "$here/exec.sh" "$profile" --user "$1"; }
keys() {
    local step key n
    for step in "$@"; do
        case "$step" in
            wait:*) sleep "${step#wait:}" ;;
            shot:*) shot "${step#shot:}" ;;
            TYPEFILE:*) guest "printf '%s\n' 'TYPE:$(head -n1 "${step#TYPEFILE:}")' > $pipe" >/dev/null; sleep 1 ;;
            RUN:*) bash -c "${step#RUN:}" >/dev/null 2>&1 ;;
            TYPE:*) guest "printf '%s\n' '$step' > $pipe" >/dev/null; sleep 1 ;;
            *) key=${step%%\**}; n=1; [ "$key" != "$step" ] && n=${step#*\*}
               for _ in $(seq 1 "$n"); do guest "printf '%s\n' '$key' > $pipe" >/dev/null; sleep 0.6; done ;;
        esac
    done
}
shot() {
    guest "DISPLAY=:1 xwd -silent -name Argosy | magick xwd:- /tmp/gs.png; base64 -w0 /tmp/gs.png" | head -n 1 | base64 -d > "$out/$1.png" 2>/dev/null
}
app_pid() { guest "pgrep -u tester -f '^[/]app/lib/argosy-desktop/bin/argosy-desktop' | head -1" | head -n 1; }
gs_pid() { guest "pgrep -u tester -f '^gamescope -W' | head -1" | head -n 1; }
start() {
    guest "eval \"\$(systemctl --user show-environment | grep -E '^(DISPLAY|WAYLAND_DISPLAY|XDG_SESSION_TYPE)=' | sed 's/^/export /')\"; mkdir -p $base/cache; rm -f $pipe; mkfifo $pipe; rm -f \$HOME/gs-exit; setsid nohup sh -c 'gamescope -W 1280 -H 800 --backend headless -- flatpak run $app --key-pipe=$pipe; echo \$? > \$HOME/gs-exit' > \$HOME/argosy-vm.log 2>&1 < /dev/null &" >/dev/null
}

note "gamescope-test $profile $(date -Iseconds)"
guest "pkill -f '^gamescope -W'; flatpak kill $app" >/dev/null 2>&1
sleep 2
guest "rm -rf $base/data/argosy-desktop $base/cache/argosy-desktop" >/dev/null
start
sleep 60
check running app_pid
check env-seen guest "tr '\0' '\n' < /proc/\$(pgrep -u tester -f '^[/]app/lib/argosy-desktop/bin/argosy-desktop' | head -1)/environ | grep -E '^GAMESCOPE_WAYLAND_DISPLAY='"
keys shot:01-welcome PAD:SOUTH wait:6 shot:02-display PAD:SOUTH wait:8 ENTER wait:3 "TYPE:http://10.0.2.2:8093" wait:2 ENTER wait:12 PAD:SOUTH wait:10 PAD:DPAD_DOWN wait:1 PAD:SOUTH wait:3 PAD:SOUTH wait:1 "RUN:$venv $root/scripts/desktop/testbed/pair-code.py --vmlab" "TYPEFILE:$code" wait:2 ENTER wait:30 shot:03-connected PAD:SOUTH wait:3 PAD:SOUTH wait:3 PAD:DPAD_DOWN wait:1 PAD:SOUTH wait:3 PAD:SOUTH wait:3 PAD:SOUTH wait:30 shot:04-home
note "stored display profile: $(guest "strings $base/data/argosy-desktop/datastore/settings.preferences_pb | grep -A1 display_profile | tail -n 1" | tail -n 1)"
note "games synced: $(guest "python3 -c \"import sqlite3,os;print(sqlite3.connect(os.path.expanduser('~/.var/app/$app/data/argosy-desktop/alauncher.db')).execute('select count(*) from games').fetchone()[0])\"" | tail -n 1)"
keys PAD:START wait:3 PAD:DPAD_DOWN*8 wait:1 shot:05-drawer-close PAD:SOUTH wait:2 shot:06-close-dialog PAD:DPAD_RIGHT wait:1 PAD:SOUTH wait:8
check close-ends-session guest "if pgrep -u tester -f '^gamescope -W' >/dev/null; then echo still running; exit 1; fi; echo \"gamescope gone, exit \$(cat \$HOME/gs-exit 2>/dev/null)\""
start
sleep 60
keys shot:06b-reopened PAD:START wait:3 PAD:DPAD_DOWN*7 wait:1 PAD:SOUTH wait:5 PAD:DPAD_UP*2 wait:2 shot:07-settings-factory
before=$(app_pid)
gs_before=$(gs_pid)
keys PAD:SOUTH wait:3 shot:08-warning PAD:DPAD_RIGHT wait:1 PAD:SOUTH wait:3 shot:09-confirm PAD:DPAD_RIGHT wait:1 PAD:SOUTH
restarted=
for i in $(seq 60); do
    sleep 2
    now=$(app_pid)
    case "$now" in ''|"$before") ;; *) restarted=$i; break ;; esac
done
if [ -n "$restarted" ]; then note "PASS restart: new process after $((restarted * 2)) s"; else note "FAIL restart: no new process"; fail=1; fi
if [ "$(gs_pid)" = "$gs_before" ] && [ -n "$gs_before" ]; then note "PASS session-kept: gamescope pid $gs_before unchanged"; else note "FAIL session-kept: gamescope changed or gone"; fail=1; fi
sleep 45
keys shot:10-wizard
check wiped guest "python3 -c \"import sqlite3,os,sys;n=sqlite3.connect(os.path.expanduser('~/.var/app/$app/data/argosy-desktop/alauncher.db')).execute('select count(*) from games').fetchone()[0];print('games',n);sys.exit(n)\""
guest "tr -d '\r' < \$HOME/argosy-vm.log | grep -E 'Exception|FATAL' | grep -v 'Cannot create Linux GL' | cut -c1-140 | sort | uniq -c | head -5" | tee -a "$report"
guest "pkill -TERM -f '^[/]app/lib/argosy-desktop/bin/argosy-desktop'" >/dev/null 2>&1
sleep 8
check session-ends-with-app guest "if pgrep -u tester -f '^gamescope -W' >/dev/null; then echo still running; exit 1; fi; echo \"gamescope gone, exit \$(cat \$HOME/gs-exit 2>/dev/null)\""
note "result $([ $fail = 0 ] && echo PASS || echo FAIL) (screens: $out)"
exit $fail
