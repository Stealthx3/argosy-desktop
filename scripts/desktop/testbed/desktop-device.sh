#!/usr/bin/env bash
# Restores the desktop's RomM device row, which a testbed snapshot restore drops.
set -euo pipefail

DATASTORE="$HOME/.local/share/argosy-desktop/datastore"
DB_NAME="$(docker inspect romm-v51 --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^DB_NAME=//p')"

ID="$(strings "$DATASTORE"/*.preferences_pb | grep -A1 -x romm_device_id | tail -1 \
    | grep -oE '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}')"
VERSION="$(strings "$DATASTORE"/*.preferences_pb | grep -A1 -x romm_device_client_version | tail -1 \
    | grep -oE '[0-9]+(\.[0-9]+)+[A-Za-z0-9.-]*' || true)"
[[ -n "$ID" ]] || { echo "no romm_device_id in the desktop datastore" >&2; exit 1; }

docker exec -i romm-testbed-db sh -c "mariadb -uroot -p\"\$MARIADB_ROOT_PASSWORD\" -N $DB_NAME" <<SQL
INSERT IGNORE INTO devices (id, user_id, name, platform, client, client_version, sync_mode, sync_enabled)
VALUES ('$ID', 1, 'Argosy Desktop (testbed)', 'linux', 'argosy', '${VERSION:-unknown}', 'API', 1);
SELECT CONCAT('device ', id, ' present, client_version ', client_version) FROM devices WHERE id = '$ID';
SQL
