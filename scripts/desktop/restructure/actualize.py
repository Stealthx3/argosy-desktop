#!/usr/bin/env python3
"""
Adds `actual` modifiers to Android classes that implement an expect class (actuals.txt).

Only the class declaration and the listed members change (<init> is the primary constructor; a
member written ::name is a top-level
function in the same file, extension receivers included; a line may list only ::names, without a
class); everything else in the file stays as upstream wrote it. Idempotent.
"""
import re
from pathlib import Path

ANDROID = Path("shared/src/androidMain/kotlin/com/nendo/argosy")
MANIFEST = Path(__file__).with_name("actuals.txt")


def main():
    for raw in MANIFEST.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        rel, spec = (s.strip() for s in line.split(":", 1))
        match = re.match(r"(\w+)\((.*)\)", spec)
        cls, members = match.groups() if match else (None, spec)
        path = ANDROID / rel
        text = path.read_text()
        new = text if cls is None else re.sub(
            rf"^((?:@\w+(?:\([^)]*\))?\s+)*)((?:public |internal |open |data )*)(?<!actual )(class|object) {cls}\b",
            rf"\1\2actual \3 {cls}",
            text,
            count=1,
            flags=re.M,
        )
        for member in filter(None, (m.strip() for m in members.split(","))):
            if member == "<init>":
                new = re.sub(rf"\b(class {cls})\s*\(", r"\1 actual constructor(", new, count=1)
                continue
            top_level = member.startswith("::")
            name = member.lstrip(":")
            indent = r"()" if top_level else r"(\s+)"
            new = re.sub(
                rf"^{indent}((?:public |override |open |suspend )*)(?<!actual )fun ((?:[\w.<>, ]+\.)?){name}\(",
                rf"\1\2actual fun \3{name}(",
                new,
                flags=re.M,
            )
            new = re.sub(
                rf"^{indent}((?:public |override |open )*)(?<!actual )(val|var) {name}\b",
                rf"\1\2actual \3 {name}",
                new,
                flags=re.M,
            )
        if new != text:
            path.write_text(new)
            print(f"{rel}: {cls or spec} marked actual")


if __name__ == "__main__":
    main()
