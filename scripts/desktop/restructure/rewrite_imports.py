#!/usr/bin/env python3
"""
Rewrites Android imports in shared commonMain to their portable stand-ins.

Import lines change, with an alias that keeps call sites as upstream wrote them; a fully qualified
android.util.Log call becomes a fully qualified PlatformLog call.
Idempotent. Run from the repository root after apply_moves.py.
"""
import re
from pathlib import Path

COMMON = Path("shared/src/commonMain/kotlin")
RULES = [
    (re.compile(r"^import android\.util\.Log$", re.M), "import com.nendo.argosy.util.PlatformLog as Log"),
    (re.compile(r"^import android\.util\.Log as (\w+)$", re.M), r"import com.nendo.argosy.util.PlatformLog as \1"),
    (re.compile(r"\bandroid\.util\.Log\."), "com.nendo.argosy.util.PlatformLog."),
]


def main():
    changed = 0
    for path in sorted(COMMON.rglob("*.kt")):
        text = path.read_text()
        new = text
        for pattern, replacement in RULES:
            new = pattern.sub(replacement, new)
        if new != text:
            path.write_text(new)
            changed += 1
    print(f"rewrote imports in {changed} files")


if __name__ == "__main__":
    main()
