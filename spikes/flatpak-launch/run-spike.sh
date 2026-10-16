#!/bin/sh
# Builds and runs the launch spike on the host. Needs flatpak-builder (host binary or the
# org.flatpak.Builder Flatpak), the Freedesktop 24.08 runtime/SDK, and the RetroArch Flatpak with a core.
# Usage: run-spike.sh <rom-file-name> [core-name] [retroarch-args...]
set -eu
cd "$(dirname "$0")"
if command -v flatpak-builder >/dev/null 2>&1; then
  BUILDER="flatpak-builder"
else
  BUILDER="flatpak run org.flatpak.Builder"
fi
# rofiles-fuse cannot mount from inside the Builder Flatpak sandbox; it is only an optimisation.
$BUILDER --user --install --force-clean --disable-rofiles-fuse build-dir io.github.stealthx3.ArgosyLaunchSpike.yml
# RetroArch's own sandbox must be able to read the shared ROM folder. Check rather than change the
# user's RetroArch permissions; print the override to apply if access is missing.
PERMS="$(flatpak info --show-permissions org.libretro.RetroArch)"
case "$PERMS" in
  *"filesystems="*host\;*|*"filesystems="*"~/Games/Argosy"*|*"filesystems="*home\;*) ;;
  *) echo "RetroArch cannot read ~/Games/Argosy. Grant it with:"
     echo "  flatpak override --user --filesystem=~/Games/Argosy org.libretro.RetroArch"
     exit 2 ;;
esac
flatpak run io.github.stealthx3.ArgosyLaunchSpike "$@"
