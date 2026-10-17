#!/usr/bin/env python3
"""
Turns listed members of shared classes into extension properties in app (member-moves.txt).

The member's text is cut from the shared file and the extension is regenerated from it, so an
upstream change to the body carries over. Idempotent: a member already gone is skipped.
"""
import re
from pathlib import Path

PKG = "com/nendo/argosy"
SHARED = Path("shared/src/commonMain/kotlin") / PKG
APP = Path("app/src/main/kotlin") / PKG
MANIFEST = Path(__file__).with_name("member-moves.txt")


def cut_member(text, prop):
    lines = text.split("\n")
    start = next((i for i, l in enumerate(lines) if re.match(rf"    val {re.escape(prop)}\b", l)), None)
    if start is None:
        return text, None
    end = start + 1
    while end < len(lines) and (lines[end].startswith("        ") or not lines[end].strip()):
        if not lines[end].strip():
            break
        end += 1
    block = lines[start:end]
    rest = lines[:start] + lines[end + 1 if end < len(lines) and not lines[end].strip() else end:]
    return "\n".join(rest), block


def main():
    for raw in MANIFEST.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        left, right = (s.strip() for s in line.split("->"))
        shared_file, member = left.split()
        app_file, package = right.split()
        cls, prop = member.split(".")
        path = SHARED / shared_file
        text, block = cut_member(path.read_text(), prop)
        if block is None:
            print(f"skip {member}: already moved")
            continue
        path.write_text(text)
        shared_pkg = re.search(r"^package\s+([\w.]+)", text, re.M).group(1)
        body = [re.sub(rf"^    val {re.escape(prop)}", f"val {cls}.{prop}", block[0])] + [l[4:] for l in block[1:]]
        out = f"package {package}\n\nimport {shared_pkg}.{cls}\n\n" + "\n".join(body) + "\n"
        (APP / app_file).write_text(out)
        print(f"{member} -> app {app_file}")


if __name__ == "__main__":
    main()
