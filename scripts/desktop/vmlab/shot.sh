#!/usr/bin/env bash
# Saves a PNG of a lab VM's screen.   usage: shot.sh <profile> <out.png>
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
. "$here/lab.sh"
dom=$(lab_domain "$1")
tmp=$(mktemp --suffix=.ppm)
virsh screenshot "$dom" "$tmp" >/dev/null
magick "$tmp" "$2"
rm -f "$tmp"
echo "$2 $(magick identify -format '%wx%h' "$2")"
