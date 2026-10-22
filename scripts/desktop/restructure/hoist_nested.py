#!/usr/bin/env python3
"""
Lifts nested declarations listed in nested-hoists.txt out of their class to a top-level file in
shared commonMain (same package, named after the declaration) and rewrites Outer.Nested references
in app and shared sources to Nested, importing it where the package differs.

The declaration's text is cut from the class and dedented, so an upstream change to it carries
over; an `actual` modifier is dropped. Idempotent: a declaration already gone is skipped and only
the references are rewritten. Run from the repository root after move_members.py, before slices.py.
"""
import re
from pathlib import Path

ROOT = Path("com/nendo/argosy")
COMMON = Path("shared/src/commonMain/kotlin")
ANDROID = Path("shared/src/androidMain/kotlin")
SOURCES = [Path("app/src"), Path("shared/src")]
MANIFEST = Path(__file__).with_name("nested-hoists.txt")


def find_close(text, open_at):
    depth = 0
    for i in range(open_at, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return i
    raise ValueError("unbalanced braces")


def cut_nested(path, nested):
    text = path.read_text()
    m = re.search(rf"^    (?:(?:actual|public|data|enum|sealed)\s+)*(?:class|interface|object)\s+{nested}\b", text, re.M)
    if not m:
        return None
    end = find_close(text, text.index("{", m.end()))
    start = m.start()
    while start > 0 and text[text.rfind("\n", 0, start - 1) + 1:start].strip().startswith(("@", "/**", "*")):
        start = text.rfind("\n", 0, start - 1) + 1
    block = text[start:end + 1]
    tail = end + 2 if text[end + 1:end + 3] == "\n\n" else end + 1
    path.write_text(text[:start] + text[tail + 1:])
    lines = [l[4:] if l.startswith("    ") else l for l in block.split("\n")]
    return re.sub(r"^((?:\w+\s+)*?)actual\s+", r"\1", "\n".join(lines))


def rewrite_references(outer, nested, pkg):
    qualified = re.compile(rf"\b{outer}\.{nested}\b")
    for root in SOURCES:
        for path in root.rglob("*.kt"):
            if "/build/" in path.as_posix():
                continue
            text = path.read_text()
            new = text.replace(f"import {pkg}.{outer}.{nested}\n", f"import {pkg}.{nested}\n")
            body_start = new.find("\n", max(new.rfind("\nimport "), 0) + 1)
            new = new[:body_start] + qualified.sub(nested, new[body_start:])
            if new == text:
                continue
            file_pkg = re.search(r"^package ([\w.]+)", new, re.M)
            if file_pkg and file_pkg.group(1) != pkg and f"import {pkg}.{nested}\n" not in new:
                anchor = re.search(r"^import [^\n]+\n", new, re.M)
                at = anchor.start() if anchor else new.index("\n", file_pkg.end()) + 1
                new = new[:at] + f"import {pkg}.{nested}\n" + new[at:]
            path.write_text(new)
            print(f"rewrote {outer}.{nested} in {path}")


def main():
    for line in MANIFEST.read_text().splitlines():
        line = line.split("#")[0].strip()
        if ":" not in line:
            continue
        rel, spec = (s.strip() for s in line.split(":", 1))
        outer, nested = spec.split(".")
        path = ANDROID / ROOT / rel
        pkg = re.search(r"^package ([\w.]+)", path.read_text(), re.M).group(1)
        block = cut_nested(path, nested)
        if block is not None:
            target = (COMMON / ROOT / rel).parent / f"{nested}.kt"
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(f"package {pkg}\n\n{block}\n")
            print(f"hoisted {outer}.{nested} to {target}")
        rewrite_references(outer, nested, pkg)


if __name__ == "__main__":
    main()
