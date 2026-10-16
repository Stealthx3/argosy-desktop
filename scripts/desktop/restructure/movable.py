#!/usr/bin/env python3
"""
Lists app files that can move to shared commonMain as they are.

A file qualifies when its imports come only from ALLOWED (or are rewritten by
rewrite_imports.py, within the members its stand-in offers), its code (comments ignored) names no Android class by full name, and every in-repo declaration it names
(by import, fully qualified name, or same-package reference) is already in shared or qualifies
too. Iterates to a fixpoint. Same-package matching is by word, so it over-reports dependencies,
never under-reports them. The compiler remains the final check.

Usage: movable.py --why <file>      prints the chain of dependencies that keeps <file> in app.
       movable.py [path-prefix ...]   prints the qualifying files under those prefixes, together
with the qualifying files they depend on, so the printed set compiles on its own.
"""
import re
import sys
from collections import defaultdict
from pathlib import Path

PKG_ROOT = "com/nendo/argosy"
APP = Path("app/src/main/kotlin")
SHARED = Path("shared/src/commonMain/kotlin")
ALLOWED = (
    "kotlin.", "kotlinx.coroutines.", "java.", "javax.",
    "androidx.room.", "androidx.sqlite.", "androidx.compose.runtime.",
    "retrofit2.", "okhttp3.", "okio.", "com.squareup.moshi.", "io.socket.", "org.json.",
    "org.apache.commons.compress.", "org.tukaani.xz.", "com.github.luben.zstd.",
    "`in`.dragonbra.", "org.bouncycastle.", "com.upokecenter.cbor.", "com.google.protobuf.",
    "androidx.datastore.core.", "androidx.datastore.preferences.core.",
)
NOT_ALLOWED = ("androidx.sqlite.db.",)
REWRITTEN = ("android.util.Log",)
GUARDED_REWRITES = {
    "android.view.KeyEvent": re.compile(r"\bKeyEvent\b(?!\.KEYCODE_\w)"),
    "com.nendo.argosy.util.DisplayAffinityHelper": re.compile(
        r"\bDisplayAffinityHelper\b(?!\.isKnownDualScreenDevice\(\))"
    ),
}
COMMENTS = re.compile(r"/\*.*?\*/|^\s*//[^\n]*", re.S | re.M)
ANDROID_FQ = re.compile(r"\bandroidx?\.[a-z]\w*(?:\.\w+)+")
FORBIDDEN_WORDS = re.compile(r"\b(R\.(string|plurals|drawable|raw|color|dimen|font|xml|array|mipmap|id)\b|BuildConfig\b)")
DECL = re.compile(
    r"^(?:@\S+\s+)*(?:(?:private|internal|public|data|enum|sealed|abstract|open|inline|value|const|"
    r"annotation|suspend|operator|infix|tailrec|external|fun|expect|actual)\s+)*"
    r"(?:class|interface|object|fun|val|var|typealias)\s+(?:<[^>]*>\s*)?(?:[\w.<>?, ]+\.)?(\w+)"
)
IMPORT = re.compile(r"^import\s+([\w.]+)(?:\s+as\s+\w+)?", re.M)
FQ = re.compile(r"\bcom\.nendo\.argosy(?:\.\w+)+")
WORD = re.compile(r"\b[A-Za-z_]\w*\b")


class Unit:
    def __init__(self, path, root):
        self.path = path
        self.rel = str(path.relative_to(root / PKG_ROOT))
        self.text = path.read_text()
        m = re.search(r"^package\s+([\w.]+)", self.text, re.M)
        self.pkg = m.group(1) if m else ""
        self.names = {m.group(1) for line in self.text.splitlines() if (m := DECL.match(line))}
        self.imports = IMPORT.findall(self.text)
        self.body = COMMENTS.sub("", IMPORT.sub("", self.text))


def load(root):
    return [Unit(p, root) for p in sorted((root / PKG_ROOT).rglob("*.kt"))]


def main():
    prefixes = sys.argv[1:]
    app, shared = load(APP), load(SHARED)
    index = defaultdict(set)
    packages = set()
    for u in app + shared:
        packages.add(u.pkg)
        for n in u.names:
            index[(u.pkg, n)].add(u)
    shared_set = set(shared)

    def resolve(fqn):
        parts = fqn.split(".")
        for i in range(len(parts) - 1, 0, -1):
            pkg = ".".join(parts[:i])
            if pkg in packages:
                return index.get((pkg, parts[i]))
        return None

    deps, blocked = {}, {}
    for u in app:
        reasons, d = [], set()
        for imp in u.imports:
            if imp in GUARDED_REWRITES:
                if GUARDED_REWRITES[imp].search(u.body):
                    reasons.append(f"uses {imp} beyond its portable stand-in")
                continue
            if imp.startswith("com.nendo.argosy."):
                if imp.endswith(".*"):
                    pkg = imp[:-2]
                    words = set(WORD.findall(u.body))
                    for w in words:
                        d |= index.get((pkg, w), set())
                    continue
                target = resolve(imp)
                if target is None:
                    reasons.append(f"unresolved {imp}")
                else:
                    d |= target
            elif imp in REWRITTEN:
                continue
            elif not imp.startswith(ALLOWED) or imp.startswith(NOT_ALLOWED):
                reasons.append(f"import {imp}")
        for fq in FQ.findall(u.body):
            target = resolve(fq)
            if target:
                d |= target
        for fq in ANDROID_FQ.findall(u.body):
            if fq.startswith(REWRITTEN):
                continue
            if not fq.startswith(ALLOWED) or fq.startswith(NOT_ALLOWED):
                reasons.append(f"reference {fq}")
        if FORBIDDEN_WORDS.search(u.body):
            reasons.append("R or BuildConfig")
        for w in set(WORD.findall(u.body)):
            d |= index.get((u.pkg, w), set())
        d.discard(u)
        deps[u] = d
        if reasons:
            blocked[u] = reasons

    movable = {u for u in app if u not in blocked}
    changed = True
    while changed:
        changed = False
        for u in list(movable):
            if any(dep not in movable and dep not in shared_set for dep in deps[u]):
                movable.discard(u)
                changed = True

    if prefixes[:1] == ["--why"]:
        by_rel = {u.rel: u for u in app}
        seen, current = set(), by_rel[prefixes[1]]
        while current is not None and current not in seen:
            seen.add(current)
            if current in movable:
                print(f"{current.rel}: movable")
                return
            if current in blocked:
                print(f"{current.rel}: {'; '.join(blocked[current])}")
                return
            stuck = sorted((d for d in deps[current] if d not in movable and d not in shared_set), key=lambda x: x.rel)
            print(f"{current.rel} -> {', '.join(d.rel for d in stuck[:6])}")
            current = stuck[0] if stuck else None
        return

    chosen = {u for u in movable if not prefixes or u.rel.startswith(tuple(prefixes))}
    pending = list(chosen)
    while pending:
        for dep in deps[pending.pop()]:
            if dep in movable and dep not in chosen:
                chosen.add(dep)
                pending.append(dep)
    for u in sorted(chosen, key=lambda x: x.rel):
        print(u.rel)


if __name__ == "__main__":
    main()
