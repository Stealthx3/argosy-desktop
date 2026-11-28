#!/usr/bin/env bash
# End-of-life Argosy Desktop Pages repo in <out>/repo: IceBreaker plus ArgosyDesktop rebased to it, current commits only.
# usage: eol-old-repo.sh <out-dir> [gpg-key-id]  (GNUPGHOME: ~/.local/share/argosy-desktop-signing/gnupg)
set -euo pipefail
out=${1:?usage: eol-old-repo.sh <out-dir> [gpg-key-id]}
key=${2:-}
root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$root"
src=build/flatpak/repo
old_id=io.github.stealthx3.ArgosyDesktop
new_id=io.github.stealthx3.icebreaker-client
url=https://stealthx3.github.io/argosy-desktop/repo/
reason="Argosy Desktop is now IceBreaker ($new_id). Accept the replacement to keep your settings and downloads. New home: https://stealthx3.github.io/icesuite-test/"
bridge_reason="This copy of IceBreaker comes from the closed Argosy Desktop repository and gets no more updates. Switch to the IceSuite test repository (your data stays): flatpak remote-add --user --if-not-exists icesuite-test https://stealthx3.github.io/icesuite-test/flatpak/icesuite-test.flatpakrepo && flatpak install --user --reinstall icesuite-test $new_id"
[ -f "$src/config" ] || { echo "no repo at $src: run build-alpha.sh first" >&2; exit 1; }
for ref in "app/$new_id/x86_64/master" "app/$old_id/x86_64/master"; do
  flatpak repo --branches "$src" | grep -q "^$ref" || { echo "missing $ref in $src" >&2; exit 1; }
done

rm -rf "${out:?}/repo"
mkdir -p "$out/repo/objects" "$out/repo/refs/heads" "$out/repo/refs/remotes" "$out/repo/state" "$out/repo/tmp" "$out/repo/extensions"
printf '[core]\nrepo_version=1\nmode=archive-z2\n' > "$out/repo/config"

sign=()
if [ -n "$key" ]; then
  sign=(--gpg-sign="$key")
  [ -n "${GNUPGHOME:-}" ] && sign+=(--gpg-homedir="$GNUPGHOME")
fi
copy() {
  local ref=$1; shift
  flatpak build-commit-from "${sign[@]}" --no-update-summary --src-repo="$src" "$@" "$out/repo" "$ref"
}
copy "app/$new_id/x86_64/master" --end-of-life="$bridge_reason"
copy "runtime/$new_id.Debug/x86_64/master" --end-of-life="$bridge_reason"
copy "app/$old_id/x86_64/master" --end-of-life="$reason" --end-of-life-rebase="$old_id=$new_id"
copy "runtime/$old_id.Debug/x86_64/master" --end-of-life="$reason" --end-of-life-rebase="$old_id=$new_id"
flatpak build-update-repo "${sign[@]}" --generate-static-deltas --prune --title="Argosy Desktop" "$out/repo"

gpg_line=""
[ -n "$key" ] && gpg_line="GPGKey=$(gpg ${GNUPGHOME:+--homedir "$GNUPGHOME"} --export "$key" | base64 -w0)"
homepage=https://github.com/Stealthx3/icebreaker-client
icon=https://raw.githubusercontent.com/Stealthx3/icebreaker-client/main/desktopApp/flatpak/icons/hicolor/scalable/apps/io.github.stealthx3.icebreaker-client.svg
{
  echo "[Flatpak Repo]"
  echo "Title=Argosy Desktop"
  echo "Url=$url"
  echo "Homepage=$homepage"
  echo "Comment=Argosy Desktop beta builds (closed: now IceBreaker)"
  echo "Description=Closed. Argosy Desktop is now IceBreaker, served from https://stealthx3.github.io/icesuite-test/"
  echo "Icon=$icon"
  [ -n "$gpg_line" ] && echo "$gpg_line"
} > "$out/argosy-desktop.flatpakrepo"
{
  echo "[Flatpak Ref]"
  echo "Title=Argosy Desktop"
  echo "Name=$old_id"
  echo "Branch=master"
  echo "Url=$url"
  echo "SuggestRemoteName=argosy-desktop"
  echo "Homepage=$homepage"
  echo "Icon=$icon"
  echo "IsRuntime=false"
  echo "RuntimeRepo=https://dl.flathub.org/repo/flathub.flatpakrepo"
  [ -n "$gpg_line" ] && echo "$gpg_line"
} > "$out/$old_id.flatpakref"
flatpak repo --branches "$out/repo"
du -sh "$out/repo"
