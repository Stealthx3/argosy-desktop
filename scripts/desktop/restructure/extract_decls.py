#!/usr/bin/env python3
"""
Moves named top-level declarations out of a Kotlin file into a new file in the same package.

Driven by extract-decls.txt next to this script. Each moved declaration keeps its KDoc and
annotations byte for byte; each file keeps only the imports its remaining text names. Idempotent:
a declaration already absent from its source file is skipped. With --refresh, a source that holds
its declarations again (an upstream release changed them) replaces the earlier extraction, wherever
apply_moves.py has since moved it. A name written as ^NAME is
also used by the file it leaves, so its top-level `private` becomes `internal`.
"""
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path("app/src/main/kotlin/com/nendo/argosy")
SHARED_ROOTS = [Path(f"shared/src/{s}/kotlin/com/nendo/argosy") for s in ("commonMain", "androidMain")]
REFRESH = "--refresh" in sys.argv
MANIFEST = Path(__file__).with_name("extract-decls.txt")
DECL = re.compile(
    r"^(?:(?:private|internal|public|data|enum|sealed|abstract|open|inline|value|const|annotation|fun|suspend|operator|infix)\s+)*"
    r"(?:class|interface|object|fun|val|var|typealias)\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)"
)


def parse(text):
    lines = text.split("\n")
    header_end = 0
    for i, line in enumerate(lines):
        if line.startswith(("package ", "import ")):
            header_end = i + 1
    starts = []
    for i in range(header_end, len(lines)):
        m = DECL.match(lines[i])
        if not m:
            continue
        start = i
        while start - 1 >= header_end:
            prev = lines[start - 1]
            if prev.startswith("@") or prev.startswith("//"):
                start -= 1
            elif prev.rstrip().endswith("*/"):
                j = start - 1
                while j >= header_end and not lines[j].lstrip().startswith("/*"):
                    j -= 1
                start = j
            else:
                break
        starts.append((start, m.group(1)))
    items = []
    for k, (start, name) in enumerate(starts):
        end = starts[k + 1][0] if k + 1 < len(starts) else len(lines)
        body = lines[start:end]
        while body and not body[-1].strip():
            body.pop()
        items.append((name, start, end, "\n".join(body)))
    header = lines[:header_end]
    return header, items, lines


def imports_used(header, text):
    words = set(re.findall(r"\w+", text))
    kept = []
    for line in header:
        if not line.startswith("import "):
            continue
        target = line[len("import "):].strip()
        alias = target.split(" as ")[-1].strip() if " as " in target else target.split(".")[-1]
        if alias == "*" or alias in words:
            kept.append(line)
    return kept


def extract(source, new_name, names, widen):
    path = next((r / source for r in [ROOT] + SHARED_ROOTS if (r / source).exists()), ROOT / source)
    text = path.read_text()
    header, items, lines = parse(text)
    wanted = [it for it in items if it[0] in names]
    missing = names - {it[0] for it in wanted}
    target = path.with_name(new_name)
    if not wanted:
        print(f"skip {source} -> {new_name}: already extracted")
        return
    if missing:
        sys.exit(f"{source}: not found as top-level declarations: {sorted(missing)}")
    rel_target = target.relative_to(next(r for r in [ROOT] + SHARED_ROOTS if path.is_relative_to(r)))
    existing = [p for p in [ROOT / rel_target] + [r / rel_target for r in SHARED_ROOTS] if p.exists()]
    if existing and not REFRESH:
        sys.exit(f"{existing[0]} exists while {source} still holds {sorted(names)}; rerun with --refresh")
    for stale in existing:
        subprocess.run(["git", "rm", "-q", "-f", str(stale)], check=True)
        print(f"refresh: removed {stale}, regenerating from {source}")
    package = next(l for l in header if l.startswith("package "))
    moved = "\n\n".join(
        re.sub(r"^private ", "internal ", it[3], count=1) if it[0] in widen else it[3]
        for it in wanted
    )
    out = [package, ""]
    used = imports_used(header, moved)
    if used:
        out += used + [""]
    target.write_text("\n".join(out) + ("\n" if used else "") + moved + "\n")

    drop = set()
    for _, start, end, _ in wanted:
        drop.update(range(start, end))
    body_start = len(header)
    rest = [l for i, l in enumerate(lines) if i >= body_start and i not in drop]
    rest_text = "\n".join(rest)
    kept_imports = set(imports_used(header, rest_text))
    new_header = [l for l in header if not l.startswith("import ") or l in kept_imports]
    result = "\n".join(new_header + rest).rstrip("\n") + "\n"
    path.write_text(result)
    print(f"{source} -> {new_name}: {len(wanted)} declarations")


def main():
    for raw in MANIFEST.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        spec, names = line.split(":", 1)
        source, new_name = (s.strip() for s in spec.split("->"))
        tokens = names.split()
        extract(
            source,
            new_name,
            {t.lstrip("^") for t in tokens},
            {t[1:] for t in tokens if t.startswith("^")},
        )


if __name__ == "__main__":
    main()
