#!/usr/bin/env bash
set -euo pipefail
old_repo=${1:?usage: eol-id-repo.sh <old-repo> <new-repo> <out-dir> [gpg-key-id]}
new_repo=${2:?usage: eol-id-repo.sh <old-repo> <new-repo> <out-dir> [gpg-key-id]}
out=${3:?usage: eol-id-repo.sh <old-repo> <new-repo> <out-dir> [gpg-key-id]}
key=${4:-}
old_id=io.github.stealthx3.IceBreaker
new_id=io.github.stealthx3.icebreaker-client
url=https://stealthx3.github.io/IceBreaker/repo/
new_home=https://stealthx3.github.io/icebreaker-client/
reason="IceBreaker has a new app ID ($new_id). Accept the replacement to keep your settings and downloads. New home: $new_home"
bridge_reason="This copy of IceBreaker comes from the retired IceBreaker repository and gets no more updates. Switch to the new one (your data stays): flatpak remote-add --user --if-not-exists icebreaker-client ${new_home}icebreaker.flatpakrepo && flatpak install --user --reinstall icebreaker-client $new_id"
flatpak repo --branches "$old_repo" | grep -q "^app/$old_id/x86_64/master" || { echo "missing app/$old_id in $old_repo" >&2; exit 1; }
flatpak repo --branches "$new_repo" | grep -q "^app/$new_id/x86_64/master" || { echo "missing app/$new_id in $new_repo" >&2; exit 1; }

rm -rf "${out:?}/repo"
mkdir -p "$out/repo/objects" "$out/repo/refs/heads" "$out/repo/refs/remotes" "$out/repo/state" "$out/repo/tmp" "$out/repo/extensions"
printf '[core]\nrepo_version=1\nmode=archive-z2\n' > "$out/repo/config"

sign=()
if [ -n "$key" ]; then
  sign=(--gpg-sign="$key")
  [ -n "${GNUPGHOME:-}" ] && sign+=(--gpg-homedir="$GNUPGHOME")
fi
copy() {
  local src=$1 ref=$2; shift 2
  flatpak repo --branches "$src" | grep -q "^$ref" || return 0
  flatpak build-commit-from "${sign[@]}" --no-update-summary --src-repo="$src" "$@" "$out/repo" "$ref"
}
copy "$new_repo" "app/$new_id/x86_64/master" --end-of-life="$bridge_reason"
copy "$new_repo" "runtime/$new_id.Debug/x86_64/master" --end-of-life="$bridge_reason"
copy "$old_repo" "app/$old_id/x86_64/master" --end-of-life="$reason" --end-of-life-rebase="$old_id=$new_id"
copy "$old_repo" "runtime/$old_id.Debug/x86_64/master" --end-of-life="$reason" --end-of-life-rebase="$old_id.Debug=$new_id.Debug"
flatpak build-update-repo "${sign[@]}" --generate-static-deltas --prune --title="IceBreaker (retired ID)" "$out/repo"

gpg_line=""
[ -n "$key" ] && gpg_line="GPGKey=$(gpg ${GNUPGHOME:+--homedir "$GNUPGHOME"} --export "$key" | base64 -w0)"
homepage=https://github.com/Stealthx3/icebreaker-client
icon=https://raw.githubusercontent.com/Stealthx3/icebreaker-client/main/desktopApp/flatpak/icons/hicolor/scalable/apps/$new_id.svg
{
  echo "[Flatpak Repo]"
  echo "Title=IceBreaker"
  echo "Url=$url"
  echo "Homepage=$homepage"
  echo "Comment=Retired: IceBreaker moved to $new_home"
  echo "Description=Closed. IceBreaker now uses the app ID $new_id, served from $new_home"
  echo "Icon=$icon"
  [ -n "$gpg_line" ] && echo "$gpg_line"
} > "$out/icebreaker.flatpakrepo"
{
  echo "[Flatpak Ref]"
  echo "Title=IceBreaker"
  echo "Name=$old_id"
  echo "Branch=master"
  echo "Url=$url"
  echo "SuggestRemoteName=icebreaker"
  echo "Homepage=$homepage"
  echo "Icon=$icon"
  echo "IsRuntime=false"
  echo "RuntimeRepo=https://dl.flathub.org/repo/flathub.flatpakrepo"
  [ -n "$gpg_line" ] && echo "$gpg_line"
} > "$out/$old_id.flatpakref"
cat > "$out/index.html" <<HTML
<!doctype html><meta charset="utf-8"><title>IceBreaker moved</title>
<h1>IceBreaker moved to a new app ID</h1>
<p>Already installed? Run <code>flatpak update --user</code> and accept the switch to <code>$new_id</code>; your settings and downloads come along.</p>
<p>New installs: <a href="$new_home">$new_home</a>. Source: <a href="$homepage">Stealthx3/icebreaker-client</a>.</p>
HTML
flatpak repo --branches "$out/repo"
du -sh "$out/repo"
