"""Copies named top-level declarations, verbatim, out of upstream files too entangled to copy whole.

Reads extracted-decls.txt (lines: `<upstream path relative to app/src/main/kotlin>: Name Name ...`)
and writes one file per source into src/extracted/kotlin, keeping the package and only the imports
the extracted text uses. Each kept declaration includes its KDoc and annotations.

usage: python3 -I extract_decls.py
"""
import os
import re
import sys

# `by` delegation calls these without naming them.
DELEGATE_OPS = {"getValue", "setValue", "provideDelegate"}
HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "..", "..", "app", "src", "main", "kotlin")
DST = os.path.join(HERE, "src", "extracted", "kotlin")

DECL = re.compile(r'^(?:@[\w.]+(?:\([^)]*\))?\s+)*(?:(?:public|internal|private|data|sealed|enum|abstract|open|inline|value|annotation|const|fun interface)\s+)*'
                  r'(?:class|object|interface|fun|val|var|typealias)\s+(?:<[^>]+>\s*)?(?:[\w.<>?, ]+\.)?(\w+)')


def blocks(lines):
    """Yields (name, start, end) per top-level declaration. Upstream declares everything at column 0,
    so a declaration runs until the KDoc/annotations of the next one (or end of file)."""
    starts = []
    for i, line in enumerate(lines):
        m = DECL.match(line)
        if m and not line.startswith((" ", "\t")):
            s = i
            while s > 0 and (lines[s - 1].startswith(("@", "/**", " *", " */")) or lines[s - 1].startswith("*/")):
                s -= 1
            starts.append((m.group(1), s))
    for k, (name, s) in enumerate(starts):
        e = (starts[k + 1][1] if k + 1 < len(starts) else len(lines)) - 1
        while e > s and (lines[e].strip() == "" or lines[e].startswith("//")):
            e -= 1
        yield name, s, e


def main():
    spec = os.path.join(HERE, "extracted-decls.txt")
    out_root = DST
    if os.path.isdir(out_root):
        for d, _, fs in os.walk(out_root):
            for f in fs:
                os.remove(os.path.join(d, f))
    count = 0
    for raw in open(spec, encoding="utf-8"):
        raw = raw.strip()
        if not raw or raw.startswith("#"):
            continue
        rel, names = raw.split(":", 1)
        want = names.split()
        text = open(os.path.join(SRC, rel), encoding="utf-8").read()
        lines = text.splitlines()
        found = {}
        for name, s, e in blocks(lines):
            if name in want and name not in found:
                found[name] = "\n".join(lines[s:e + 1])
        missing = [w for w in want if w not in found]
        if missing:
            sys.exit(f"{rel}: not found: {missing}")
        body = "\n\n".join(found[w] for w in want)
        pkg = re.search(r'^package .*$', text, re.M).group(0)
        used = set(re.findall(r'\b\w+\b', body))
        imports = [l for l in lines if l.startswith("import ")
                   and (l.split(" as ")[-1].rsplit(".", 1)[-1].strip() in used | DELEGATE_OPS or l.rstrip().endswith(".*"))]
        out = os.path.join(out_root, os.path.dirname(rel), "Extracted" + os.path.basename(rel))
        os.makedirs(os.path.dirname(out), exist_ok=True)
        header = f"// Extracted verbatim from app/src/main/kotlin/{rel}: {' '.join(want)}\n"
        with open(out, "w", encoding="utf-8") as fh:
            fh.write(header + pkg + "\n\n" + "\n".join(imports) + "\n\n" + body + "\n")
        count += len(want)
    print(f"extracted {count} declarations")


main()
