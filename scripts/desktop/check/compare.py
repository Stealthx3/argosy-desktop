#!/usr/bin/env python3
"""
Compares baseline-* and candidate-* screenshots and database summaries in a check folder.

Masks the header clock (with the blur halo it casts under the frosted side menu) and the side
menu's own clock. A database time column or dated path, and a DataStore timestamp that moved forward, are reported,
not failed: both runs
write such values as they sync. Exits non-zero on any other difference.
"""
import json
import re
import sqlite3
import sys
from datetime import datetime
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

MASKS = [(1440, 0, 1920, 185), (0, 940, 500, 1010)]


def load(path):
    image = Image.open(path).convert("RGB")
    draw = ImageDraw.Draw(image)
    for box in MASKS:
        draw.rectangle(box, fill=(0, 0, 0))
    return image


def timestamp(value):
    try:
        return datetime.fromisoformat(str(value).replace("Z", "+00:00"))
    except ValueError:
        return None


EPOCH_MS_KEYS = {"emulator_update_last_check", "last_integrity_check_time"}


def advanced(before, after, key=None):
    if key in EPOCH_MS_KEYS and isinstance(before, int) and isinstance(after, int):
        return after >= before
    b, a = timestamp(before), timestamp(after)
    return b is not None and a is not None and a >= b


TIME_COLUMN = re.compile(r"(At|Time|time|Timestamp|timestamp)$")
DATED = re.compile(r"\d{8}_\d{6}|save_precache_\d+_\d+")


def database_differences(out):
    base = sqlite3.connect(out / "baseline-db" / "alauncher.db")
    cand = sqlite3.connect(out / "candidate-db" / "alauncher.db")
    real, timing = [], []
    tables = [r[0] for r in base.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")]
    for table in tables:
        cols = [r[1] for r in base.execute(f'PRAGMA table_info("{table}")')]
        rows_b = base.execute(f'SELECT * FROM "{table}" ORDER BY 1').fetchall()
        rows_c = cand.execute(f'SELECT * FROM "{table}" ORDER BY 1').fetchall()
        if len(rows_b) != len(rows_c):
            real.append(f"{table}: {len(rows_b)} vs {len(rows_c)} rows")
            continue
        for rb, rc in zip(rows_b, rows_c):
            for col, vb, vc in zip(cols, rb, rc):
                if vb == vc:
                    continue
                same_shape = isinstance(vb, str) and isinstance(vc, str) and DATED.sub("#", vb) == DATED.sub("#", vc)
                (timing if TIME_COLUMN.search(col) or same_shape else real).append(f"{table}.{col}")
    return sorted(set(real)), sorted(set(timing))


def main():
    out, screens = Path(sys.argv[1]), sys.argv[2:]
    failed = False
    for screen in screens:
        box = ImageChops.difference(load(out / f"baseline-{screen}.png"), load(out / f"candidate-{screen}.png")).getbbox()
        print(f"{screen}: {'identical' if box is None else f'DIFFERS in {box}'}")
        failed |= box is not None
    cand = json.loads((out / "candidate-db.json").read_text())
    real, timing = database_differences(out)
    same = not real
    print(f"database: {'identical' if same else 'DIFFERS in ' + ', '.join(real)} (version {cand['user_version']}, {sum(cand['tables'].values())} rows)")
    if timing:
        print(f"database times and dated paths written again by the candidate run: {', '.join(timing)}")
    base_ds = json.loads((out / "baseline-datastore.json").read_text())
    cand_ds = json.loads((out / "candidate-datastore.json").read_text())
    ds_diff, ds_moved = [], []
    for name in sorted(set(base_ds) | set(cand_ds)):
        b, c = base_ds.get(name, {}), cand_ds.get(name, {})
        for k in sorted(set(b) | set(c)):
            if b.get(k) == c.get(k):
                continue
            (ds_moved if advanced(b.get(k), c.get(k), k) else ds_diff).append(f"{name}:{k}")
    keys = sum(len(v) for v in cand_ds.values())
    print(f"datastore: {'identical' if not ds_diff else 'DIFFERS in ' + ', '.join(ds_diff)} ({len(cand_ds)} files, {keys} keys)")
    if ds_moved:
        print(f"datastore timestamps written again by the candidate run: {', '.join(ds_moved)}")
    sys.exit(1 if failed or not same or ds_diff else 0)


if __name__ == "__main__":
    main()
