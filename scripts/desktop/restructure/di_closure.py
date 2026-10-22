#!/usr/bin/env python3
"""
Reports the Dagger graph a set of root classes needs on the desktop.

Walks @Inject constructors (and @Provides / @Binds return types) from the roots, resolving each
parameter type through the declaring file's imports, its package, or a fully qualified name. Each
type is classed by where Dagger on the desktop can find it:
  common   @Inject constructor in commonMain or desktopMain (Dagger builds it; walked further)
  bound    a @Provides/@Binds in desktopMain returns it
  expect   an expect class in commonMain (the desktop actual needs an @Inject constructor or a binding)
  android  declared only in androidMain or app (blocks the desktop graph)
  abstract an interface/abstract class in commonMain with no desktop binding
  external not declared in the repo (a library type: needs a desktop @Provides)
Types reached through dagger.Lazy, Provider or Set/Map multibindings are walked the same way.

Usage: di_closure.py <Root> [<Root> ...]   (simple or fully qualified names)
       --tree   prints, for each blocker, the chain from a root that reaches it
"""
import re
import sys
from collections import defaultdict, deque
from pathlib import Path

ROOTS = {
    "common": Path("shared/src/commonMain/kotlin"),
    "desktop": Path("shared/src/desktopMain/kotlin"),
    "androidMain": Path("shared/src/androidMain/kotlin"),
    "app": Path("app/src/main/kotlin"),
}
GENERATED = Path("shared/build/generated/desktopDaoModule/desktopMain")

DECL = re.compile(r"^(?:[\w@()., \"]*\s)?(?:(expect|actual|abstract|sealed|open|data|internal|private|public|enum|annotation|inner|value|fun)\s+)*(class|interface|object|typealias)\s+(\w+)", re.M)
INJECT = re.compile(r"@Inject\s+constructor\s*\(")
CLASS = re.compile(r"\bclass\s+(\w+)")
PROVIDES = re.compile(r"@(Provides|Binds)\b[\s\S]*?fun\s+(?:<[^>]*>\s*)?(\w+)\s*\(([^)]*)\)\s*:\s*([\w.<>?, ]+?)\s*(?:=|\{|\n)", re.S)
WRAPPERS = ("Lazy", "Provider", "dagger.Lazy", "javax.inject.Provider", "Set", "Map", "MutableSet", "List")


def strip_comments(s):
    s = re.sub(r"/\*.*?\*/", "", s, flags=re.S)
    return re.sub(r"//[^\n]*", "", s)


class File:
    def __init__(self, path, root):
        self.path = path
        self.root = root
        self.text = strip_comments(path.read_text(errors="replace"))
        m = re.search(r"^package\s+([\w.]+)", self.text, re.M)
        self.pkg = m.group(1) if m else ""
        self.imports = {}
        for imp in re.findall(r"^import\s+([\w.`]+)(?:\s+as\s+(\w+))?", self.text, re.M):
            name, alias = imp
            name = name.replace("`", "")
            self.imports[alias or name.rsplit(".", 1)[-1]] = name


def balanced(text, start):
    depth = 1
    i = start
    while i < len(text) and depth:
        c = text[i]
        if c in "(<":
            depth += 1 if c == "(" else 0
        if c == ")":
            depth -= 1
        i += 1
    return text[start:i - 1]


def params(sig):
    out, depth, cur = [], 0, ""
    for c in sig:
        if c in "(<":
            depth += 1
        elif c in ")>":
            depth -= 1
        if c == "," and depth == 0:
            out.append(cur)
            cur = ""
        else:
            cur += c
    if cur.strip():
        out.append(cur)
    types = []
    for p in out:
        p = re.sub(r"@\w+(?:\([^)]*\))?(?:\.\w+)*", "", p)
        if ":" not in p:
            continue
        t = p.split(":", 1)[1].split("=", 1)[0].strip()
        types.append(t)
    return types


