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
    (re.compile(r"^import com\.nendo\.argosy\.R$", re.M), "import com.nendo.argosy.res.ArgosyR as R"),
    (re.compile(r"\bcom\.nendo\.argosy\.R\."), "com.nendo.argosy.res.ArgosyR."),
    (
        re.compile(r"^import androidx\.compose\.ui\.res\.(stringResource|pluralStringResource|painterResource)$", re.M),
        r"import com.nendo.argosy.res.\1",
    ),
    (
        re.compile(r"^import android\.content\.Context$", re.M),
        "import com.nendo.argosy.platform.PlatformContext as Context\nimport com.nendo.argosy.platform.cacheDir\nimport com.nendo.argosy.platform.filesDir\nimport com.nendo.argosy.platform.getQuantityString\nimport com.nendo.argosy.platform.getString\nimport com.nendo.argosy.platform.resources",
    ),
    (
        re.compile(r"^import androidx\.compose\.ui\.platform\.LocalContext$", re.M),
        "import com.nendo.argosy.platform.LocalPlatformContext as LocalContext\nimport com.nendo.argosy.platform.cacheDir\nimport com.nendo.argosy.platform.filesDir\nimport com.nendo.argosy.platform.getQuantityString\nimport com.nendo.argosy.platform.getString\nimport com.nendo.argosy.platform.resources",
    ),
    (
        re.compile(r"^import androidx\.hilt\.navigation\.compose\.hiltViewModel$", re.M),
        "import com.nendo.argosy.di.hiltViewModel",
    ),
    (
        re.compile(r"^import androidx\.compose\.ui\.platform\.LocalConfiguration$", re.M),
        "import com.nendo.argosy.platform.LocalPlatformConfiguration as LocalConfiguration\n"
        "import com.nendo.argosy.platform.orientation\n"
        "import com.nendo.argosy.platform.screenHeightDp\n"
        "import com.nendo.argosy.platform.screenWidthDp",
    ),
    (
        re.compile(r"^import androidx\.activity\.compose\.BackHandler$", re.M),
        "import com.nendo.argosy.platform.BackHandler",
    ),
    (
        re.compile(r"^import android\.content\.res\.Configuration$", re.M),
        "import com.nendo.argosy.platform.PlatformConfiguration as Configuration",
    ),
    (
        re.compile(r"^import androidx\.compose\.ui\.platform\.LocalView$", re.M),
        "import com.nendo.argosy.platform.LocalPlatformView as LocalView\nimport com.nendo.argosy.platform.getLocationOnScreen",
    ),
    (re.compile(r"^import android\.os\.Build$", re.M), "import com.nendo.argosy.platform.PlatformBuild as Build"),
    (re.compile(r"\bandroid\.os\.Build\."), "com.nendo.argosy.platform.PlatformBuild."),
    (re.compile(r"^import android\.graphics\.Color$", re.M), "import com.nendo.argosy.platform.PlatformColor as Color"),
    (re.compile(r"^import android\.graphics\.Color as (\w+)$", re.M), r"import com.nendo.argosy.platform.PlatformColor as \1"),
    (re.compile(r"\bandroid\.graphics\.Color\."), "com.nendo.argosy.platform.PlatformColor."),
    (
        re.compile(r"^import androidx\.core\.graphics\.ColorUtils$", re.M),
        "import com.nendo.argosy.platform.PlatformColorUtils as ColorUtils",
    ),
    (
        re.compile(r"^import androidx\.compose\.ui\.platform\.LocalLifecycleOwner$", re.M),
        "import androidx.lifecycle.compose.LocalLifecycleOwner",
    ),
    (
        re.compile(r"^import dagger\.hilt\.android\.qualifiers\.ApplicationContext$", re.M),
        "import com.nendo.argosy.di.ApplicationContext",
    ),
    (re.compile(r"^import com\.nendo\.argosy\.BuildConfig$", re.M), "import com.nendo.argosy.config.AppBuildConfig as BuildConfig"),
    (re.compile(r"\bcom\.nendo\.argosy\.BuildConfig\."), "com.nendo.argosy.config.AppBuildConfig."),
    (re.compile(r"^import dagger\.hilt\.android\.lifecycle\.HiltViewModel$", re.M), "import com.nendo.argosy.di.HiltViewModel"),
    (re.compile(r"^import android\.view\.KeyEvent$", re.M), "import com.nendo.argosy.core.input.AndroidKeyCodes as KeyEvent"),
    (
        re.compile(r"^import com\.nendo\.argosy\.util\.DisplayAffinityHelper$", re.M),
        "import com.nendo.argosy.util.DualScreenDeviceInfo as DisplayAffinityHelper",
    ),
]


COMPANION_IMPORTS = {
    "com.nendo.argosy.platform.PlatformContext": [
        "com.nendo.argosy.platform.cacheDir", "com.nendo.argosy.platform.filesDir",
        "com.nendo.argosy.platform.getQuantityString", "com.nendo.argosy.platform.getString",
        "com.nendo.argosy.platform.resources",
    ],
    "com.nendo.argosy.platform.LocalPlatformContext": [
        "com.nendo.argosy.platform.cacheDir", "com.nendo.argosy.platform.filesDir",
        "com.nendo.argosy.platform.getQuantityString", "com.nendo.argosy.platform.getString",
        "com.nendo.argosy.platform.resources",
    ],
    "com.nendo.argosy.platform.LocalPlatformView": ["com.nendo.argosy.platform.getLocationOnScreen"],
    "com.nendo.argosy.platform.LocalPlatformConfiguration": [
        "com.nendo.argosy.platform.orientation", "com.nendo.argosy.platform.screenHeightDp",
        "com.nendo.argosy.platform.screenWidthDp",
    ],
}


def ensure_companions(text):
    for stand_in, companions in COMPANION_IMPORTS.items():
        anchor = re.search(rf"^import {re.escape(stand_in)}( as \w+)?$", text, re.M)
        if not anchor:
            continue
        missing = [c for c in companions if not re.search(rf"^import {re.escape(c)}$", text, re.M)]
        if missing:
            text = text[:anchor.end()] + "".join(f"\nimport {c}" for c in missing) + text[anchor.end():]
    return text


def main():
    changed = 0
    for path in sorted(COMMON.rglob("*.kt")):
        text = path.read_text()
        new = text
        for pattern, replacement in RULES:
            new = pattern.sub(replacement, new)
        new = ensure_companions(new)
        if new != text:
            path.write_text(new)
            changed += 1
    print(f"rewrote imports in {changed} files")


if __name__ == "__main__":
    main()
