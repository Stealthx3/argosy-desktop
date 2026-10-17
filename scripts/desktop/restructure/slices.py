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
    "kotlin.", "kotlinx.coroutines.", "java.io.", "java.util.", "java.time.", "java.net.", "java.security.",
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
    m = re.search(rf"^(?:@\w+(?:\([^)]*\))?\s+)*(?:(?:public|open|actual|fun)\s+)*(?:class|object|interface) {name}\b", text, re.M)
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
        return "kotlin.Result.failure(UnsupportedOperationException(\"not available on desktop\"))"
    return None


STDLIB_SUPERTYPES = ("AutoCloseable", "Closeable", "java.io.Closeable", "Runnable")


def kind_word_guess(text, cls_match, cls):
    decl = text[cls_match.start():cls_match.end()]
    return next(w for w in ("object", "interface", "class") if re.search(rf"\b{w} {cls}\b", decl))


def present(declaration, existing):
    first = declaration.strip().split("\n")[0]
    key = member_key(first)
    if key:
        return any(member_key(line) == key for line in existing.splitlines())
    m = re.search(r"\b(class|object|val|var)\s+(\w+)", first)
    if "companion object" in first:
        return re.search(r"\bcompanion object\b", existing) is not None
    return bool(m) and re.search(rf"\b{m.group(1)}\s+{m.group(2)}\b", existing) is not None


def member_key(line):
    m = re.match(r"\s*(?:(?:actual|expect|suspend|operator|infix|inline|override|open)\s+)*fun\s+(?:<[^>]*>\s*)?(?:[\w.<>?, ]+\.)?(\w+)\(", line)
    if not m:
        return None
    try:
        close = find_close(line, m.end() - 1)
    except ValueError:
        return None
    params = []
    for p in split_depth0(strip_defaults(line[m.end():close])):
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


def slice_top_level(rel):
    path = ANDROID / ROOT / rel
    text = path.read_text()
    pkg = re.search(r"^package ([\w.]+)", text, re.M).group(1)
    imports = re.findall(r"^import ([\w.]+)(?: as (\w+))?$", text, re.M)
    imported = {alias or name.rsplit(".", 1)[1]: name for name, alias in imports}
    stem = Path(rel).stem
    expect_path = (COMMON / ROOT / rel).parent / f"{stem}.expect.kt"
    previous = {}
    if expect_path.exists():
        for line in expect_path.read_text().splitlines():
            key = member_key(line)
            if key:
                previous[key] = line
    expect_lines, desktop_lines, edits, skipped = [], [], [], []
    for m in re.finditer(r"^((?:@\w+(?:\([^)]*\))?\s+)*)((?:public |suspend |inline |actual )*)fun\s+(?:<[^>]*>\s*)?((?:[\w.<>?, ]+\.)?)(\w+)\(", text, re.M):
        mods, receiver, name = m.group(2), m.group(3), m.group(4)
        open_paren = m.end() - 1
        close = find_close(text, open_paren)
        end = signature_end(text, m.start(2), "fun")
        tail = " ".join(text[close + 1:end].split())
        rtype = tail[1:].strip() if tail.startswith(":") else ("Unit" if text[end] == "{" else "")
        params = text[open_paren + 1:close]
        sig = f"{receiver}{name}({params}) {rtype}"
        words = set(re.findall(r"\b[A-Z]\w*", sig))
        blocked = sorted(w for w in words if w in imported and not portable_import(imported[w]))
        if not rtype or blocked:
            skipped.append(f"{name}: {', '.join(blocked) or 'expression body without a declared return type'}")
            continue
        suspend = "suspend " if "suspend" in mods else ""
        returns = "" if rtype == "Unit" else f": {rtype}"
        line_text = f"{suspend}fun {receiver}{name}({' '.join(params.split())}){returns}"
        if "actual" in mods and member_key(line_text) in previous:
            line_text = re.sub(r"^expect\s+", "", previous[member_key(line_text)])
            params = line_text[line_text.index("(") + 1:find_close(line_text, line_text.index("("))]
        expect_lines.append("expect " + line_text)
        value = neutral(rtype) if rtype != "Unit" else None
        body = " {\n}" if rtype == "Unit" else f" = {value or 'TODO(\"desktop: ' + name + '\")'}"
        desktop_lines.append(f"actual {suspend}fun {receiver}{name}({' '.join(strip_defaults(params).split())}){returns}{body}")
        edits.append((open_paren + 1, close, strip_defaults(text[open_paren + 1:close])))
        edits.append((m.start(2), m.start(2), "" if "actual" in mods else "actual "))
    for a, b, rep in sorted(edits, key=lambda e: e[0], reverse=True):
        text = text[:a] + rep + text[b:]
    path.write_text(text)
    body_text = "\n".join(expect_lines)
    needed = []
    for name, alias in imports:
        short = alias or name.rsplit(".", 1)[1]
        if re.search(rf"\b{short}\b", body_text) and portable_import(name):
            needed.append(f"import {name}" + (f" as {alias}" if alias else ""))
    header = "\n".join(needed)
    for pattern, replacement in rewrite_imports.RULES:
        header = pattern.sub(replacement, header)
    expect_path.parent.mkdir(parents=True, exist_ok=True)
    expect_path.write_text(f"package {pkg}\n\n" + (header + "\n\n" if header else "") + "\n\n".join(expect_lines) + "\n")
    desktop_path = (DESKTOP / ROOT / rel).parent / f"{stem}.desktop.kt"
    if not desktop_path.exists():
        desktop_path.parent.mkdir(parents=True, exist_ok=True)
        desktop_path.write_text(f"package {pkg}\n\n" + (header + "\n\n" if header else "") + "\n\n".join(desktop_lines) + "\n")
    print(f"{rel}: {len(expect_lines)} top-level functions in the expect slice")
    for s in skipped:
        print(f"  left out {s}")


