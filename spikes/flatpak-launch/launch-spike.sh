#!/bin/sh
# Runs inside the sandbox. Usage: launch-spike <rom-file-name> [core-name] [retroarch-args...]
# The ROM must sit in ~/Games/Argosy on the host; core defaults to a
# RetroArch core name such as mgba_libretro.
set -eu
ROM_DIR="$HOME/Games/Argosy"
ROM="$ROM_DIR/${1:?usage: launch-spike <rom-file-name> [core-name]}"
CORE="${2:-mgba_libretro}"
shift; [ $# -gt 0 ] && shift
# Remaining arguments go to RetroArch (e.g. --max-frames=N to exit on its own).
EMU=org.libretro.RetroArch

echo "== sandbox check: can we see the ROM? =="
ls -l "$ROM"

echo "== host check: which emulator Flatpaks are installed? =="
flatpak-spawn --host flatpak list --app --columns=application | grep -i -E "retroarch|dolphin|duckstation|pcsx2|ppsspp|melonds" || true

echo "== host launch: $EMU with $CORE =="
START=$(date +%s)
# The core path is the RetroArch Flatpak's own cores folder on the host.
STATUS=0
flatpak-spawn --host flatpak run "$EMU" \
  -L "$HOME/.var/app/$EMU/config/retroarch/cores/$CORE.so" "$@" "$ROM" || STATUS=$?
END=$(date +%s)
echo "== emulator exited with status $STATUS after $((END - START)) s =="
echo "(the wait above is how Argosy Desktop will detect play-session start and end)"
