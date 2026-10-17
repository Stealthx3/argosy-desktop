#!/usr/bin/env python3
"""
Lists app files that can move to shared commonMain as they are.

A sealed type moves only with every subtype declared in another file (Kotlin forbids extending a
sealed type from another module). A file qualifies when its imports come only from ALLOWED (or are rewritten by
rewrite_imports.py, within the members its stand-in offers), its code (comments ignored) names no Android class by full name, and every in-repo declaration it names
(by import, fully qualified name, or same-package reference) is already in shared or qualifies
too. Iterates to a fixpoint. Same-package matching is by word, so it over-reports dependencies,
never under-reports them. The compiler remains the final check.

Files placed in shared androidMain may use Android APIs but not R, BuildConfig or modules shared
does not depend on; commonMain files may not depend on them.

Usage: movable.py --check           audits files already in shared against the same rules.
       movable.py --why <file>      prints the chain of dependencies that keeps <file> in app.
       movable.py --android <file>  prints what placing <file> in androidMain also needs.
       movable.py [--exclude glob ...] [path-prefix ...]   (an excluded file and everything that
                                    depends on it stays in app)   prints the qualifying files under those prefixes, together
with the qualifying files they depend on, so the printed set compiles on its own.
"""
import fnmatch
import re
import sys
from collections import defaultdict
from pathlib import Path

