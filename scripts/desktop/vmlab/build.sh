#!/usr/bin/env bash
# Builds one lab VM from its cloud image: a 40 GB overlay disk, cloud-init installs the desktop,
# qemu-guest-agent, Flatpak + Flathub and an autologin user `tester`, then powers off. The finished
# VM gets a 1920x1080 virtio display and an internal snapshot `clean`.
# usage: build.sh <profile>     log: $LAB/<domain>.build.log
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
. "$here/lab.sh"
profile=$1
dom=$(lab_domain "$profile")
base=$(lab_base_image "$profile")
disk="$LAB/$dom.qcow2"
log="$LAB/$dom.build.log"
[ -f "$base" ] || { echo "missing base image $base" >&2; exit 1; }
if virsh dominfo "$dom" >/dev/null 2>&1; then echo "$dom already exists" >&2; exit 1; fi

seed=$(mktemp -d)
"$here/user-data.sh" "$profile" > "$seed/user-data"
printf 'instance-id: %s\nlocal-hostname: %s\n' "$dom" "$dom" > "$seed/meta-data"
fmt=$(qemu-img info --output=json "$base" | python3 -I -c 'import json,sys; print(json.load(sys.stdin)["format"])')
qemu-img create -q -f qcow2 -F "$fmt" -b "$base" "$disk" 40G
{
echo "== $(date -Is) build $dom"
virt-install --name "$dom" --memory 8192 --vcpus 4 --cpu host-passthrough \
    --osinfo detect=on,require=off --import --disk "path=$disk,bus=virtio" \
    --network user,model=virtio --graphics spice,listen=none \
    --video virtio --channel unix,target.type=virtio,target.name=org.qemu.guest_agent.0 \
    --sound none --cloud-init "user-data=$seed/user-data,meta-data=$seed/meta-data" \
    --noautoconsole --noreboot --wait -1
for _ in $(seq 120); do [ "$(virsh domstate "$dom")" = "shut off" ] && break; sleep 5; done
echo "== $(date -Is) first boot finished (cloud-init powered off)"
} >> "$log" 2>&1
[ "$(virsh domstate "$dom")" = "shut off" ] || { echo "$dom did not power off; see $log" >&2; exit 1; }
rm -rf "$seed"

virt-xml "$dom" --edit --video model.type=virtio,model.heads=1 >> "$log" 2>&1
tmp=$(mktemp)
virsh dumpxml --inactive "$dom" > "$tmp"
python3 -I - "$tmp" <<'PY'
import sys, xml.etree.ElementTree as ET
path = sys.argv[1]
tree = ET.parse(path)
model = tree.find('./devices/video/model')
for r in model.findall('resolution'):
    model.remove(r)
ET.SubElement(model, 'resolution', x='1920', y='1080')
tree.write(path)
PY
virsh define "$tmp" >> "$log" 2>&1
rm -f "$tmp"

# Any power-off ends the first boot, including an interrupted one, and graphical.target is active even
# without a desktop, so require tester's autologin session on seat0 before snapshotting.
virsh start "$dom" >> "$log" 2>&1
ok=0
for _ in $(seq 120); do
    if virsh qemu-agent-command "$dom" '{"execute":"guest-ping"}' >/dev/null 2>&1 \
        && "$here/exec.sh" "$profile" 'loginctl list-sessions --no-legend | grep -w tester | grep -qw seat0' >> "$log" 2>&1; then ok=1; break; fi
    sleep 2
done
virsh shutdown "$dom" >> "$log" 2>&1
for _ in $(seq 60); do [ "$(virsh domstate "$dom")" = "shut off" ] && break; sleep 2; done
[ "$(virsh domstate "$dom")" = "shut off" ] || virsh destroy "$dom" >> "$log" 2>&1
[ "$ok" = 1 ] || { echo "$dom: no guest agent or tester desktop session after the first boot (interrupted build?); see $log" >&2; exit 1; }
virsh snapshot-create-as "$dom" clean "cloud-init done, desktop installed, powered off" >> "$log" 2>&1
echo "built $dom ($(du -h "$disk" | cut -f1) on disk); snapshot: clean"
