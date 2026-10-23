#!/usr/bin/env bash
# usage: scripts/desktop/flatpak/push-pages.sh  (after publish-repo.sh <pages-url>/repo/ <key>; single-commit gh-pages)
set -euo pipefail
root="$(cd "$(dirname "$0")/../../.." && pwd)"
cd "$root"
app_id=io.github.stealthx3.ArgosyDesktop
src=build/flatpak
site=build/flatpak-pages
remote=$(git remote get-url origin)
ssh_cmd=$(git config --get core.sshCommand || true)
grep -q '^Url=https://stealthx3.github.io/argosy-desktop/repo/$' "$src/$app_id.flatpakref" \
  || { echo "flatpakref Url is not the Pages URL: rerun publish-repo.sh" >&2; exit 1; }
rm -rf "$site"
mkdir -p "$site"
cp -a "$src/repo" "$site/repo"
rm -rf "$site/repo/tmp" "$site/repo/.lock"
cp "$src/argosy-desktop.flatpakrepo" "$src/$app_id.flatpakref" "$site/"
touch "$site/.nojekyll"
cat > "$site/index.html" <<HTML
<!doctype html><meta charset="utf-8"><title>Argosy Desktop Flatpak repo</title>
<h1>Argosy Desktop (alpha)</h1>
<p><code>flatpak install --user --from https://stealthx3.github.io/argosy-desktop/$app_id.flatpakref</code></p>
<p>Or add the repo: <a href="argosy-desktop.flatpakrepo">argosy-desktop.flatpakrepo</a>.
Source: <a href="https://github.com/Stealthx3/argosy-desktop">Stealthx3/argosy-desktop</a>.</p>
HTML
cd "$site"
git init -q -b gh-pages
git add -A
git -c user.name="$(git -C "$root" config user.name)" -c user.email="$(git -C "$root" config user.email)" \
  commit -q -m "Argosy Desktop Flatpak repo ($(date -u +%Y-%m-%dT%H:%MZ))"
GIT_SSH_COMMAND="${ssh_cmd:-ssh}" git push -q --force "$remote" gh-pages
echo "pushed gh-pages: $(git rev-parse --short HEAD), $(du -sh . | cut -f1)"
