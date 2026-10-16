#!/bin/sh
# Builds and runs the launch spike on the host. Needs flatpak-builder,
# the Freedesktop 24.08 runtime/SDK, and the RetroArch Flatpak with a core.
set -eu
cd "$(dirname "$0")"
flatpak-builder --user --install --force-clean build-dir io.github.stealthx3.ArgosyLaunchSpike.yml
# RetroArch's own sandbox must be able to read the shared ROM folder:
flatpak override --user --filesystem=~/Games/Argosy org.libretro.RetroArch
flatpak run io.github.stealthx3.ArgosyLaunchSpike "$@"
