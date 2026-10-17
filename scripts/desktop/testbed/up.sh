#!/usr/bin/env bash
# Brings the disposable RomM 5.1 testbed up and makes it usable. Safe to re-run:
# every step checks before it acts (secrets, library, venv, containers, admin, scan, token).
#
# usage: scripts/desktop/testbed/up.sh [--rescan]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
STATE="$ROOT/build/desktop-testbed"
ENV_FILE="$STATE/compose.env"
CREDS="$STATE/credentials.env"
BASE_URL="http://localhost:8093"

mkdir -p "$STATE"
umask 077

if [[ ! -f "$ENV_FILE" ]]; then
    cat > "$ENV_FILE" <<ENV
TESTBED_LIBRARY=$STATE/library
DB_USER=romm-user
DB_PASSWORD=$(openssl rand -hex 16)
DB_ROOT_PASSWORD=$(openssl rand -hex 16)
DB_PORT=3399
ROMM_AUTH_SECRET_KEY=$(openssl rand -hex 32)
ENV
    echo "wrote $ENV_FILE"
fi

umask 022
python3 -I "$ROOT/scripts/desktop/testbed/make-library.py" "$STATE/library" >/dev/null

if [[ ! -x "$STATE/venv/bin/python" ]] || ! "$STATE/venv/bin/python" -c "import socketio, requests" 2>/dev/null; then
    python3 -m venv "$STATE/venv"
    "$STATE/venv/bin/pip" -q install "python-socketio[client]" requests
fi

docker compose -p argosy-testbed --env-file "$ENV_FILE" \
    -f "$ROOT/testbed/romm/docker-compose.base.yml" -f "$ROOT/testbed/romm/romm-5.1.yml" up -d

for _ in $(seq 60); do
    curl -sf "$BASE_URL/api/heartbeat" >/dev/null && break
    sleep 3
done
curl -sf "$BASE_URL/api/heartbeat" >/dev/null || { echo "RomM did not answer at $BASE_URL" >&2; exit 1; }

"$STATE/venv/bin/python" -I "$ROOT/scripts/desktop/testbed/romm-setup.py" "$BASE_URL" "$CREDS" "$STATE/library" "$@"
"$STATE/venv/bin/python" -I "$ROOT/scripts/desktop/testbed/seed-saves.py" "$BASE_URL" "$CREDS"
echo "RomM 5.1 testbed ready at $BASE_URL (emulator: http://10.0.2.2:8093); credentials in $CREDS"