def main():
    if not MANIFEST.exists():
        return
    for raw in MANIFEST.read_text().splitlines():
        line = raw.split("#")[0].strip()
        if not line:
            continue
        rel, spec = (s.strip() for s in line.split(":", 1))
        if spec == "*":
            slice_top_level(rel)
            continue
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
        interface_class = re.search(rf"\binterface {cls}\b", text[cls_match.start():cls_match.end()]) is not None
        interface_defaults = set(re.findall(r"^    (?:val|var|fun|suspend fun)\s+(\w+)[^\n]*(?:get\(\)|\)\s*(?::[^\n{=]+)?[{=])", text[body_start:body_end], re.M))
        supertypes = []
        ctor_params = None
        ctor_edit = None
        ctor_m = re.match(r"\s*(?:actual\s+constructor\s*)?\(", text[cls_match.end():])
        if ctor_m and "@Inject" not in text[cls_match.end():cls_match.end() + 40] and kind_word_guess(text, cls_match, cls) == "class":
            open_at = cls_match.end() + ctor_m.end() - 1
            raw_params = text[open_at + 1:find_close(text, open_at)]
            plain = re.sub(r"\b(?:private|internal|protected|public)?\s*(?:val|var)\s+", "", raw_params)
            ctor_words = set(re.findall(r"\b[A-Z]\w*", plain))
            if not any((w in imported and not portable_import(imported[w])) or (w in top_level and w != cls) for w in ctor_words):
                ctor_params = " ".join(plain.split()).rstrip(",")
                if "actual constructor" not in text[cls_match.end():open_at]:
                    ctor_edit = (open_at, open_at, " actual constructor")
        header_text = text[cls_match.end():body_start]
        while re.search(r"\([^()]*\)", header_text):
            header_text = re.sub(r"\([^()]*\)", "", header_text)
        header_m = re.search(r":\s*([^{]+)$", header_text.strip())
        if header_m:
            for st in split_depth0(header_m.group(1)):
                st_m = re.match(r"\s*([\w.]+)", st)
                if not st_m:
                    continue
                st_name = st_m.group(1)
                if (COMMON / ROOT / rel).parent.joinpath(f"{st_name}.expect.kt").exists() or st_name in STDLIB_SUPERTYPES:
                    supertypes.append(st_name)
        nested = set(re.findall(r"^    (?:@\w+\s+)*(?:(?:data|sealed|abstract|open|inner|value)\s+)*(?:class|interface|object)\s+(\w+)", text[body_start:body_end], re.M))
        nested -= set(re.findall(r"^    (?:\w+\s+)*enum\s+class\s+(\w+)", text[body_start:body_end], re.M))
        constants = dict(re.findall(r"\bconst\s+val\s+(\w+)\s*(?::\s*\w+)?\s*=\s*([^\n]+)", text))
        expect_lines, desktop_lines, edits, skipped = [], [], [], []
        if ctor_edit:
            edits.append(ctor_edit)
        found = list(members(text, body_start, body_end))
        found.sort(key=lambda sm: 0 if sm[1].group(2) in ("class", "object") else 1)
        for start, m in found:
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
            if kind == "object" and re.search(r"\bcompanion\b", mods):
                inner_expect, inner_desktop = [], []
                brace = text.find("{", start + m.end(2))
                newline = text.find("\n", start + m.end(2))
                if brace != -1 and (newline == -1 or brace < newline):
                    comp_end = find_close(text, brace)
                    for cstart, cm in list(members(text, brace, comp_end)):
                        cmods, ckind = cm.group(1), cm.group(2)
                        if re.search(r"\b(private|protected|internal|override)\b", cmods):
                            continue
                        if ckind == "val" and re.search(r"\bconst\b", cmods):
                            km = re.match(r"val\s+(\w+)\s*(?::\s*(\w+))?\s*=\s*([^\n]+)", text[cstart + cm.start(2):].split("\n", 1)[0])
                            literal = km.group(3).strip() if km else ""
                            ctype = (km.group(2) if km and km.group(2) else
                                     "String" if literal.startswith('"') else "Long" if re.fullmatch(r"-?\d+L", literal)
                                     else "Int" if re.fullmatch(r"-?\d+", literal) else "Boolean" if literal in ("true", "false") else "")
                            if not km or not ctype or not re.fullmatch(r'"[^"]*"|-?\d+L?|true|false', literal):
                                skipped.append(f"Companion.{km.group(1) if km else '?'}: not a literal constant")
                                continue
                            inner_expect.append(f"        val {km.group(1)}: {ctype}")
                            inner_desktop.append(f"        actual const val {km.group(1)}: {ctype} = {literal}")
                            edits.append((cstart + cm.start(1), cstart + cm.start(1), "" if "actual" in cmods else "actual "))
                            continue
                        if ckind != "fun":
                            continue
                        cdecl = cstart + cm.start(2)
                        cend = signature_end(text, cdecl, ckind)
                        csig = " ".join(text[cdecl:cend].split())
                        cname = re.match(r"fun\s+(?:<[^>]*>\s*)?(\w+)", csig).group(1)
                        po = csig.index("(")
                        pc = find_close(csig, po)
                        cparams, crt = csig[po + 1:pc], csig[pc + 1:].strip()
                        crt = crt[1:].strip() if crt.startswith(":") else ("Unit" if text[cend] == "{" else "")
                        cwords = set(re.findall(r"\b[A-Z]\w*", csig))
                        cblocked = sorted(w for w in cwords if (w in imported and not portable_import(imported[w])) or w in nested)
                        if not crt or cblocked:
                            skipped.append(f"Companion.{cname}: {', '.join(cblocked) or 'no declared return type'}")
                            continue
                        csusp = "suspend " if re.search(r"\bsuspend\b", cmods) else ""
                        creturns = "" if crt == "Unit" else f": {crt}"
                        cline = f"        {csusp}fun {cname}({cparams.strip()}){creturns}"
                        if "actual" in cmods and member_key(cline) in previous:
                            cline = previous[member_key(cline)]
                            cparams = cline[cline.index("(") + 1:find_close(cline, cline.index("("))]
                        inner_expect.append(cline)
                        cvalue = neutral(crt) if crt != "Unit" else None
                        cbody = " {\n        }" if crt == "Unit" else f" = {cvalue or 'TODO(\"desktop: ' + cname + '\")'}"
                        inner_desktop.append(f"        actual {csusp}fun {cname}({strip_defaults(cparams).strip()}){creturns}{cbody}")
                        cpo = text.index("(", cdecl)
                        edits.append((cpo + 1, find_close(text, cpo), strip_defaults(text[cpo + 1:find_close(text, cpo)])))
                        edits.append((cstart + cm.start(1), cstart + cm.start(1), "" if "actual" in cmods else "actual "))
                if inner_expect:
                    expect_lines.append("    companion object {\n" + "\n\n".join(inner_expect) + "\n    }")
                    desktop_lines.append("    actual companion object {\n" + "\n\n".join(inner_desktop) + "\n    }")
                else:
                    expect_lines.append("    companion object {}")
                    desktop_lines.append("    actual companion object {}")
                edits.append((start + m.start(1), start + m.start(1), "" if "actual" in mods else "actual "))
                continue
            if kind == "class" and re.search(r"\bdata\b", mods):
                decl_start = start + m.start(2)
                open_paren = text.index("(", decl_start)
                close_paren = find_close(text, open_paren)
                head = " ".join(text[decl_start:open_paren].split())
                nested_name = head.split()[-1]
                after = text[close_paren + 1:close_paren + 3]
                body_open = close_paren + 1 + (len(after) - len(after.lstrip())) if after.lstrip().startswith("{") else None
                body_close = find_close(text, body_open) if body_open is not None else close_paren
                props, prop_edits = [], []
                offset = open_paren + 1
                for param in split_depth0(text[open_paren + 1:close_paren]):
                    pm = re.match(r"\s*((?:@\w+\s+)*)(private\s+|internal\s+)?(actual\s+)?(val|var)\s+(\w+)\s*:\s*(.+?)\s*(?:=.*)?$", param, re.S)
                    if pm and not pm.group(2):
                        props.append((pm.group(4), pm.group(5), " ".join(pm.group(6).split())))
                        if not pm.group(3):
                            prop_edits.append(offset + pm.start(4))
                    offset += len(param) + 1
                if body_open is not None:
                    for bm in re.finditer(r"(?m)^        (actual\s+)?(val)\s+(\w+)\s*:\s*(\S[^=\n]*?)\s*(?:get\(\)|=|$)", text[body_open:body_close]):
                        props.append(("val", bm.group(3), bm.group(4).strip()))
                        if not bm.group(1):
                            prop_edits.append(body_open + bm.start(2))
                words = set(re.findall(r"\b[A-Z]\w*", " ".join(t for _, _, t in props)))
                blocked = sorted(
                    w for w in words
                    if (w in imported and not portable_import(imported[w])) or (w in top_level and w != cls)
                    or (w in nested and w != nested_name)
                )
                if blocked:
                    skipped.append(f"{nested_name}: {', '.join(blocked)} not portable or declared in this file")
                    continue
                nested.discard(nested_name)
                expect_lines.append(f"    class {nested_name} {{\n" + "\n".join(f"        val {n}: {t}" for _, n, t in props) + "\n    }")
                desktop_lines.append(f"__NESTED__:{nested_name}")
                edits.append((start + m.start(1), start + m.start(1), "" if "actual" in mods else "actual "))
                edits += [(pos, pos, "actual ") for pos in prop_edits]
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
                rtype, params = re.sub(r"\s+get\(\).*$", "", tm.group(1).strip()), ""
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
            if interface_class and name in interface_defaults:
                skipped.append(f"{name}: has a default body in the Android interface (expect members are abstract)")
                continue
            operator = "operator " if re.search(r"\boperator\b", mods) else ""
            suspend = operator + suspend
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
        kind_word = next(w for w in ("object", "interface", "class") if re.search(rf"\b{w} {cls}\b", cls_decl))
        if not re.search(r"\bactual\b", cls_decl):
            edits.append((cls_match.end() - len(f"{kind_word} {cls}"), cls_match.end() - len(f"{kind_word} {cls}"), "actual "))
        for a, b, rep in sorted(edits, key=lambda e: e[0], reverse=True):
            text = text[:a] + rep + text[b:]
        path.write_text(text)
        for k, line_text in enumerate(desktop_lines):
            if line_text.startswith("__NESTED__:"):
                nested_name = line_text.split(":", 1)[1]
                nm = re.search(rf"^    actual data class {nested_name}\(", text, re.M)
                close_paren = find_close(text, nm.end() - 1)
                after = text[close_paren + 1:close_paren + 3]
                end = find_close(text, close_paren + 1 + (len(after) - len(after.lstrip()))) if after.lstrip().startswith("{") else close_paren
                copied = text[nm.start():end + 1]
                private_names = set(re.findall(r"\bprivate\s+(?:const\s+)?(?:val|var|fun)\s+(\w+)", text))

                def neutral_getter(gm):
                    expr_words = set(re.findall(r"\b[A-Za-z_]\w*", gm.group(3)))
                    bad = any((w in imported and not portable_import(imported[w])) or w in private_names for w in expr_words)
                    if not bad:
                        return gm.group(0)
                    return f"{gm.group(1)}get() = {neutral(gm.group(2).strip()) or 'TODO(\"desktop\")'}"
                copied = re.sub(r"(actual val \w+: ([^\n=]+?)\s*\n?\s*)get\(\)\s*=\s*([^\n]+)", neutral_getter, copied)
                desktop_lines[k] = copied
        body_text = "\n".join(expect_lines + desktop_lines + [ctor_params or ""] + supertypes)
        needed = []
        for name, alias in imports:
            short = alias or name.rsplit(".", 1)[1]
            if re.search(rf"\b{short}\b", body_text) and portable_import(name):
                needed.append(f"import {name}" + (f" as {alias}" if alias else ""))
        header = "\n".join(needed)
        for pattern, replacement in rewrite_imports.RULES:
            header = pattern.sub(replacement, header)
        out_dir = (COMMON / ROOT / rel).parent
        supers = f" : {', '.join(supertypes)}" if supertypes else ""
        if any(st in ("AutoCloseable", "Closeable", "java.io.Closeable") for st in supertypes):
            if not any(re.match(r"\s*(?:actual |override )*fun close\(", l) for l in desktop_lines):
                desktop_lines.append("    override fun close() {\n    }")
        if supertypes:
            for st_name in [st for st in supertypes if st not in STDLIB_SUPERTYPES]:
                st_text = (COMMON / ROOT / rel).parent.joinpath(f"{st_name}.expect.kt").read_text()
                st_android = (ANDROID / ROOT / rel).parent.joinpath(f"{st_name}.kt").read_text()
                abstract = re.findall(r"^    (?:actual )?val (\w+): (\S+)\s*$", st_android, re.M)
                for member_name, member_type in abstract:
                    literal = re.search(rf"^    (?:actual )?override val {member_name}\s*(?::\s*\S+)?\s*=\s*(\"[^\"]*\"|\d+L?|true|false)\s*$", text, re.M)
                    value = literal.group(1) if literal else (neutral(member_type) or f'TODO("desktop: {member_name}")')
                    desktop_lines.append(f"    override val {member_name}: {member_type} = {value}")
        ctor_expect = f"({ctor_params})" if ctor_params is not None else ""
        expect_text = f"package {pkg}\n\n" + (header + "\n\n" if header else "") + f"expect {kind_word} {cls}{ctor_expect}{supers} {{\n" + "\n\n".join(expect_lines) + "\n}\n"
        out_dir.mkdir(parents=True, exist_ok=True)
        (out_dir / f"{cls}.expect.kt").write_text(expect_text)
        desktop_path = (DESKTOP / ROOT / rel).parent / f"{cls}.desktop.kt"
        if desktop_path.exists():
            existing = desktop_path.read_text()
            for d in desktop_lines:
                if not d.strip().startswith("actual companion object {\n"):
                    continue
                body = re.search(r"^    actual companion object(?: \{\})?", existing, re.M)
                if not body:
                    continue
                inner = [l for l in d.split("\n")[1:-1] if l.strip()]
                wanted_inner = [l for l in "\n".join(inner).split("\n\n") if l.strip()]
                block_end = existing.find("\n    }", body.end()) if "{}" not in body.group(0) and existing[body.end():body.end() + 2].strip().startswith("{") else -1
                present_text = existing[body.start():block_end] if block_end != -1 else ""
                missing_inner = [w for w in d.split("\n")[1:-1] if w.strip() and member_key(w) and not any(member_key(l) == member_key(w) for l in present_text.splitlines())]
                if not missing_inner:
                    continue
                if block_end == -1:
                    existing = existing[:body.start()] + "    actual companion object {\n" + "\n".join(missing_inner) + "\n    }" + existing[body.end():]
                else:
                    existing = existing[:block_end] + "\n" + "\n".join(missing_inner) + existing[block_end:]
                desktop_path.write_text(existing)
            missing = [d for d in desktop_lines if not present(d, existing)]
            if missing:
                cut = existing.rstrip().rfind("}")
                existing = existing[:cut].rstrip() + "\n\n" + "\n\n".join(missing) + "\n}\n"
                wanted = "\n".join(needed)
                for pattern, replacement in rewrite_imports.RULES:
                    wanted = pattern.sub(replacement, wanted)
                add = [l for l in wanted.splitlines() if l and not re.search(rf"^{re.escape(l)}$", existing, re.M)]
                if add:
                    last = list(re.finditer(r"^(?:import|package) .*$", existing, re.M))[-1]
                    existing = existing[:last.end()] + "\n" + "\n".join(add) + existing[last.end():]
                desktop_path.write_text(existing)
                print(f"  {len(missing)} members added to {desktop_path.name}")
        else:
            desktop_path.parent.mkdir(parents=True, exist_ok=True)
            singleton = "@Singleton\n" if re.search(r"@Singleton", text[max(0, cls_match.start() - 200):cls_match.end()]) else ""
            desktop_header = "\n".join(sorted(set(needed + ["import javax.inject.Inject"] + (["import javax.inject.Singleton"] if singleton else []))))
            for pattern, replacement in rewrite_imports.RULES:
                desktop_header = pattern.sub(replacement, desktop_header)
            constructor = " @Inject constructor()" if kind_word == "class" else ""
            if ctor_params is not None:
                constructor = f" actual constructor({strip_defaults(ctor_params)})"
            if kind_word == "interface":
                cleaned = []
                for l in desktop_lines:
                    vm = re.match(r"(\s*actual (?:val|var) (\w+): .+?) = (.*)$", l)
                    if vm:
                        cleaned.append(f"{vm.group(1)} get() = {vm.group(3)}" if vm.group(2) in interface_defaults else vm.group(1))
                        continue
                    fm = member_key(l)
                    if fm and fm[0] not in interface_defaults:
                        l = re.sub(r"\s*(?:\{\s*\}|= .*)$", "", l, flags=re.S)
                    cleaned.append(l)
                desktop_lines = cleaned
            if kind_word == "object":
                singleton = ""
                desktop_header = "\n".join(l for l in desktop_header.splitlines() if l not in ("import javax.inject.Inject", "import javax.inject.Singleton"))
            desktop_path.write_text(
                f"package {pkg}\n\n{desktop_header}\n\n{singleton}actual {kind_word} {cls}{constructor}{supers} {{\n"
                + "\n\n".join(desktop_lines) + "\n}\n"
            )
        print(f"{rel}: {cls}: {len(expect_lines)} members in the expect slice")
        for s in skipped:
            print(f"  left out {s}")


if __name__ == "__main__":
    main()
