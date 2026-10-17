#!/usr/bin/env python3
"""
Adds `actual` modifiers to Android classes that implement an expect class (actuals.txt).

Only the class declaration and the listed members change; everything else in the file stays as
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
            rf"^((?:@\w+(?:\([^)]*\))?\s+)*)((?:public |internal |open )*)(?<!actual )class {cls}\b",
            rf"\1\2actual class {cls}",
            text,
            count=1,
            flags=re.M,
        )
        for member in filter(None, (m.strip() for m in members.split(","))):
            new = re.sub(
                rf"^(\s+)((?:public |override |open |suspend )*)(?<!actual )fun {member}\(",
                rf"\1\2actual fun {member}(",
                new,
                flags=re.M,
            )
        if new != text:
            path.write_text(new)
            print(f"{rel}: {cls} marked actual")


if __name__ == "__main__":
    main()
