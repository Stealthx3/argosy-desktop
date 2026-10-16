#!/usr/bin/env python3
"""
Compares baseline-* and candidate-* screenshots and database summaries in a check folder.

Masks the header clock (with the blur halo it casts under the frosted side menu) and the side
menu's own clock. Exits non-zero if any screen or the database differs.
"""
import json
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw

MASKS = [(1490, 0, 1920, 175), (0, 940, 500, 1010)]


def load(path):
    image = Image.open(path).convert("RGB")
    draw = ImageDraw.Draw(image)
    for box in MASKS:
        draw.rectangle(box, fill=(0, 0, 0))
    return image


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
    sys.exit(1 if failed or not same else 0)


if __name__ == "__main__":
    main()
