#!/usr/bin/env bash
# usage: scripts/desktop/flatpak/push-pages.sh --release <version> | --phase-close <n> | --step-close <phase>.<step> | --end-of-life
#   IceBreaker's own Pages repo (this repo's gh-pages, https://stealthx3.github.io/icebreaker-client/), from publish-repo.sh output
#   --end-of-life: eol-old-repo.sh output to the user site Stealthx3/stealthx3.github.io, folder argosy-desktop/
set -euo pipefail
case "${1:-}" in
  --release) [[ "${2:-}" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] && label="release ${2}" ;;
  --phase-close) [[ "${2:-}" =~ ^[0-9]+$ ]] && label="phase ${2} close" ;;
  --step-close) [[ "${2:-}" =~ ^[0-9]+\.[0-9]+$ ]] && label="step ${2} close" ;;
  --end-of-life) label="end of life: moved to IceBreaker"; src=build/flatpak/eol ;;
esac
[ -n "${label:-}" ] \
  || { echo "refused: the Pages Flatpak repo is updated only for a release or at a closed step or phase: push-pages.sh --release <version> | --step-close <phase>.<step> | --phase-close <n>" >&2; exit 2; }
root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$root"
src=${src:-build/flatpak}
ssh_cmd=$(git config --get core.sshCommand || true)
if [ "$1" = --end-of-life ]; then
  site=build/user-site
  dir=$site/argosy-desktop
  remote=git@github.com:Stealthx3/stealthx3.github.io.git
  branch=main
  app_id=io.github.stealthx3.ArgosyDesktop
  repo_file=argosy-desktop.flatpakrepo
  pages_url=https://stealthx3.github.io/argosy-desktop/
else
  site=build/flatpak-pages
  dir=$site
  remote=$(git remote get-url origin)
  branch=gh-pages
  app_id=io.github.stealthx3.icebreaker-client
  repo_file=icebreaker.flatpakrepo
  pages_url=https://stealthx3.github.io/icebreaker-client/
fi
grep -qx "Url=${pages_url}repo/" "$src/$app_id.flatpakref" \
  || { echo "flatpakref Url is not the Pages URL: rerun publish-repo.sh" >&2; exit 1; }
rm -rf "$site"
mkdir -p "$dir"
cp -a "$src/repo" "$dir/repo"
rm -rf "$dir/repo/tmp" "$dir/repo/.lock"
cp "$src/$repo_file" "$src/$app_id.flatpakref" "$dir/"
touch "$site/.nojekyll"
if [ "$1" = --end-of-life ]; then
  cat > "$dir/index.html" <<HTML
<!doctype html><meta charset="utf-8"><title>Argosy Desktop is now IceBreaker</title>
<h1>Argosy Desktop is now IceBreaker</h1>
<p>This repository is closed. Already installed? Run <code>flatpak update --user</code> and accept the switch to <code>io.github.stealthx3.icebreaker-client</code>; your settings and downloads come along.</p>
<p>New installs: <a href="https://stealthx3.github.io/icesuite-test/">IceSuite test repository</a>. Source: <a href="https://github.com/Stealthx3/icebreaker-client">Stealthx3/icebreaker-client</a>.</p>
HTML
  cat > "$site/index.html" <<HTML
<!doctype html><meta charset="utf-8"><title>Stealthx3</title>
<h1>Stealthx3</h1>
<p><a href="https://stealthx3.github.io/icesuite-test/">IceSuite test repository</a> ·
<a href="https://github.com/Stealthx3/icebreaker-client">IceBreaker</a> ·
<a href="argosy-desktop/">Argosy Desktop (closed, now IceBreaker)</a></p>
HTML
else
  cat > "$site/index.html" <<HTML
<!doctype html><meta charset="utf-8"><title>IceBreaker Flatpak repo</title>
<h1>IceBreaker</h1>
<p>The Linux port of the Argosy launcher for RomM.</p>
<p><code>flatpak install --user --from ${pages_url}$app_id.flatpakref</code></p>
<p>Or add the repo: <a href="$repo_file">$repo_file</a>.
Source and release notes: <a href="https://github.com/Stealthx3/icebreaker-client">Stealthx3/icebreaker-client</a>.</p>
HTML
fi
cd "$site"
git init -q -b "$branch"
git add -A
git -c user.name="$(git -C "$root" config user.name)" -c user.email="$(git -C "$root" config user.email)" \
  commit -q -m "Flatpak repo, $label ($(date -u +%Y-%m-%dT%H:%MZ))"
GIT_SSH_COMMAND="${ssh_cmd:-ssh}" git push -q --force "$remote" "$branch"
echo "pushed $branch to $remote: $(git rev-parse --short HEAD), $(du -sh . | cut -f1)"
