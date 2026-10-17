#!/usr/bin/env python3
"""
Rewrites Android imports in shared commonMain to their portable stand-ins, and app's BuildConfig
and R in shared androidMain to their shared equivalents.

Import lines change, with an alias that keeps call sites as upstream wrote them; a fully qualified
android.util.Log call becomes a fully qualified PlatformLog call.
Idempotent. Run from the repository root after apply_moves.py.
"""
import re
from pathlib import Path

COMMON = Path("shared/src/commonMain/kotlin")
ANDROID = Path("shared/src/androidMain/kotlin")
ANDROID_RULES = [
    (re.compile(r"^import com\.nendo\.argosy\.BuildConfig$", re.M), "import com.nendo.argosy.config.AppBuildConfig as BuildConfig"),
    (re.compile(r"\bcom\.nendo\.argosy\.BuildConfig\."), "com.nendo.argosy.config.AppBuildConfig."),
    (re.compile(r"^import com\.nendo\.argosy\.R$", re.M), "import com.nendo.argosy.shared.R"),
    (re.compile(r"\bcom\.nendo\.argosy\.R\."), "com.nendo.argosy.shared.R."),
]
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
    (re.compile(r"^import coil\.compose\.AsyncImage$", re.M), "import com.nendo.argosy.platform.image.AsyncImage"),
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
    (re.compile(r"^import android\.os\.SystemClock$", re.M), "import com.nendo.argosy.platform.PlatformSystemClock as SystemClock"),
    (re.compile(r"(?<!import )\bandroid\.os\.SystemClock\b"), "com.nendo.argosy.platform.PlatformSystemClock"),
    (re.compile(r"(?<!import )\bandroid\.content\.Context\b"), "com.nendo.argosy.platform.PlatformContext"),
    (
        re.compile(r"@dagger\.hilt\.android\.qualifiers\.ApplicationContext\b"),
        "@com.nendo.argosy.di.ApplicationContext",
    ),
    (re.compile(r"^import android\.os\.Environment$", re.M), "import com.nendo.argosy.platform.PlatformEnvironment as Environment"),
    (re.compile(r"\bandroid\.os\.Environment\."), "com.nendo.argosy.platform.PlatformEnvironment."),
    (re.compile(r"^import android\.os\.StatFs$", re.M), "import com.nendo.argosy.platform.PlatformStatFs as StatFs"),
    (re.compile(r"\bandroid\.os\.StatFs\("), "com.nendo.argosy.platform.PlatformStatFs("),
    (re.compile(r"^import android\.text\.format\.DateFormat$", re.M), "import com.nendo.argosy.platform.PlatformDateFormat as DateFormat"),
    (
        re.compile(r"\bandroid\.text\.format\.DateFormat\.getBestDateTimePattern\b"),
        "com.nendo.argosy.platform.PlatformDateFormat.getBestDateTimePattern",
    ),
    (re.compile(r"(?<!import )\bandroid\.graphics\.Bitmap\.Config\b"), "com.nendo.argosy.platform.PlatformBitmapConfig"),
    (re.compile(r"^import android\.graphics\.Bitmap$", re.M), "import com.nendo.argosy.platform.PlatformBitmap as Bitmap"),
    (re.compile(r"(?<!import )\bandroid\.graphics\.Bitmap\b(?!Factory)"), "com.nendo.argosy.platform.PlatformBitmap"),
    (re.compile(r"^import android\.graphics\.BitmapFactory$", re.M), "import com.nendo.argosy.platform.PlatformBitmapFactory as BitmapFactory"),
    (re.compile(r"(?<!import )\bandroid\.graphics\.BitmapFactory\b"), "com.nendo.argosy.platform.PlatformBitmapFactory"),
    (re.compile(r"^import androidx\.compose\.ui\.graphics\.asImageBitmap$", re.M), "import com.nendo.argosy.platform.asImageBitmap"),
    (re.compile(r"^import android\.util\.Base64$", re.M), "import com.nendo.argosy.platform.PlatformBase64 as Base64"),
    (re.compile(r"\bandroid\.util\.Base64\."), "com.nendo.argosy.platform.PlatformBase64."),
    (
        re.compile(r"^import android\.content\.SharedPreferences$", re.M),
        "import com.nendo.argosy.platform.PlatformSharedPreferences as SharedPreferences",
    ),
    (re.compile(r"^import android\.provider\.Settings$", re.M), "import com.nendo.argosy.platform.PlatformSettings as Settings"),
    (re.compile(r"\bandroid\.provider\.Settings\.Secure\."), "com.nendo.argosy.platform.PlatformSettings.Secure."),
    (re.compile(r"^import android\.content\.Intent$", re.M), "import com.nendo.argosy.platform.PlatformIntent as Intent"),
    (re.compile(r"\bandroid\.net\.Uri\.(parse|fromFile|encode|decode)\("), r"com.nendo.argosy.platform.PlatformUris.\1("),
    (re.compile(r"^import android\.net\.Uri$", re.M), "import com.nendo.argosy.platform.PlatformUri as Uri"),
    (re.compile(r"(?<!import )\bandroid\.net\.Uri\b"), "com.nendo.argosy.platform.PlatformUri"),
    (re.compile(r"^import android\.os\.Process$", re.M), "import com.nendo.argosy.platform.PlatformProcess as Process"),
    (re.compile(r"\bandroid\.os\.Process\.(is64Bit|myUid)\b"), r"com.nendo.argosy.platform.PlatformProcess.\1"),
    (re.compile(r"^import android\.view\.KeyEvent$", re.M), "import com.nendo.argosy.core.input.AndroidKeyCodes as KeyEvent"),
    (
        re.compile(r"^import com\.nendo\.argosy\.util\.DisplayAffinityHelper$", re.M),
        "import com.nendo.argosy.util.DualScreenDeviceInfo as DisplayAffinityHelper",
    ),
]


