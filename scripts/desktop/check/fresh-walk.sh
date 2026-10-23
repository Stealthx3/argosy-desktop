#!/usr/bin/env bash
# Runs a desktop walk against a throwaway data and cache dir (never the owner's ~/.local/share/argosy-desktop).
#   fresh-walk.sh <name> <desktop-drive steps...>   state kept in build/desktop-fresh/<name>/ until removed by hand
# FLATPAK_USER_DIR keeps host detection on the real per-user Flatpaks, which flatpak otherwise looks
# for under XDG_DATA_HOME.
set -euo pipefail
cd "$(dirname "$0")/../../.."
name=$1
shift
root="$PWD/build/desktop-fresh/$name"
mkdir -p "$root/data" "$root/cache"
export XDG_DATA_HOME="$root/data" XDG_CACHE_HOME="$root/cache"
export FLATPAK_USER_DIR="${FLATPAK_USER_DIR:-$HOME/.local/share/flatpak}"
scripts/desktop/check/desktop-drive.sh "build/desktop-check/fresh-$name" 30 "$@"
