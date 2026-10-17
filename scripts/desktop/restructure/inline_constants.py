#!/usr/bin/env python3
"""
Inlines string constants listed in inline-constants.txt.

The value is read from the source declaration each run, so a changed upstream value carries over.
Files are looked up in app and in shared's source sets. Idempotent.
"""
import re
from pathlib import Path

PKG = "com/nendo/argosy"
ROOTS = [Path("app/src/main/kotlin") / PKG] + [
    Path(f"shared/src/{s}/kotlin") / PKG for s in ("commonMain", "androidMain")
]
MANIFEST = Path(__file__).with_name("inline-constants.txt")


def find(rel):
    for root in ROOTS:
        if (root / rel).exists():
            return root / rel
    raise SystemExit(f"{rel} not found")


def main():
    for raw in MANIFEST.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        left, right = (s.strip() for s in line.split("<-"))
        target_rel, expression = left.split()
        source_rel, prop = right.split()
        source = find(source_rel).read_text()
        m = re.search(rf'(?:override |const )*val {re.escape(prop)}(?:\s*:\s*String)?\s*=\s*("(?:[^"\\\\]|\\\\.)*")', source)
        if not m:
            raise SystemExit(f"{prop} not found as a string constant in {source_rel}")
        target = find(target_rel)
        text = target.read_text()
        new = re.sub(rf"\b{re.escape(expression)}\b", m.group(1), text)
        if new != text:
            target.write_text(new)
            print(f"{target_rel}: {expression} -> {m.group(1)}")


if __name__ == "__main__":
    main()
