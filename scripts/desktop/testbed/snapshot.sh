#!/usr/bin/env bash
# Saves and restores the whole save-sync test state under one name: the RomM testbed's Docker
# volumes (database, resources, redis, assets), the credentials file that matches them, and the
# emulator state of AVD argosy_handheld_check via its own snapshots. Restoring replaces clearing
# app data or reinstalling; nothing here deletes app data.
#
# usage: scripts/desktop/testbed/snapshot.sh save|restore <name>
#        scripts/desktop/testbed/snapshot.sh list
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
STATE="$ROOT/build/desktop-testbed"
SNAPS="$STATE/snapshots"
SERIAL="${SERIAL:-emulator-5556}"
AVD="${AVD:-argosy_handheld_check}"
SDK="$HOME/Android/Sdk"
ADB="$SDK/platform-tools/adb -s $SERIAL"
PROJECT=argosy-testbed
VOLUMES=(testbed_db_data v51_resources v51_redis v51_assets)
HELPER_IMAGE=mariadb:11.3.2
COMPOSE=(docker compose -p "$PROJECT" --env-file "$STATE/compose.env"
         -f "$ROOT/testbed/romm/docker-compose.base.yml" -f "$ROOT/testbed/romm/romm-5.1.yml")

cmd="${1:-}"
name="${2:-}"

ensure_emulator() {
    if ! $ADB get-state >/dev/null 2>&1; then
        ANDROID_SDK_ROOT="$SDK" nohup "$SDK/emulator/emulator" -avd "$AVD" -port "${SERIAL#emulator-}" \
            -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -no-snapshot-save \
            > "$STATE/emulator-$AVD.log" 2>&1 &
        $ADB wait-for-device
    fi
    until [ "$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 2; done
}

wait_romm() {
    for _ in $(seq 60); do
        curl -sf http://localhost:8093/api/heartbeat >/dev/null && return 0
        sleep 3
    done
    echo "RomM did not come back" >&2
    return 1
}

save() {
    local dir="$SNAPS/$name"
    [[ -e "$dir" ]] && { echo "snapshot $name exists; pick another name or delete $dir" >&2; exit 1; }
    mkdir -p "$dir"
    ensure_emulator
    $ADB emu avd snapshot save "$name" | tr -d '\r' | grep -q OK
    "${COMPOSE[@]}" stop
    for v in "${VOLUMES[@]}"; do
        docker run --rm -v "${PROJECT}_$v:/v:ro" -v "$dir:/b" "$HELPER_IMAGE" \
            tar -C /v -cf "/b/$v.tar" .
    done
    "${COMPOSE[@]}" start
    cp "$STATE/credentials.env" "$dir/credentials.env"
    date -u +%FT%TZ > "$dir/created"
    wait_romm
    echo "saved snapshot $name (server volumes + emulator $SERIAL)"
}

restore() {
    local dir="$SNAPS/$name"
    [[ -d "$dir" ]] || { echo "no snapshot $name" >&2; exit 1; }
    "${COMPOSE[@]}" stop
    for v in "${VOLUMES[@]}"; do
        docker run --rm -v "${PROJECT}_$v:/v" -v "$dir:/b:ro" "$HELPER_IMAGE" \
            sh -c "find /v -mindepth 1 -delete && tar -C /v -xf /b/$v.tar"
    done
    "${COMPOSE[@]}" start
    install -m 600 "$dir/credentials.env" "$STATE/credentials.env"
    ensure_emulator
    $ADB emu avd snapshot load "$name" | tr -d '\r' | grep -q OK
    until [ "$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = 1 ]; do sleep 2; done
    wait_romm
    echo "restored snapshot $name"
}

list() {
    echo "server snapshots ($SNAPS):"
    ls -1 "$SNAPS" 2>/dev/null | sed 's/^/  /'
    if $ADB get-state >/dev/null 2>&1; then
        echo "emulator snapshots ($SERIAL):"
        $ADB emu avd snapshot list | tr -d '\r' | sed 's/^/  /'
    fi
}

case "$cmd" in
    save) [[ -n "$name" ]] || exit 2; save ;;
    restore) [[ -n "$name" ]] || exit 2; restore ;;
    list) list ;;
    *) sed -n 2,9p "$0"; exit 2 ;;
esac
