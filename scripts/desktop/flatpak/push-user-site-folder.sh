#!/usr/bin/env bash
set -euo pipefail
folder=${1:?usage: push-user-site-folder.sh <folder> <src-dir> <label>}
src=${2:?usage: push-user-site-folder.sh <folder> <src-dir> <label>}
label=${3:?usage: push-user-site-folder.sh <folder> <src-dir> <label>}
case "$folder" in argosy-desktop|""|.|..|*/*) echo "refused: $folder is protected or not a plain folder name" >&2; exit 2 ;; esac
root="$(cd "$(dirname "$0")/../../.." && pwd)"
src=$(cd "$src" && pwd)
ssh_cmd=$(git -C "$root" config --get core.sshCommand || true)
site=$root/build/user-site-clone
rm -rf "$site"
GIT_SSH_COMMAND="${ssh_cmd:-ssh}" git clone -q --depth 1 git@github.com:Stealthx3/stealthx3.github.io.git "$site"
cd "$site"
[ -d argosy-desktop ] || { echo "argosy-desktop/ missing from the user site; stopping" >&2; exit 1; }
rm -rf "$folder"
mkdir -p "$folder"
cp -a "$src/repo" "$folder/repo"
rm -rf "$folder/repo/tmp" "$folder/repo/.lock"
find "$src" -maxdepth 1 -type f \( -name '*.flatpakrepo' -o -name '*.flatpakref' -o -name 'index.html' \) -exec cp {} "$folder/" \;
git add -A "$folder"
git -c user.name="$(git -C "$root" config user.name)" -c user.email="$(git -C "$root" config user.email)" \
  commit -q -m "$folder: $label ($(date -u +%Y-%m-%dT%H:%MZ))"
GIT_SSH_COMMAND="${ssh_cmd:-ssh}" git push -q origin HEAD:main
echo "pushed $folder/ to the user site: $(git rev-parse --short HEAD), folder $(du -sh "$folder" | cut -f1), argosy-desktop/ untouched: $(git diff --quiet HEAD~1 HEAD -- argosy-desktop && echo yes || echo NO)"
