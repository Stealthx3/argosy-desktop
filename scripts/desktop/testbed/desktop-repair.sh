#!/usr/bin/env bash
# Re-pairs the desktop with the testbed after a snapshot restore.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
OUT="${1:-$ROOT/build/desktop-check/repair}"
cd "$ROOT"

scripts/desktop/check/desktop-drive.sh "$OUT" 25 \
    PAD:START wait:2 PAD:DPAD_DOWN*8 wait:1 PAD:SOUTH wait:3 \
    PAD:DPAD_DOWN*13 wait:2 PAD:SOUTH wait:3 PAD:SOUTH wait:3 \
    PAD:DPAD_DOWN*2 wait:1 PAD:DPAD_RIGHT wait:2 PAD:DPAD_DOWN wait:1 PAD:SOUTH wait:2 \
    "RUN:build/desktop-testbed/venv/bin/python scripts/desktop/testbed/pair-code.py" \
    TYPEFILE:build/desktop-testbed/pair-code.txt wait:1 PAD:DPAD_DOWN wait:2 PAD:SOUTH wait:12 shot:paired
scripts/desktop/testbed/desktop-device.sh
