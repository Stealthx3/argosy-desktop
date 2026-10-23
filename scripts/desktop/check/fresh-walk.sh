#!/usr/bin/env bash
# Runs a desktop walk against a throwaway data and cache dir (never the owner's ~/.local/share/argosy-desktop).
#   fresh-walk.sh <name> <desktop-drive steps...>   state kept in build/desktop-fresh/<name>/ until removed by hand
set -euo pipefail
cd "$(dirname "$0")/../../.."
name=$1
shift
root="$PWD/build/desktop-fresh/$name"
mkdir -p "$root/data" "$root/cache"
export XDG_DATA_HOME="$root/data" XDG_CACHE_HOME="$root/cache"
scripts/desktop/check/desktop-drive.sh "build/desktop-check/fresh-$name" 30 "$@"
