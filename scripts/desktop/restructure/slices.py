#!/usr/bin/env python3
"""
Gives commonMain an expect slice of Android classes kept in shared androidMain (slices.txt).

For each listed class: the public members whose signatures use only portable types become an
`expect class` in commonMain (<Class>.expect.kt, regenerated on every run); the Android class and
those members are marked `actual`, with default argument values moved to the expect declaration
(Kotlin forbids them on an actual); a desktop actual with neutral results is written once, when
missing, and never overwritten. Members left out are reported with the reason.

Idempotent. Run from the repository root after apply_moves.py and before rewrite_imports.py.
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import rewrite_imports  # noqa: E402

ROOT = Path("com/nendo/argosy")
COMMON = Path("shared/src/commonMain/kotlin")
ANDROID = Path("shared/src/androidMain/kotlin")
DESKTOP = Path("shared/src/desktopMain/kotlin")
MANIFEST = Path(__file__).with_name("slices.txt")
PORTABLE_IMPORTS = (
    "kotlin.", "kotlinx.coroutines.", "java.io.", "java.util.", "java.time.", "java.net.",
    "androidx.compose.ui.graphics.", "com.nendo.argosy.",
)
NOT_A_TYPE_STAND_IN = ("android.content.Intent",)


def portable_import(name):
    if name.startswith(PORTABLE_IMPORTS):
        return True
    if name in NOT_A_TYPE_STAND_IN:
        return False
    line = f"import {name}"
    for pattern, replacement in rewrite_imports.RULES:
        line = pattern.sub(replacement, line)
    return line.startswith("import com.nendo.argosy.")
MODIFIERS = re.compile(r"^(?:(?:public|suspend|inline|operator|infix|tailrec|override|open|final|actual)\s+)*")


def split_depth0(text, sep=","):
    parts, depth, current, quote = [], 0, [], None
    for ch in text:
        if quote:
            current.append(ch)
            if ch == quote:
                quote = None
            continue
        if ch in "\"'":
            quote = ch
        elif ch == ">" and current and current[-1] == "-":
            pass
        elif ch in "([{<":
            depth += 1
        elif ch in ")]}>":
            depth -= 1
        if ch == sep and depth == 0:
            parts.append("".join(current))
            current = []
        else:
            current.append(ch)
    if "".join(current).strip():
        parts.append("".join(current))
    return parts


def find_close(text, start):
    depth, quote = 0, None
    i = start
    while i < len(text):
        ch = text[i]
        if quote:
            if ch == "\\":
                i += 2
                continue
            if ch == quote:
                quote = None
        elif ch == '"':
            quote = ch
        elif ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    raise ValueError("unbalanced")


def class_body(text, name):
    m = re.search(rf"^(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|open|actual)\s+)*(?:class|object) {name}\b", text, re.M)
    if not m:
        raise SystemExit(f"class {name} not found")
    i = m.end()
    while text[i] != "{":
        if text[i] == "(":
            i = find_close(text, i)
        i += 1
    return m, i, find_close(text, i)


def members(text, body_start, body_end):
    """Yields (start, end_of_signature, signature_text) for public members at depth 1."""
    i = body_start + 1
    depth = 0
    line_start = True
    while i < body_end:
        ch = text[i]
        if ch == '"':
            if text.startswith('"""', i):
                i = text.index('"""', i + 3) + 3
                continue
            j = i + 1
            while text[j] != '"':
                j += 2 if text[j] == "\\" else 1
            i = j + 1
            continue
        if text.startswith("//", i):
            i = text.index("\n", i)
            continue
        if text.startswith("/*", i):
            i = text.index("*/", i) + 2
            continue
        if ch in "{(":
            depth += 1
        elif ch in "})":
            depth -= 1
        elif ch == "\n":
            line_start = True
            i += 1
            continue
        if depth == 0 and line_start and not ch.isspace():
            line_start = False
            rest = text[i:body_end]
            m = re.match(r"(?:@\w+(?:\([^)]*\))?\s+)*((?:(?:public|private|protected|internal|suspend|inline|operator|override|open|final|actual|const|lateinit|data|enum|sealed|inner|abstract|companion)\s+)*)(fun|val|var|class|object|interface)\b", rest)
            if m:
                yield i, m
                i += m.end()
                continue
        elif depth == 0 and not ch.isspace():
            line_start = False
        i += 1