PKG_ROOT = "com/nendo/argosy"
APP = Path("app/src/main/kotlin")
SHARED = Path("shared/src/commonMain/kotlin")
SHARED_ANDROID = Path("shared/src/androidMain/kotlin")
ALLOWED = (
    "kotlin.", "kotlinx.coroutines.", "java.", "javax.",
    "androidx.room.", "androidx.sqlite.", "androidx.compose.runtime.",
    "retrofit2.", "okhttp3.", "okio.", "com.squareup.moshi.", "io.socket.", "org.json.",
    "org.apache.commons.compress.", "org.tukaani.xz.", "com.github.luben.zstd.",
    "`in`.dragonbra.", "org.bouncycastle.", "com.upokecenter.cbor.", "com.google.protobuf.",
    "androidx.datastore.core.", "androidx.datastore.preferences.core.",
    "dagger.Lazy", "dagger.assisted.", "dagger.Module", "dagger.Provides", "dagger.Binds",
    "dagger.multibindings.",
    "androidx.annotation.StringRes", "androidx.annotation.PluralsRes", "androidx.annotation.DrawableRes",
    "androidx.annotation.RawRes",
)
NOT_ALLOWED = ("androidx.sqlite.db.",)
REWRITTEN = (
    "android.util.Log", "com.nendo.argosy.BuildConfig", "dagger.hilt.android.lifecycle.HiltViewModel",
    "com.nendo.argosy.R",
)
GENERATED = ("com.nendo.argosy.config.AppBuildConfig",)
GUARDED_REWRITES = {
    "android.view.KeyEvent": re.compile(r"\bKeyEvent\b(?!\.KEYCODE_\w)"),
    "com.nendo.argosy.util.DisplayAffinityHelper": re.compile(
        r"\bDisplayAffinityHelper\b(?!\.isKnownDualScreenDevice\(\))"
    ),
}
STRINGS = re.compile(r'"(?:[^"\\\n]|\\.)*"')
COMMENTS = re.compile(r"/\*.*?\*/|^\s*//[^\n]*", re.S | re.M)
ANDROID_FQ = re.compile(r"(?<![\w.])(?:android|androidx|com|org|io|net|dagger|coil|in|okhttp3|retrofit2)\.[a-z]\w*(?:\.\w+)+")
ANDROID_MAIN_ALLOWED = ALLOWED + (
    "android.", "androidx.annotation.", "androidx.datastore.", "dagger.hilt.android.qualifiers.",
)
FORBIDDEN_WORDS = re.compile(r"\bR\.(color|dimen|font|xml|array|mipmap|id|style|attr|layout|menu|bool|integer)\b")
DECL = re.compile(
    r"^(?:@\S+\s+)*(?:(?:private|internal|public|data|enum|sealed|abstract|open|inline|value|const|"
    r"annotation|suspend|operator|infix|tailrec|external|fun|expect|actual)\s+)*"
    r"(?:class|interface|object|fun|val|var|typealias)\s+(?:<[^>]*>\s*)?(?:([\w.<>?, ]+)\.)?(\w+)"
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
        self.names, self.receivers = set(), {}
        for line in self.text.splitlines():
            m = DECL.match(line)
            if not m or re.search(r"\bactual\b", line.split("(")[0]):
                continue
            self.names.add(m.group(2))
            if m.group(1):
                self.receivers[m.group(2)] = re.sub(r"<.*|\?", "", m.group(1)).split(".")[-1]
        self.imports = IMPORT.findall(self.text)
        self.body = COMMENTS.sub("", IMPORT.sub("", self.text))
        self.sealed = set(re.findall(r"^\s*(?:\w+\s+)*sealed\s+(?:class|interface)\s+(\w+)", self.text, re.M))


def load(root):
    return [Unit(p, root) for p in sorted((root / PKG_ROOT).rglob("*.kt"))]


def analyse(u, index, packages):
    def resolve(fqn):
        parts = fqn.split(".")
        for i in range(len(parts) - 1, 0, -1):
            pkg = ".".join(parts[:i])
            if pkg in packages:
                return index.get((pkg, parts[i]))
        return None

    common, android, deps = [], [], set()

    def external(name, kind):
        if not name.startswith(ALLOWED) or name.startswith(NOT_ALLOWED):
            common.append(f"{kind} {name}")
        if not name.startswith(ANDROID_MAIN_ALLOWED):
            android.append(f"{kind} {name}")

    for imp in u.imports:
        if imp in GUARDED_REWRITES:
            if GUARDED_REWRITES[imp].search(u.body):
                common.append(f"uses {imp} beyond its portable stand-in")
            target = resolve(imp)
            if target:
                deps |= target
            continue
        if imp in REWRITTEN or imp in GENERATED:
            continue
        if imp.startswith("com.nendo.argosy."):
            if imp.endswith(".*"):
                for w in set(WORD.findall(u.body)):
                    deps |= index.get((imp[:-2], w), set())
                continue
            target = resolve(imp)
            if target is None:
                common.append(f"unresolved {imp}")
                android.append(f"unresolved {imp}")
            else:
                deps |= target
        else:
            external(imp, "import")
    for fq in FQ.findall(u.body):
        deps |= resolve(fq) or set()
    for fq in ANDROID_FQ.findall(STRINGS.sub('""', u.body)):
        if not fq.startswith(REWRITTEN) and not fq.startswith("com.nendo.argosy."):
            external(fq, "reference")
    if FORBIDDEN_WORDS.search(u.body):
        common.append("R type with no shared resources")
        android.append("R type with no shared resources")
    if u.pkg == "com.nendo.argosy" and re.search(r"\bR\.\w", u.body):
        common.append("R without an import")
        android.append("R without an import")
    if u.pkg == "com.nendo.argosy" and re.search(r"\bBuildConfig\b", u.body):
        common.append("BuildConfig without an import")
        android.append("BuildConfig without an import")
    words = set(WORD.findall(u.body))
    for w in words:
        deps |= {t for t in index.get((u.pkg, w), set()) if t.receivers.get(w, w) == w or t.receivers[w] in words}
    deps.discard(u)
    return deps, common, android


def main():
    args = sys.argv[1:]
    excluded = []
    while "--exclude" in args:
        i = args.index("--exclude")
        excluded.append(args[i + 1])
        del args[i:i + 2]
    app, common_shared, android_shared = load(APP), load(SHARED), load(SHARED_ANDROID)
    index = defaultdict(set)
    packages = set()
    for u in app + common_shared + android_shared:
        packages.add(u.pkg)
        for n in u.names:
            index[(u.pkg, n)].add(u)
    in_common = set(common_shared)
    in_shared = in_common | set(android_shared)

    deps, common_reasons, android_reasons = {}, {}, {}
    everything = app + common_shared + android_shared
    for u in everything:
        deps[u], common_reasons[u], android_reasons[u] = analyse(u, index, packages)
    for sealed_unit in everything:
        for name in sealed_unit.sealed:
            header = re.compile(
                r"\b(?:(?:class|interface)\s+\w+(?:<[^>]*>)?|object(?:\s+\w+)?)"
                r"(?:\s*(?:private|internal|public|protected)?\s*(?:constructor)?\s*"
                r"\((?:[^()]|\((?:[^()]|\([^()]*\))*\))*\))?"
                r"\s*:\s*(?:[\w.<>?]+(?:\([^)]*\))?\s*,\s*)*(?:\w+\.)*" + re.escape(name) + r"\b"
            )
            for u in everything:
                if u is not sealed_unit and u.pkg == sealed_unit.pkg and header.search(u.body):
                    deps[sealed_unit].add(u)

    if args[:1] == ["--check"]:
        bad = 0
        for u in common_shared:
            outside = sorted(d.rel for d in deps[u] if d not in in_common)
            for problem in common_reasons[u] + [f"depends on {r} outside commonMain" for r in outside]:
                print(f"commonMain {u.rel}: {problem}")
                bad += 1
        for u in android_shared:
            outside = sorted(d.rel for d in deps[u] if d not in in_shared)
            for problem in android_reasons[u] + [f"depends on {r} in app" for r in outside]:
                print(f"androidMain {u.rel}: {problem}")
                bad += 1
        print(f"{bad} problems")
        sys.exit(1 if bad else 0)

    def held_back(u):
        return any(fnmatch.fnmatchcase(u.rel, glob.replace("**", "*")) for glob in excluded)

    movable = {u for u in app if not common_reasons[u] and not held_back(u)}
    changed = True
    while changed:
        changed = False
        for u in list(movable):
            if any(dep not in movable and dep not in in_common for dep in deps[u]):
                movable.discard(u)
                changed = True

    by_rel = {u.rel: u for u in app}
    if args[:1] == ["--why"]:
        if args[1] not in by_rel:
            sys.exit(f"{args[1]} is not in app")
        seen, current = set(), by_rel[args[1]]
        while current is not None and current not in seen:
            seen.add(current)
            if current in movable:
                print(f"{current.rel}: movable")
                return
            if common_reasons[current]:
                print(f"{current.rel}: {'; '.join(common_reasons[current])}")
                return
            stuck = sorted((d for d in deps[current] if d not in movable and d not in in_common), key=lambda x: x.rel)
            print(f"{current.rel} -> {', '.join(d.rel for d in stuck[:6])}")
            current = stuck[0] if stuck else None
        return

    if args[:1] == ["--android"]:
        place = {by_rel[a] for a in args[1:]}
        pending, problems = list(place), []
        while pending:
            u = pending.pop()
            if android_reasons[u]:
                problems.append(f"{u.rel}: {'; '.join(android_reasons[u])}")
            for dep in deps[u]:
                if dep in in_shared or dep in place or dep in movable:
                    continue
                place.add(dep)
                pending.append(dep)
        for u in sorted(place, key=lambda x: x.rel):
            print(f"androidMain {u.rel}")
        for u in sorted({d for p in place for d in deps[p] if d in movable}, key=lambda x: x.rel):
            print(f"commonMain {u.rel}")
        for line in problems:
            print(f"blocked {line}")
        return

    chosen = {u for u in movable if not args or u.rel.startswith(tuple(args))}
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
