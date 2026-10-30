#!/usr/bin/env bash
# usage: scale.sh <profile> <scale>
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
profile=$1 scale=$2
session_env='eval "$(systemctl --user show-environment | grep -E "^(DISPLAY|WAYLAND_DISPLAY|XDG_CURRENT_DESKTOP)=" | sed "s/^/export /")"'
case "$profile" in
    *-kde) "$here/exec.sh" "$profile" --user "$session_env; kscreen-doctor output.Virtual-1.scale.$scale >/dev/null 2>&1; kscreen-doctor -o | sed 's/\x1b\[[0-9;]*m//g' | grep -i -E 'scale|geometry'" ;;
    *) "$here/exec.sh" "$profile" --user "$session_env; gsettings set org.gnome.mutter experimental-features \"['scale-monitor-framebuffer', 'xwayland-native-scaling']\"; gdctl set --logical-monitor --primary --monitor Virtual-1 --scale $scale; gdctl show | grep -E 'Scale'" ;;
esac
