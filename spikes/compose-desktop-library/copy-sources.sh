#!/usr/bin/env bash
# Copies (never moves) the upstream files this spike compiles unchanged. The list is the measured
# dependency closure of LibraryGameList; everything outside it is stubbed in src/main/kotlin.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
src="$here/../../app/src/main/kotlin"
dst="$here/src/upstream/kotlin"
rm -rf "$dst"; mkdir -p "$dst"
while read -r f; do
  [[ -z "$f" || "$f" == \#* ]] && continue
  install -D -m 644 "$src/$f" "$dst/$f"
done < "$here/upstream-files.txt"
echo "copied $(find "$dst" -name '*.kt' | wc -l) files"
