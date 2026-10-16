#!/usr/bin/env bash
# Copies (never moves) upstream's Room database sources into this spike.
# Re-run after pulling upstream to re-test the same code.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
src="$here/../../app/src/main/kotlin/com/nendo/argosy"
dst="$here/src/main/kotlin/com/nendo/argosy"
rm -rf "$dst"
mkdir -p "$dst/data/local" "$dst/data/model" "$dst/data/platform" "$dst/util"
# Entities, DAOs, Converters, ALauncherDatabase. Excluded: migrations/ (built on
# SupportSQLiteDatabase, not needed by a fresh desktop install) and
# DatabaseFactory.kt (Android Context + Room.databaseBuilder(context, ...)).
(cd "$src/data/local" && find . -name '*.kt' -not -path './migrations/*' -not -name DatabaseFactory.kt \
  -exec install -D -m 644 {} "$dst/data/local/{}" \;)
# Out-of-package types the database code imports (all plain Kotlin/JVM).
for f in data/model/ArtSlot.kt data/model/FileOrigin.kt data/model/GameSource.kt \
         data/model/ResolvedGameArt.kt data/platform/PlatformDefinitions.kt util/SearchNormalizer.kt; do
  install -m 644 "$src/$f" "$dst/$f"
done
echo "copied $(find "$dst" -name '*.kt' | wc -l) files"
