#!/usr/bin/env bash
# usage: scripts/desktop/flatpak/publish-repo.sh <repo-url> [gpg-key-id]  (unsigned without a key)
set -euo pipefail
url=${1:?usage: publish-repo.sh <repo-url> [gpg-key-id]}
key=${2:-}
case "$url" in */) ;; *) url="$url/" ;; esac
root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$root"
app_id=io.github.stealthx3.ArgosyDesktop
out=build/flatpak
repo=$out/repo
[ -f "$repo/config" ] || { echo "no repo at $repo: run build-alpha.sh first" >&2; exit 1; }
sign=()
gpg_line=""
if [ -n "$key" ]; then
  sign=(--gpg-sign="$key")
  flatpak build-sign "${sign[@]}" "$repo" "$app_id" master
  gpg_line="GPGKey=$(gpg --export "$key" | base64 -w0)"
fi
flatpak build-update-repo "${sign[@]}" --generate-static-deltas --prune \
  --title="Argosy Desktop" "$repo"
homepage=https://github.com/Stealthx3/argosy-desktop
icon=https://raw.githubusercontent.com/Stealthx3/argosy-desktop/main/argosy_icon.svg
{
  echo "[Flatpak Repo]"
  echo "Title=Argosy Desktop"
  echo "Url=$url"
  echo "Homepage=$homepage"
  echo "Comment=Argosy Desktop alpha builds"
  echo "Description=Flatpak repository of Argosy Desktop, the Linux port of the Argosy launcher"
  echo "Icon=$icon"
  [ -n "$gpg_line" ] && echo "$gpg_line"
} > "$out/argosy-desktop.flatpakrepo"
{
  echo "[Flatpak Ref]"
  echo "Title=Argosy Desktop"
  echo "Name=$app_id"
  echo "Branch=master"
  echo "Url=$url"
  echo "SuggestRemoteName=argosy-desktop"
  echo "Homepage=$homepage"
  echo "Icon=$icon"
  echo "IsRuntime=false"
  echo "RuntimeRepo=https://dl.flathub.org/repo/flathub.flatpakrepo"
  [ -n "$gpg_line" ] && echo "$gpg_line"
} > "$out/$app_id.flatpakref"
flatpak repo --branches "$repo"
echo "wrote $out/argosy-desktop.flatpakrepo and $out/$app_id.flatpakref (Url=$url)"
