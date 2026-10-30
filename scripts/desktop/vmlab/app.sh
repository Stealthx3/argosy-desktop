#!/usr/bin/env bash
# Installs, starts and stops the alpha Flatpak inside a lab VM, as `tester` in the desktop session.
#   app.sh <profile> install            install from the public alpha .flatpakref (runtime from Flathub)
#   app.sh <profile> bundle <file>      install a local .flatpak bundle over it (served from the host)
#   app.sh <profile> start [args...]    start it with a key pipe, log at ~tester/argosy-vm.log
#   app.sh <profile> term|kill|status   SIGTERM the app (exit-time check) / flatpak kill / process list
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
. "$here/lab.sh"
profile=$1 action=$2; shift 2
app=io.github.stealthx3.ArgosyDesktop
ref=https://stealthx3.github.io/argosy-desktop/$app.flatpakref
pipe="\$HOME/.var/app/$app/cache/key-pipe"
session_env='eval "$(systemctl --user show-environment | grep -E "^(DISPLAY|WAYLAND_DISPLAY|XDG_SESSION_TYPE|XDG_CURRENT_DESKTOP|XAUTHORITY)=" | sed "s/^/export /")"'
case "$action" in
    install) "$here/exec.sh" "$profile" --long --user "flatpak install --user -y --noninteractive --from $ref; flatpak info --user $app | grep -E 'Version|Commit'" ;;
    bundle)
        dir=$(cd "$(dirname "$1")" && pwd) name=$(basename "$1")
        python3 -I -m http.server 8765 --bind 127.0.0.1 --directory "$dir" >/dev/null 2>&1 & server=$!
        trap 'kill $server' EXIT
        sleep 1
        "$here/exec.sh" "$profile" --long --user "curl -sfo /var/tmp/$name http://10.0.2.2:8765/$name && flatpak install --user -y --noninteractive --reinstall --bundle /var/tmp/$name && flatpak info --user $app | grep -E 'Version|Commit'" ;;
    start)
        "$here/exec.sh" "$profile" --user "$session_env; mkdir -p \$HOME/.var/app/$app/cache; rm -f $pipe; mkfifo $pipe; echo \"session \$XDG_SESSION_TYPE \$XDG_CURRENT_DESKTOP DISPLAY=\$DISPLAY WAYLAND_DISPLAY=\$WAYLAND_DISPLAY\"; setsid nohup flatpak run $app --key-pipe=$pipe $* > \$HOME/argosy-vm.log 2>&1 < /dev/null &" ;;
    term) "$here/exec.sh" "$profile" --user "pid=\$(pgrep -u tester -f '^[/]app/lib/argosy-desktop/bin/argosy-desktop' | head -1); [ -n \"\$pid\" ] || { echo 'not running'; exit 1; }; t0=\$(date +%s%N); kill -TERM \$pid; while kill -0 \$pid 2>/dev/null; do sleep 0.1; [ \$(( (\$(date +%s%N) - t0) / 1000000 )) -gt 10000 ] && { echo 'still running after 10 s'; exit 1; }; done; echo \"exited in \$(( (\$(date +%s%N) - t0) / 1000000 )) ms\"" ;;
    kill) "$here/exec.sh" "$profile" --user "flatpak kill $app 2>/dev/null; echo killed" ;;
    status) "$here/exec.sh" "$profile" --user "pgrep -a -u tester -f '^[/]app/lib/argosy-desktop/bin/argosy-desktop' | cut -c1-120; [ \${PIPESTATUS[0]} = 0 ] || echo 'not running'" ;;
    *) echo "unknown action $action" >&2; exit 2 ;;
esac
