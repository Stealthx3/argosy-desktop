#!/usr/bin/env bash
# Reverts a lab VM to its `clean` snapshot and starts it.   usage: reset.sh <profile>
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
. "$here/lab.sh"
dom=$(lab_domain "$1")
virsh destroy "$dom" >/dev/null 2>&1 || true
virsh snapshot-revert "$dom" clean
virsh start "$dom" >/dev/null
for _ in $(seq 120); do
    virsh qemu-agent-command "$dom" '{"execute":"guest-ping"}' >/dev/null 2>&1 && break
    sleep 2
done
virsh qemu-agent-command "$dom" '{"execute":"guest-ping"}' >/dev/null 2>&1 || { echo "$dom: guest agent did not answer in 240 s" >&2; exit 1; }
# The agent answers before the network is up; a Flathub or Pages fetch right after would fail to resolve.
"$here/exec.sh" "$1" 'for i in $(seq 60); do getent hosts flathub.org >/dev/null && exit 0; sleep 2; done; exit 1' >/dev/null \
    || { echo "$dom: no DNS after 120 s" >&2; exit 1; }
echo "$dom up"
