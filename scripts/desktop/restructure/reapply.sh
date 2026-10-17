#!/usr/bin/env bash
# Re-applies the restructure after merging an upstream release into main:
#   git merge <release-tag>          (then run this from the repository root)
# Conflicts in step 1's extraction sources resolve to upstream's version and are re-extracted;
# moves and import rewrites are re-run; the audit reports what is left. Any other conflict is
# printed and left for a person.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
HERE=scripts/desktop/restructure

sources=$(grep -vE '^\s*(#|$)' "$HERE/extract-decls.txt" | sed -E 's/ *->.*//' | sort -u)
for src in $sources; do
    path="app/src/main/kotlin/com/nendo/argosy/$src"
    if git diff --name-only --diff-filter=U | grep -qx "$path"; then
        git checkout --theirs -- "$path"
        git add "$path"
        echo "conflict in $src: took upstream's version, re-extracting"
    fi
done

python3 -I "$HERE/extract_decls.py" --refresh
python3 -I "$HERE/inline_constants.py"
python3 -I "$HERE/apply_moves.py"
python3 -I "$HERE/move_members.py"
python3 -I "$HERE/actualize.py"
python3 -I "$HERE/rewrite_imports.py"
git add -A app/src/main/kotlin shared/src
python3 -I "$HERE/movable.py" --check || true

left=$(git diff --name-only --diff-filter=U)
if [ -n "$left" ]; then
    echo "conflicts left for a person:"
    echo "$left"
    exit 1
fi
echo "restructure re-applied; build and run the checks before committing the merge"
