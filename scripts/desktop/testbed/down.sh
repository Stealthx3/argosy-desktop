#!/usr/bin/env bash
# Stops the RomM testbed. --purge also deletes its database, resources and assets volumes
# (the library and credentials under build/desktop-testbed/ are kept; delete that folder by hand).
#
# usage: scripts/desktop/testbed/down.sh [--purge]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
args=(down)
[[ "${1:-}" == "--purge" ]] && args+=(-v)
docker compose -p argosy-testbed --env-file "$ROOT/build/desktop-testbed/compose.env" \
    -f "$ROOT/testbed/romm/docker-compose.base.yml" -f "$ROOT/testbed/romm/romm-5.1.yml" "${args[@]}"
if [[ "${1:-}" == "--purge" ]]; then
    sed -i '/^ROMM_CLIENT_TOKEN=/d' "$ROOT/build/desktop-testbed/credentials.env" 2>/dev/null || true
fi
