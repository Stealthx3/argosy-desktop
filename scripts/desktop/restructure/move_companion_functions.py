#!/usr/bin/env python3
"""
Moves listed companion-object functions out to Class.Companion extensions in app
(companion-moves.txt), so the class itself can move to shared.

Every overload of a listed function moves with its body unchanged; the companion stays, without
a body, as the extensions' receiver. Callers outside the package get the import they need.
Idempotent.
"""
import re
from pathlib import Path

PKG = "com/nendo/argosy"
APP = Path("app/src/main/kotlin") / PKG
ROOTS = [APP] + [Path(f"shared/src/{s}/kotlin") / PKG for s in ("commonMain", "androidMain")]
MANIFEST = Path(__file__).with_name("companion-moves.txt")


def find(rel):
    return next((r / rel for r in ROOTS if (r / rel).exists()), None)


def balanced_end(lines, start):
    depth = 0
    seen_body = False
    for i in range(start, len(lines)):
        code = re.sub(r'"(?:[^"\\\\]|\\\\.)*"', '""', lines[i])
        depth += code.count("(") + code.count("{") - code.count(")") - code.count("}")
        seen_body = seen_body or "=" in code or "{" in code
        if depth == 0 and seen_body and i + 1 < len(lines) and not lines[i + 1].startswith("            "):
            return i
    return len(lines) - 1


def main():
    outputs = {}
    for raw in MANIFEST.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        left, right = (s.strip() for s in line.split("->"))
        source_rel, member = left.split()
        cls, fn = member.split(".")
        target_rel, *rest = right.split()
        callers = rest[rest.index("import-in:") + 1:] if "import-in:" in rest else []
        path = find(source_rel)
        lines = path.read_text().split("\n")
        starts = [i for i, l in enumerate(lines) if re.match(rf"        fun {re.escape(fn)}\(", l)]
        if not starts:
            print(f"skip {member}: already moved")
            continue
        blocks, drop = [], set()
        for s in starts:
            e = balanced_end(lines, s)
            while s > 0 and (lines[s - 1].strip().startswith("@") or lines[s - 1].strip().endswith("*/")):
                if lines[s - 1].strip().endswith("*/"):
                    while not lines[s - 1].strip().startswith("/*"):
                        s -= 1
                s -= 1
            blocks.append(lines[s:e + 1])
            drop.update(range(s, e + 1))
            if e + 1 < len(lines) and not lines[e + 1].strip():
                drop.add(e + 1)
        kept = [l for i, l in enumerate(lines) if i not in drop]
        text = "\n".join(kept)
        text = re.sub(r"companion object \{\s*\}", "companion object", text)
        package = re.search(r"^package\s+([\w.]+)", text, re.M).group(1)
        imports = [l for l in lines if l.startswith("import ")]
        moved_text = "\n".join("\n".join(b) for b in blocks)
        words = set(re.findall(r"\w+", moved_text))
        used = [l for l in imports if l.split(".")[-1].split(" as ")[-1] in words]
        rest_words = set(re.findall(r"\w+", re.sub(r"^import .*$", "", text, flags=re.M)))
        text = "\n".join(
            l for l in text.split("\n")
            if not l.startswith("import ") or l.split(".")[-1].split(" as ")[-1] in rest_words
        )
        text = re.sub(r"^(package [^\n]+\n)\n+", r"\1\n", text)
        path.write_text(text)
        functions = []
        for b in blocks:
            body = [re.sub(rf"^        fun {re.escape(fn)}\(", f"fun {cls}.Companion.{fn}(", l) for l in b]
            functions.append("\n".join(l[8:] if l.startswith("        ") else l for l in body))
        entry = outputs.setdefault(target_rel, {"package": package, "imports": [], "functions": []})
        entry["imports"] += [l for l in used if l not in entry["imports"]]
        entry["functions"] += functions
        for caller in callers:
            cpath = find(caller)
            ctext = cpath.read_text()
            imp = f"import {package}.{fn}"
            if re.search(rf"^{re.escape(imp)}$", ctext, re.M):
                continue
            last = list(re.finditer(r"^import .*$", ctext, re.M))[-1]
            cpath.write_text(ctext[:last.end()] + "\n" + imp + ctext[last.end():])
        print(f"{member} -> app {target_rel}")
    for target_rel, entry in outputs.items():
        used = sorted(entry["imports"])
        out = f"package {entry['package']}\n\n" + "\n".join(used) + ("\n\n" if used else "") + "\n\n".join(entry["functions"]) + "\n"
        (APP / target_rel).write_text(out)


if __name__ == "__main__":
    main()
