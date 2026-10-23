#!/usr/bin/env bash
# Builds the Argosy Desktop alpha Flatpak and installs it for the current user, beside the
# Gradle dev run (the Flatpak keeps its data under ~/.var/app/io.github.stealthx3.ArgosyDesktop).
# Steps: jlink distributable from Gradle, flatpak-builder into build/flatpak/repo (the repo the
# alpha is published from), user install, then flatpak-builder-lint on the manifest and repo.
#
# usage: scripts/desktop/flatpak/build-alpha.sh [--no-gradle]
set -euo pipefail
root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$root"
app_id=io.github.stealthx3.ArgosyDesktop
manifest=desktopApp/flatpak/$app_id.yml
out=build/flatpak
mkdir -p "$out"
if [ "${1:-}" != "--no-gradle" ]; then
  JAVA_HOME="$HOME/.jdks/jdk-21.0.12.1+1" ./gradlew --no-daemon --console=plain :desktopApp:createDistributable
fi
if command -v flatpak-builder >/dev/null 2>&1; then
  builder=(flatpak-builder)
else
  builder=(flatpak run --filesystem="$root" org.flatpak.Builder)
fi
"${builder[@]}" --user --install --force-clean --disable-rofiles-fuse \
  --state-dir="$out/.flatpak-builder" --repo="$out/repo" "$out/build-dir" "$manifest"
flatpak run --command=flatpak-builder-lint org.flatpak.Builder manifest "$manifest" || true
flatpak run --command=flatpak-builder-lint org.flatpak.Builder repo "$out/repo" || true
flatpak info --user "$app_id"
