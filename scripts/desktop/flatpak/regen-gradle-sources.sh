#!/usr/bin/env bash
# Regenerates desktopApp/flatpak/generated/gradle-sources.json for the Flathub manifest: copies the
# tracked files to a scratch folder, runs the desktop-only build there with an empty Gradle home and
# an empty ANDROID_HOME (logging every download), then turns the downloads into flatpak sources.
# Rerun after any dependency or plugin change. Downloads about 1.2 GB.
# usage: scripts/desktop/flatpak/regen-gradle-sources.sh <scratch-dir>
set -euo pipefail
root="$(cd "$(dirname "$0")/../../.." && pwd)"
work="$1"; copy="$work/src"
rm -rf "$work"; mkdir -p "$copy" "$work/empty-sdk"
cd "$root" && git ls-files --recurse-submodules -z | rsync -a --from0 --files-from=- "$root/" "$copy/"
cd "$copy"
set +o pipefail
env -u ANDROID_SDK_ROOT ANDROID_HOME="$work/empty-sdk" GRADLE_USER_HOME="$work/gradle-home" \
  JAVA_HOME="$HOME/.jdks/jdk-21.0.12.1+1" ./gradlew --no-daemon --no-build-cache --console=plain --debug \
  -Pargosy.desktopOnly=true :desktopApp:createDistributable 2>&1 \
  | grep -E "Performing HTTP GET|Resource missing|BUILD (SUCCESSFUL|FAILED)" > "$work/capture.log"
set -o pipefail
grep -q "BUILD SUCCESSFUL" "$work/capture.log" || { echo "capture build failed"; exit 1; }
python3 -I "$root/scripts/desktop/flatpak/gradle-sources.py" "$work/capture.log" "$work/gradle-home" \
  "$root/desktopApp/flatpak/generated/gradle-sources.json"
