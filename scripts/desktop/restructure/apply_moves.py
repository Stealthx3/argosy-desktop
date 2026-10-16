#!/usr/bin/env python3
"""
Moves app files into :shared source sets with git mv, as listed in moves.txt.

Idempotent: files already in place are left alone, so re-running after an upstream merge moves
only the files upstream added under app/. Moves never edit file contents.
"""
import fnmatch
import subprocess
import sys
from pathlib import Path

PKG = "com/nendo/argosy"
APP = Path("app/src/main/kotlin") / PKG
MANIFEST = Path(__file__).with_name("moves.txt")


def rules():
    section, out = None, {}
    for raw in MANIFEST.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("["):
            section = line.strip("[]")
            out.setdefault(section, [])
        else:
            sign, glob = line.split(None, 1)
            out[section].append((sign, glob))
    return out


def matches(rel, glob):
    return fnmatch.fnmatchcase(rel, glob.replace("**", "*"))


def selected(candidates, section_rules):
    chosen = set()
    for sign, glob in section_rules:
        hits = {c for c in candidates if matches(c, glob)}
        chosen = chosen | hits if sign == "+" else chosen - hits
    return sorted(chosen)


def main():
    if not APP.is_dir():
        sys.exit("run from the repository root")
    candidates = {str(p.relative_to(APP)) for p in APP.rglob("*.kt")}
    moved = 0
    for section, section_rules in rules().items():
        dest_root = Path("shared/src") / section / "kotlin" / PKG
        for rel in selected(candidates, section_rules):
            dest = dest_root / rel
            if dest.exists():
                sys.exit(f"{rel} exists in both app and {section}; resolve by hand")
            dest.parent.mkdir(parents=True, exist_ok=True)
            subprocess.run(["git", "mv", str(APP / rel), str(dest)], check=True)
            moved += 1
    print(f"moved {moved} files")


if __name__ == "__main__":
    main()
