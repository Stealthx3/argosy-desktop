#!/usr/bin/env bash
# Moves or clicks a lab VM's pointer at screen pixels (1920x1080).   usage: mouse.sh <profile> move|click <x> <y>
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
. "$here/lab.sh"
dom=$(lab_domain "$1"); action=$2
ax=$(( $3 * 32767 / 1920 )) ay=$(( $4 * 32767 / 1080 ))
qmp() { virsh qemu-monitor-command "$dom" "{\"execute\":\"input-send-event\",\"arguments\":{\"events\":$1}}" >/dev/null; }
qmp "[{\"type\":\"abs\",\"data\":{\"axis\":\"x\",\"value\":$ax}},{\"type\":\"abs\",\"data\":{\"axis\":\"y\",\"value\":$ay}}]"
if [ "$action" = click ]; then
    sleep 0.2
    qmp '[{"type":"btn","data":{"down":true,"button":"left"}}]'
    sleep 0.1
    qmp '[{"type":"btn","data":{"down":false,"button":"left"}}]'
fi
