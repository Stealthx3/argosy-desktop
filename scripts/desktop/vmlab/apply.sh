#!/usr/bin/env bash
# Re-applies the lab settings (user-data.sh --settings-only) to a built VM and retakes `clean`:
# reverts to `clean`, boots once with a NoCloud seed under a new instance id (so cloud-init runs
# again), waits for the power-off, then replaces the snapshot. libguestfs (virt-customize) cannot
# run on this host, so this is how a built VM changes.   usage: apply.sh <profile>
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
. "$here/lab.sh"
profile=$1
dom=$(lab_domain "$profile")
seed=$(mktemp -d)
iso="$LAB/$dom.seed.iso"
"$here/user-data.sh" "$profile" --settings-only > "$seed/user-data"
printf 'instance-id: %s-%s\nlocal-hostname: %s\n' "$dom" "$(date +%s)" "$dom" > "$seed/meta-data"
genisoimage -quiet -output "$iso" -volid cidata -joliet -rock "$seed/user-data" "$seed/meta-data"
rm -rf "$seed"
virsh destroy "$dom" >/dev/null 2>&1 || true
virsh snapshot-revert "$dom" clean
virt-xml "$dom" --add-device --disk "$iso,device=cdrom,bus=sata" >/dev/null
virsh start "$dom" >/dev/null
for _ in $(seq 120); do [ "$(virsh domstate "$dom")" = "shut off" ] && break; sleep 5; done
state=$(virsh domstate "$dom")
virt-xml "$dom" --remove-device --disk device=cdrom >/dev/null
rm -f "$iso"
[ "$state" = "shut off" ] || { echo "$dom did not power off after the settings run" >&2; exit 1; }
virsh snapshot-delete "$dom" clean >/dev/null
virsh snapshot-create-as "$dom" clean "lab settings applied $(date -Is), powered off" >/dev/null
echo "$dom: settings applied, clean retaken"