def unwrap(t):
    t = t.strip().rstrip("?")
    while True:
        m = re.match(r"([\w.]+)\s*<(.*)>$", t)
        if not m:
            return [t]
        head, inner = m.groups()
        if head in WRAPPERS:
            parts = params("x: " + inner.replace(",", ", x: ")) if head.endswith("Map") else [inner]
            return [u for p in (parts[-1:] if head.endswith("Map") else parts) for u in unwrap(p)]
        return [head]


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    tree = "--tree" in sys.argv
    files = [File(p, r) for r, base in ROOTS.items() for p in base.rglob("*.kt")]
    if GENERATED.exists():
        files += [File(p, "desktop") for p in GENERATED.rglob("*.kt")]
    decls = defaultdict(list)
    for f in files:
        for m in DECL.finditer(f.text):
            mods = m.group(0)
            decls[f"{f.pkg}.{m.group(3)}"].append((f, m.group(2), mods))
    by_simple = defaultdict(set)
    for fq in decls:
        by_simple[fq.rsplit(".", 1)[-1]].add(fq)

    def resolve(f, name):
        name = name.strip()
        if "." in name and name in decls:
            return name
        first = name.split(".")[0]
        if first in f.imports:
            return f.imports[first] + name[len(first):]
        if f"{f.pkg}.{name}" in decls:
            return f"{f.pkg}.{name}"
        if name in decls:
            return name
        if name in ("Context", "Application"):
            return "android.content." + name
        cands = by_simple.get(name, ())
        return next(iter(cands)) if len(cands) == 1 else name

    injects = {}
    for f in files:
        for m in INJECT.finditer(f.text):
            owners = CLASS.findall(f.text, 0, m.start())
            if not owners:
                continue
            fq = f"{f.pkg}.{owners[-1]}"
            sig = balanced(f.text, m.end())
            injects.setdefault(fq, []).append((f, [resolve(f, u) for t in params(sig) for u in unwrap(t)]))
    provided = defaultdict(list)
    for f in files:
        if "@Provides" not in f.text and "@Binds" not in f.text:
            continue
        for m in PROVIDES.finditer(f.text):
            ret = m.group(4).strip()
            deps = [resolve(f, u) for t in params(m.group(3)) for u in unwrap(t)]
            for r in unwrap(ret):
                provided[resolve(f, r)].append((f, deps))

    def classify(fq):
        for f, deps in provided.get(fq, []):
            if f.root == "desktop":
                return "bound", deps
        for f, deps in injects.get(fq, []):
            if f.root in ("common", "desktop"):
                return "common", deps
        ds = decls.get(fq, [])
        if not ds:
            return "external", []
        roots = {f.root for f, _, _ in ds}
        if roots <= {"androidMain", "app"}:
            return "android", []
        for f, kind, mods in ds:
            if f.root == "common" and "expect" in mods:
                return "expect", []
        for f, kind, mods in ds:
            if f.root == "desktop" and "actual" in mods:
                return "expect", []
        return "abstract", []

    roots = [r if r in decls else next(iter(by_simple.get(r, {r}))) for r in args]
    seen, parent, order = {}, {}, deque(roots)
    for r in roots:
        parent[r] = None
    while order:
        fq = order.popleft()
        if fq in seen:
            continue
        kind, deps = classify(fq)
        seen[fq] = kind
        for d in deps:
            if d not in seen and d not in parent:
                parent[d] = fq
            order.append(d)

    groups = defaultdict(list)
    for fq, kind in seen.items():
        groups[kind].append(fq)
    for kind in ("android", "external", "abstract", "expect", "bound", "common"):
        items = sorted(groups.get(kind, []))
        print(f"## {kind} ({len(items)})")
        if kind == "common" and not tree:
            continue
        for fq in items:
            line = f"  {fq}"
            if tree and kind != "common":
                chain, p = [], parent.get(fq)
                while p:
                    chain.append(p.rsplit(".", 1)[-1])
                    p = parent.get(p)
                line += "   <- " + " <- ".join(chain)
            print(line)


if __name__ == "__main__":
    main()