COMPANION_IMPORTS = {
    "com.nendo.argosy.platform.PlatformContext": [
        "com.nendo.argosy.platform.applicationContext", "com.nendo.argosy.platform.packageName",
        "com.nendo.argosy.platform.assets", "com.nendo.argosy.platform.contentResolver", "com.nendo.argosy.platform.cacheDir", "com.nendo.argosy.platform.filesDir",
        "com.nendo.argosy.platform.getSharedPreferences",
        "com.nendo.argosy.platform.list", "com.nendo.argosy.platform.open",
        "com.nendo.argosy.platform.getQuantityString", "com.nendo.argosy.platform.getString",
        "com.nendo.argosy.platform.resources",
    ],
    "com.nendo.argosy.platform.LocalPlatformContext": [
        "com.nendo.argosy.platform.applicationContext", "com.nendo.argosy.platform.packageName",
        "com.nendo.argosy.platform.assets", "com.nendo.argosy.platform.contentResolver", "com.nendo.argosy.platform.cacheDir", "com.nendo.argosy.platform.filesDir",
        "com.nendo.argosy.platform.getSharedPreferences",
        "com.nendo.argosy.platform.list", "com.nendo.argosy.platform.open",
        "com.nendo.argosy.platform.getQuantityString", "com.nendo.argosy.platform.getString",
        "com.nendo.argosy.platform.resources",
    ],
    "com.nendo.argosy.platform.LocalPlatformView": ["com.nendo.argosy.platform.getLocationOnScreen"],
    "com.nendo.argosy.platform.PlatformUri": [
        "com.nendo.argosy.platform.authority", "com.nendo.argosy.platform.encodedPath",
        "com.nendo.argosy.platform.getQueryParameter", "com.nendo.argosy.platform.host",
        "com.nendo.argosy.platform.lastPathSegment", "com.nendo.argosy.platform.path",
        "com.nendo.argosy.platform.pathSegments", "com.nendo.argosy.platform.queryParameterNames",
        "com.nendo.argosy.platform.scheme",
    ],
    "com.nendo.argosy.platform.PlatformBitmap": [
        "com.nendo.argosy.platform.asImageBitmap", "com.nendo.argosy.platform.config", "com.nendo.argosy.platform.copy",
        "com.nendo.argosy.platform.getPixel", "com.nendo.argosy.platform.height", "com.nendo.argosy.platform.recycle",
        "com.nendo.argosy.platform.width",
    ],
    "com.nendo.argosy.platform.LocalPlatformConfiguration": [
        "com.nendo.argosy.platform.orientation", "com.nendo.argosy.platform.screenHeightDp",
        "com.nendo.argosy.platform.screenWidthDp",
    ],
}


