#!/usr/bin/env bash
# Drives the Argosy Flatpak running in a lab VM (started by `app.sh <profile> start`) through its key
# pipe, the way desktop-drive.sh drives a host run. Screenshots are of the whole VM screen.
# usage: walk.sh <profile> <out-dir> <step>...
#   step = shot:<name> | wait:<s> | TYPE:<text> | TYPEFILE:<host file> | RUN:<host command> | <key>[*n]
#   key  = an AWT key name (ENTER, DOWN, ESCAPE, A ...) or PAD:<SDL button>
set -uo pipefail
here=$(cd "$(dirname "$0")" && pwd)
profile=$1 out=$2; shift 2
mkdir -p "$out"
pipe='$HOME/.var/app/io.github.stealthx3.ArgosyDesktop/cache/key-pipe'
send() { "$here/exec.sh" "$profile" --user "printf '%s\n' $(printf %q "$1") > $pipe"; }
for step in "$@"; do
    case "$step" in
        shot:*) "$here/shot.sh" "$profile" "$out/${step#shot:}.png" >/dev/null && echo "shot $out/${step#shot:}.png" ;;
        wait:*) sleep "${step#wait:}" ;;
        TYPE:*) send "$step"; echo "type ${step#TYPE:}"; sleep 1 ;;
        TYPEFILE:*) send "TYPE:$(head -n1 "${step#TYPEFILE:}")"; echo "type <${step#TYPEFILE:}>"; sleep 1 ;;
        RUN:*) bash -c "${step#RUN:}"; echo "ran: exit $?" ;;
        *) key=${step%%\**}; n=1; [ "$key" != "$step" ] && n=${step#*\*}
           for _ in $(seq 1 "$n"); do send "$key"; echo "key $key"; sleep 0.6; done ;;
    esac
done
