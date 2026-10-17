#!/usr/bin/env python3
"""
Adds `actual` modifiers to Android classes that implement an expect class (actuals.txt).

Only the class declaration and the listed members change (a member written ::name is a top-level
function in the same file); everything else in the file stays as
upstream wrote it. Idempotent.
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
        cls, members = re.match(r"(\w+)\((.*)\)", spec).groups()
        path = ANDROID / rel
        text = path.read_text()
        new = re.sub(
            rf"^((?:@\w+(?:\([^)]*\))?\s+)*)((?:public |internal |open )*)(?<!actual )(class|object) {cls}\b",
            rf"\1\2actual \3 {cls}",
            text,
            count=1,
            flags=re.M,
        )
        for member in filter(None, (m.strip() for m in members.split(","))):
            top_level = member.startswith("::")
            name = member.lstrip(":")
            indent = r"()" if top_level else r"(\s+)"
            new = re.sub(
                rf"^{indent}((?:public |override |open |suspend )*)(?<!actual )fun {name}\(",
                rf"\1\2actual fun {name}(",
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
            print(f"{rel}: {cls} marked actual")


if __name__ == "__main__":
    main()
