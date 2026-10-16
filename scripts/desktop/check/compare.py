#!/usr/bin/env python3
"""
Compares baseline-* and candidate-* screenshots and database summaries in a check folder.

Masks the header clock (with the blur halo it casts under the frosted side menu) and the side
menu's own clock. A DataStore value that is a timestamp and moved forward is reported, not failed: both runs
write such values as they sync. Exits non-zero on any other difference.
"""
import json
import sys
from datetime import datetime
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

MASKS = [(1490, 0, 1920, 175), (0, 940, 500, 1010)]


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


def advanced(before, after):
    b, a = timestamp(before), timestamp(after)
    return b is not None and a is not None and a >= b


def main():
    out, screens = Path(sys.argv[1]), sys.argv[2:]
    failed = False
    for screen in screens:
        box = ImageChops.difference(load(out / f"baseline-{screen}.png"), load(out / f"candidate-{screen}.png")).getbbox()
        print(f"{screen}: {'identical' if box is None else f'DIFFERS in {box}'}")
        failed |= box is not None
    base = json.loads((out / "baseline-db.json").read_text())
    cand = json.loads((out / "candidate-db.json").read_text())
    same = base == cand
    print(f"database: {'identical' if same else 'DIFFERS'} (version {cand['user_version']}, {sum(cand['tables'].values())} rows)")
    base_ds = json.loads((out / "baseline-datastore.json").read_text())
    cand_ds = json.loads((out / "candidate-datastore.json").read_text())
    ds_diff, ds_moved = [], []
    for name in sorted(set(base_ds) | set(cand_ds)):
        b, c = base_ds.get(name, {}), cand_ds.get(name, {})
        for k in sorted(set(b) | set(c)):
            if b.get(k) == c.get(k):
                continue
            (ds_moved if advanced(b.get(k), c.get(k)) else ds_diff).append(f"{name}:{k}")
    keys = sum(len(v) for v in cand_ds.values())
    print(f"datastore: {'identical' if not ds_diff else 'DIFFERS in ' + ', '.join(ds_diff)} ({len(cand_ds)} files, {keys} keys)")
    if ds_moved:
        print(f"datastore timestamps written again by the candidate run: {', '.join(ds_moved)}")
    sys.exit(1 if failed or not same or ds_diff else 0)


if __name__ == "__main__":
    main()