def signature_end(text, start, kind):
    i = start
    depth = 0
    while i < len(text):
        ch = text[i]
        if ch == ">" and text[i - 1] == "-":
            pass
        elif ch in "(<[":
            depth += 1
        elif ch in ")>]":
            depth -= 1
        elif depth == 0 and (ch in "{=" or (kind != "fun" and ch == "\n")):
            if ch == "=" and text[i + 1] == ">":
                i += 2
                continue
            return i
        elif depth == 0 and kind != "fun" and text.startswith(" by ", i):
            return i
        i += 1
    return i


def strip_defaults(params):
    out = []
    for p in split_depth0(params):
        out.append(re.sub(r"\s*=\s*.*\Z", "", p, flags=re.S))
    return ",".join(out)


DATA_CLASSES = {}


def data_class_params(name):
    if not DATA_CLASSES:
        for path in COMMON.rglob("*.kt"):
            text = path.read_text()
            for m in re.finditer(r"^data class (\w+)\(", text, re.M):
                try:
                    close = find_close(text, m.end() - 1)
                except ValueError:
                    continue
                DATA_CLASSES.setdefault(m.group(1), text[m.end():close])
    return DATA_CLASSES.get(name)


def construct(name):
    params = data_class_params(name)
    if params is None:
        return None
    args = []
    for p in split_depth0(params):
        if "=" in p.split(":", 1)[-1] and re.search(r"=\s*\S", p):
            continue
        pm = re.match(r"\s*(?:@\w+\s+)*(?:val|var)\s+(\w+)\s*:\s*(.+)", p.strip(), re.S)
        if not pm:
            return None
        value = neutral(pm.group(2))
        if value is None:
            return None
        args.append(f"{pm.group(1)} = {value}")
    return f"{name}({', '.join(args)})"


def neutral(type_text):
    t = type_text.strip()
    if t.endswith("?"):
        return "null"
    base = re.sub(r"<.*", "", t).split(".")[-1]
    inner = t[t.find("<") + 1:t.rfind(">")] if "<" in t else ""
    simple = {
        "Boolean": "false", "Int": "0", "Long": "0L", "Float": "0f", "Double": "0.0", "String": '""',
        "Unit": "Unit", "List": "emptyList()", "Set": "emptySet()", "Map": "emptyMap()",
        "Collection": "emptyList()", "Flow": "kotlinx.coroutines.flow.emptyFlow()",
        "ByteArray": "ByteArray(0)",
    }
    if base in simple:
        return simple[base]
    if base == "StateFlow":
        value = neutral(inner)
        return None if value is None else f"kotlinx.coroutines.flow.MutableStateFlow({value})"
    if base == "SharedFlow":
        return "kotlinx.coroutines.flow.MutableSharedFlow()"
    if re.fullmatch(r"\w+", t):
        built = construct(t)
        if built:
            return built
    if base == "Result":
        return "Result.failure(UnsupportedOperationException(\"not available on desktop\"))"
    return None


def member_key(line):
    m = re.match(r"\s*(?:actual\s+)?(?:suspend\s+)?fun\s+(?:<[^>]*>\s*)?(?:[\w.<>?, ]+\.)?(\w+)\((.*)\)", line)
    if not m:
        return None
    params = []
    for p in split_depth0(strip_defaults(m.group(2))):
        pm = re.match(r"\s*(?:vararg\s+)?(\w+)\s*:\s*(.+)", p.strip(), re.S)
        if pm:
            params.append((pm.group(1), re.sub(r"\s+", "", pm.group(2)).split(".")[-1]))
    return m.group(1), tuple(params)


def declared_returns(text, body_start, body_end):
    returns = {}
    for m in re.finditer(r"\bfun\s+(?:<[^>]*>\s*)?(\w+)\(", text[body_start:body_end]):
        open_at = body_start + m.end() - 1
        close = find_close(text, open_at)
        tail = text[close + 1:close + 200]
        r = re.match(r"\s*:\s*([^={\n]+?)\s*[={\n]", tail)
        if r:
            returns.setdefault(m.group(1), r.group(1).strip())
        elif re.match(r"\s*\{", tail):
            returns.setdefault(m.group(1), "Unit")
    return returns


