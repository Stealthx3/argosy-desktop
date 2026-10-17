#!/usr/bin/env python3
"""
Moves files into :shared source sets with git mv, as listed in moves.txt. A file ends up in the
last section that lists it, wherever it is now (app, or another shared source set); a path present
in more than one source set (an expect and its actual) stays where it is.

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
    blocks = []
    for raw in MANIFEST.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("["):
            blocks.append((line.strip("[]"), []))
        else:
            sign, glob = line.split(None, 1)
            blocks[-1][1].append((sign, glob))
    return blocks


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
    roots = {"app": APP}
    for section, _ in rules():
        roots.setdefault(section, Path("shared/src") / section / "kotlin" / PKG)
    where, split = {}, set()
    for name, root in roots.items():
        for p in root.rglob("*.kt") if root.is_dir() else ():
            rel = str(p.relative_to(root))
            if rel in where:
                split.add(rel)
            where[rel] = name
    for rel in split:
        del where[rel]
    target = {}
    for section, section_rules in rules():
        for rel in selected(set(where), section_rules):
            target[rel] = section
    moved = 0
    for rel, section in sorted(target.items()):
        if where[rel] == section:
            continue
        source, dest = roots[where[rel]] / rel, roots[section] / rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        tracked = subprocess.run(["git", "ls-files", "--error-unmatch", str(source)], capture_output=True).returncode == 0
        if tracked:
            subprocess.run(["git", "mv", str(source), str(dest)], check=True)
        else:
            source.rename(dest)
            subprocess.run(["git", "add", str(dest)], check=True)
        moved += 1
    print(f"moved {moved} files")


if __name__ == "__main__":
    main()
