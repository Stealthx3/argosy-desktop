#!/usr/bin/env python3
"""
Dumps the device's save tables, or a RomM GET /api/saves response, as sorted JSON for diffing.

  save_tables.py db <alauncher.db> [--normalize] [--part saves|states]
  save_tables.py api <saves.json> [--normalize]
  save_tables.py datastore <datastore.json> --normalize

--normalize masks what legitimately differs between two runs from the same snapshot: clock values
(columns or keys ending in At/_at, epoch-millisecond columns and DataStore keys), the timestamps RomM puts in stored
file names, and the dated folder names of the device's save cache. Hashes, sizes, slots, channels,
ids and flags are kept, so equal normalized output means equal save state.

--part splits the device tables: "saves" is the save tables plus every pending_sync_queue row except
SAVE_STATE; "states" is the state tables plus the SAVE_STATE queue rows. Emulator save states hold
the whole machine, so their bytes depend on how long the game ran, and their upload races the RomM
reconnect after a game closes; they are evidence, not part of the save comparison.
"""
import json
import re
import sqlite3
import sys

TABLES = ["save_sync", "save_cache", "save_channels", "save_ownership", "pending_sync_queue",
          "state_cache", "state_ownership", "state_tombstones", "sigil_sync_state"]
STATE_TABLES = {"state_cache", "state_ownership", "state_tombstones"}
CLOCK_KEY = re.compile(r"(At|_at)$")
EPOCH_MS_KEYS = {"emulator_update_last_check", "last_integrity_check_time"}
CLOCK_COLUMNS = {"cachedAt", "localUpdatedAt", "serverUpdatedAt", "lastSyncedAt", "createdAt",
                 "updatedAt", "userSelectedRestorePointAt", "corruptZipTimestamp"}
STAMP = [
    (re.compile(r"\[\d{4}-\d{2}-\d{2}_\d{2}-\d{2}-\d{2}\]"), "[<stamp>]"),
    (re.compile(r"/\d{8}_\d{6}/"), "/<stamp>/"),
    (re.compile(r"save_precache_\d+_\d+\.tmp"), "save_precache_<n>.tmp"),
    (re.compile(r"timestamp=[^&\"]+"), "timestamp=<stamp>"),
    (re.compile(r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[+-]\d{2}:\d{2})?$"), "<clock>"),
]


def mask(value):
    if isinstance(value, str):
        for pattern, repl in STAMP:
            value = pattern.sub(repl, value)
    return value


def normalize_row(row):
    out = {}
    for key, value in row.items():
        if key in CLOCK_COLUMNS or key in EPOCH_MS_KEYS or CLOCK_KEY.search(key):
            out[key] = None if value is None else "<clock>"
        elif isinstance(value, dict):
            out[key] = normalize_row(value)
        elif isinstance(value, list):
            out[key] = [normalize_row(v) if isinstance(v, dict) else mask(v) for v in value]
        else:
            out[key] = mask(value)
    return out


def from_db(path):
    conn = sqlite3.connect(path)
    conn.row_factory = sqlite3.Row
    present = {r[0] for r in conn.execute("SELECT name FROM sqlite_master WHERE type='table'")}
    return {t: [dict(r) for r in conn.execute(f'SELECT * FROM "{t}" ORDER BY 1')] for t in TABLES if t in present}


def split(data, part):
    out = {}
    for table, rows in data.items():
        if table == "pending_sync_queue":
            out[table] = [r for r in rows if (r.get("syncType") == "SAVE_STATE") == (part == "states")]
        elif (table in STATE_TABLES) == (part == "states"):
            out[table] = rows
    return out


def main():
    kind, path = sys.argv[1], sys.argv[2]
    normalize = "--normalize" in sys.argv
    if kind == "db":
        data = from_db(path)
        if normalize:
            data = {t: [normalize_row(r) for r in rows] for t, rows in data.items()}
        if "--part" in sys.argv:
            data = split(data, sys.argv[sys.argv.index("--part") + 1])
    elif kind == "datastore":
        data = json.load(open(path))
        if normalize:
            data = normalize_row(data)
    else:
        data = sorted(json.load(open(path)), key=lambda s: s["id"])
        if normalize:
            data = [normalize_row(s) for s in data]
    print(json.dumps(data, indent=1, sort_keys=True, default=str))


main()