def main():
    if not MANIFEST.exists():
        return
    for raw in MANIFEST.read_text().splitlines():
        line = raw.split("#")[0].strip()
        if not line:
            continue
        rel, spec = (s.strip() for s in line.split(":", 1))
        cls_m = re.match(r"(\w+)(?:\((.*)\))?$", spec)
        cls = cls_m.group(1)
        overrides = dict(
            (k.strip(), v.strip()) for k, v in (o.split(":", 1) for o in split_depth0(cls_m.group(2) or "") if o.strip())
        )
        path = ANDROID / ROOT / rel
        text = path.read_text()
        pkg = re.search(r"^package ([\w.]+)", text, re.M).group(1)
        imports = re.findall(r"^import ([\w.]+)(?: as (\w+))?$", text, re.M)
        imported = {alias or name.rsplit(".", 1)[1]: name for name, alias in imports}
        top_level = set(re.findall(r"^(?:(?:data|enum|sealed|private|internal|value)\s+)*(?:class|object|interface|typealias)\s+(\w+)", text, re.M))
        cls_match, body_start, body_end = class_body(text, cls)
        expect_path = (COMMON / ROOT / rel).parent / f"{cls}.expect.kt"
        previous = {}
        if expect_path.exists():
            for line in expect_path.read_text().splitlines():
                key = member_key(line)
                if key:
                    previous[key] = line
        declared = declared_returns(text, body_start, body_end)
        nested = set(re.findall(r"^    (?:@\w+\s+)*(?:(?:data|sealed|abstract|open|inner|value)\s+)*(?:class|interface|object)\s+(\w+)", text[body_start:body_end], re.M))
        nested -= set(re.findall(r"^    (?:\w+\s+)*enum\s+class\s+(\w+)", text[body_start:body_end], re.M))
        constants = dict(re.findall(r"\bconst\s+val\s+(\w+)\s*(?::\s*\w+)?\s*=\s*([^\n]+)", text))
        expect_lines, desktop_lines, edits, skipped = [], [], [], []
        for start, m in list(members(text, body_start, body_end)):
            mods, kind = m.group(1), m.group(2)
            if re.search(r"\b(private|protected|internal|override)\b", mods):
                continue
            if kind == "class" and re.search(r"\benum\b", mods):
                decl_start = start + m.start(2)
                brace = text.index("{", decl_start)
                head = " ".join(text[decl_start:brace].split())
                entries = text[brace + 1:find_close(text, brace)]
                if not re.fullmatch(r"[\w\s,]*", entries):
                    skipped.append(f"{head}: enum entries with bodies or arguments")
                    continue
                listed = ", ".join(e.strip() for e in entries.split(",") if e.strip())
                expect_lines.append(f"    enum {head} {{ {listed} }}")
                desktop_lines.append(f"    actual enum {head} {{ {listed} }}")
                edits.append((start + m.start(1), start + m.start(1), "" if "actual" in mods else "actual "))
                continue
            if kind in ("class", "object", "interface"):
                continue
            decl_start = start + m.start(2)
            end = signature_end(text, decl_start, kind)
            sig = " ".join(text[decl_start:end].split())
            name_m = re.match(r"(?:fun|val|var)\s+(?:<[^>]*>\s*)?(?:[\w.<>?, ]+\.)?(\w+)", sig)
            name = name_m.group(1)
            if kind == "fun":
                pm = sig.index("(", name_m.end() - len(name))
                close = find_close(sig, pm)
                params, rtype = sig[pm + 1:close], sig[close + 1:].strip()
                rtype = rtype[1:].strip() if rtype.startswith(":") else ""
                if not rtype:
                    if text[end] == "=":
                        callee = re.match(r"=\s*(\w+)\(", text[end:end + 200])
                        rtype = overrides.get(name) or (declared.get(callee.group(1)) if callee else None)
                        if not rtype:
                            skipped.append(f"{name}: expression body without a declared return type")
                            continue
                    else:
                        rtype = "Unit"
            else:
                tm = re.match(r"(?:val|var)\s+\w+\s*:\s*(.+)", sig)
                if not tm:
                    skipped.append(f"{name}: no declared type")
                    continue
                rtype, params = tm.group(1).strip(), ""
                following = "\n".join(text[end:].split("\n")[1:4])
                if kind == "var" and re.search(r"^\s*(?:private|internal|protected) set\b", following, re.M):
                    kind = "val"
            words = set(re.findall(r"\b[A-Z]\w*", sig.split("=")[0] if kind != "fun" else sig))
            blocked = sorted(
                w for w in words
                if (w in imported and not portable_import(imported[w]))
                or (w in top_level and w != cls)
                or w in nested
            )
            if blocked:
                skipped.append(f"{name}: {', '.join(blocked)} not portable or declared in this file")
                continue
            suspend = "suspend " if re.search(r"\bsuspend\b", mods) else ""
            if kind == "fun":
                for const_name, const_value in constants.items():
                    params = re.sub(rf"(=\s*[^,]*?)\b{const_name}\b", lambda mm: mm.group(1) + const_value.strip(), params)
                head = sig[:sig.index("(", name_m.end() - len(name))]
                returns = "" if rtype == "Unit" else f": {rtype}"
                line_text = f"    {suspend}{head}({params.strip()}){returns}"
                if "actual" in mods and member_key(line_text) in previous:
                    line_text = previous[member_key(line_text)]
                    params = line_text[line_text.index("(") + 1:find_close(line_text, line_text.index("("))]
                expect_lines.append(line_text)
                value = neutral(rtype)
                if value is None:
                    skipped_desktop = f"TODO(\"desktop: {name}\")"
                    value = skipped_desktop
                body = " {\n    }" if rtype == "Unit" else f" = {value}"
                desktop_lines.append(f"    actual {suspend}{head}({strip_defaults(params).strip()}){returns}{body}")
                sig_text = text[decl_start:end]
                pm_abs = decl_start + sig_text.index("(", sig_text.index(name))
                close_abs = find_close(text, pm_abs)
                edits.append((pm_abs + 1, close_abs, strip_defaults(text[pm_abs + 1:close_abs])))
            else:
                expect_lines.append(f"    {kind} {name}: {rtype}")
                value = neutral(rtype) or f"TODO(\"desktop: {name}\")"
                desktop_lines.append(f"    actual {kind} {name}: {rtype} = {value}")
            edits.append((start + m.start(1), start + m.start(1), "" if "actual" in mods else "actual "))
        cls_decl = text[cls_match.start():cls_match.end()]
        kind_word = "object" if re.search(rf"\bobject {cls}\b", cls_decl) else "class"
        if not re.search(r"\bactual\b", cls_decl):
            edits.append((cls_match.end() - len(f"{kind_word} {cls}"), cls_match.end() - len(f"{kind_word} {cls}"), "actual "))
        for a, b, rep in sorted(edits, key=lambda e: e[0], reverse=True):
            text = text[:a] + rep + text[b:]
        path.write_text(text)
        body_text = "\n".join(expect_lines + desktop_lines)
        needed = []
        for name, alias in imports:
            short = alias or name.rsplit(".", 1)[1]
            if re.search(rf"\b{short}\b", body_text) and portable_import(name):
                needed.append(f"import {name}" + (f" as {alias}" if alias else ""))
        header = "\n".join(needed)
        for pattern, replacement in rewrite_imports.RULES:
            header = pattern.sub(replacement, header)
        out_dir = (COMMON / ROOT / rel).parent
        expect_text = f"package {pkg}\n\n" + (header + "\n\n" if header else "") + f"expect {kind_word} {cls} {{\n" + "\n\n".join(expect_lines) + "\n}\n"
        (out_dir / f"{cls}.expect.kt").write_text(expect_text)
        desktop_path = (DESKTOP / ROOT / rel).parent / f"{cls}.desktop.kt"
        if not desktop_path.exists():
            desktop_path.parent.mkdir(parents=True, exist_ok=True)
            singleton = "@Singleton\n" if re.search(r"@Singleton", text[max(0, cls_match.start() - 200):cls_match.end()]) else ""
            desktop_header = "\n".join(sorted(set(needed + ["import javax.inject.Inject"] + (["import javax.inject.Singleton"] if singleton else []))))
            for pattern, replacement in rewrite_imports.RULES:
                desktop_header = pattern.sub(replacement, desktop_header)
            constructor = " @Inject constructor()" if kind_word == "class" else ""
            if kind_word == "object":
                singleton = ""
                desktop_header = "\n".join(l for l in desktop_header.splitlines() if l not in ("import javax.inject.Inject", "import javax.inject.Singleton"))
            desktop_path.write_text(
                f"package {pkg}\n\n{desktop_header}\n\n{singleton}actual {kind_word} {cls}{constructor} {{\n"
                + "\n\n".join(desktop_lines) + "\n}\n"
            )
        print(f"{rel}: {cls}: {len(expect_lines)} members in the expect slice")
        for s in skipped:
            print(f"  left out {s}")


if __name__ == "__main__":
    main()