BITMAP_CONFIG = re.compile(r"(?<![\w.])Bitmap\.Config\b")


def rewrite_bitmap_config(text):
    if not re.search(r"^import com\.nendo\.argosy\.platform\.PlatformBitmap as Bitmap$", text, re.M):
        return text
    if not BITMAP_CONFIG.search(text):
        return text
    text = BITMAP_CONFIG.sub("PlatformBitmapConfig", text)
    if not re.search(r"^import com\.nendo\.argosy\.platform\.PlatformBitmapConfig$", text, re.M):
        text = re.sub(
            r"^(import com\.nendo\.argosy\.platform\.PlatformBitmap as Bitmap)$",
            r"\1\nimport com.nendo.argosy.platform.PlatformBitmapConfig",
            text,
            count=1,
            flags=re.M,
        )
    return text


MODE_PRIVATE = re.compile(r"(?<![\w.])Context\.MODE_PRIVATE\b")


def rewrite_mode_private(text):
    if not re.search(r"^import com\.nendo\.argosy\.platform\.PlatformContext as Context$", text, re.M):
        return text
    if not MODE_PRIVATE.search(text):
        return text
    text = MODE_PRIVATE.sub("MODE_PRIVATE", text)
    if not re.search(r"^import com\.nendo\.argosy\.platform\.MODE_PRIVATE$", text, re.M):
        text = re.sub(
            r"^(import com\.nendo\.argosy\.platform\.PlatformContext as Context)$",
            r"\1\nimport com.nendo.argosy.platform.MODE_PRIVATE",
            text,
            count=1,
            flags=re.M,
        )
    return text


URI_STATICS = re.compile(r"(?<![\w.])Uri\.(parse|fromFile|encode|decode)\(")


def rewrite_uri_statics(text):
    if not re.search(r"^import com\.nendo\.argosy\.platform\.PlatformUri as Uri$", text, re.M):
        return text
    if not URI_STATICS.search(text):
        return text
    text = URI_STATICS.sub(r"PlatformUris.\1(", text)
    if not re.search(r"^import com\.nendo\.argosy\.platform\.PlatformUris$", text, re.M):
        text = re.sub(
            r"^(import com\.nendo\.argosy\.platform\.PlatformUri as Uri)$",
            r"\1\nimport com.nendo.argosy.platform.PlatformUris",
            text,
            count=1,
            flags=re.M,
        )
    return text


def ensure_companions(text):
    for stand_in, companions in COMPANION_IMPORTS.items():
        anchor = re.search(rf"^import {re.escape(stand_in)}( as \w+)?$", text, re.M)
        if not anchor and re.search(rf"(?<!import ){re.escape(stand_in)}\b", text):
            anchor = list(re.finditer(r"^(?:package|import) .*$", text, re.M))[-1]
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
        new = rewrite_bitmap_config(new)
        new = rewrite_mode_private(new)
        new = rewrite_uri_statics(new)
        new = ensure_companions(new)
        if new != text:
            path.write_text(new)
            changed += 1
    for path in sorted(ANDROID.rglob("*.kt")):
        text = path.read_text()
        new = text
        for pattern, replacement in ANDROID_RULES:
            new = pattern.sub(replacement, new)
        if new != text:
            path.write_text(new)
            changed += 1
    print(f"rewrote imports in {changed} files")


if __name__ == "__main__":
    main()
