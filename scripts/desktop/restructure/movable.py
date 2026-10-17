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

keep-in-app.txt lists globs that stay in app whatever this script concludes (with the reason,
usually a desktop compile error the import rules cannot see).

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
    "androidx.annotation.",
    "androidx.compose.ui.", "androidx.compose.foundation.", "androidx.compose.animation.",
    "androidx.compose.material3.", "androidx.compose.material.icons.", "androidx.compose.material.ripple.",
    "androidx.lifecycle.",
    "com.nendo.sigil.", "com.google.zxing.", "org.bitlet.weupnp.", "com.swordfish.libretrodroid.Variable", "com.swordfish.libretrodroid.ShaderConfig",
)
NOT_ALLOWED = (
    "androidx.sqlite.db.",
    "androidx.lifecycle.ProcessLifecycleOwner", "androidx.lifecycle.AndroidViewModel", "androidx.lifecycle.LiveData",
    "androidx.lifecycle.MutableLiveData", "androidx.lifecycle.asLiveData", "androidx.lifecycle.Observer",
    "androidx.lifecycle.ViewModelProvider", "androidx.lifecycle.findViewTree",
    "androidx.compose.ui.platform.ComposeView", "androidx.compose.ui.tooling.", "androidx.compose.ui.viewinterop.",
    "androidx.compose.ui.res.",
    "androidx.compose.ui.graphics.asAndroid", "androidx.compose.ui.graphics.nativeCanvas",
    "androidx.compose.ui.graphics.NativePaint", "androidx.compose.ui.text.font.Typeface",
    "androidx.compose.ui.text.android", "androidx.compose.ui.input.pointer.pointerInteropFilter",
    "androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent",
)
REWRITTEN = (
    "android.annotation.SuppressLint",
    "android.util.Log", "android.graphics.Color.", "android.os.Build.", "com.nendo.argosy.BuildConfig", "dagger.hilt.android.lifecycle.HiltViewModel",
    "com.nendo.argosy.R",
    "androidx.compose.ui.res.stringResource", "androidx.compose.ui.res.pluralStringResource",
    "androidx.compose.ui.res.painterResource", "androidx.compose.ui.platform.LocalLifecycleOwner",
    "android.os.SystemClock.elapsedRealtime", "android.os.SystemClock.uptimeMillis", "android.os.StatFs",
    "android.os.Environment.MEDIA_MOUNTED", "android.os.Environment.DIRECTORY_DOWNLOADS",
    "android.os.Environment.getExternalStorage", "android.os.Environment.isExternalStorageManager",
    "android.text.format.DateFormat.getBestDateTimePattern",
)
GENERATED = ("com.nendo.argosy.config.AppBuildConfig", "com.nendo.argosy.res.ArgosyR")
CONTEXT_MEMBERS = (
    r"(?!getString\b|resources\b|filesDir\b|cacheDir\b|assets\b|getSharedPreferences\b|contentResolver\b"
    r"|packageName\b|applicationContext\b|getSystemService\b|getExternalFilesDir\b|packageManager\b)[a-zA-Z_]\w*"
)
CONTEXT_REST = (
    r"\.resources\.(?!getQuantityString\b|getString\b)\w+"
    r"|LocalContext\.current\." + CONTEXT_MEMBERS
    + r"|\.assets\.(?!list\b|open\b)\w+"
    r"|\bcontentResolver\.\w+"
    r"|(?<![\w.])Context\.(?!MODE_PRIVATE\b|CONNECTIVITY_SERVICE\b|USER_SERVICE\b)[A-Z]\w*"
    r"|\bpackageManager\.(?!getPackageInfo\b)\w+|\bpm\.(?!getPackageInfo\b)\w+"
    r"|\.getPackageInfo\([^)]*\)\.(?!versionName\b)\w+"
)
CONTEXT_BEYOND_STRINGS = (
    r"\b(?:context|ctx|appContext|applicationContext|baseContext)\??\." + CONTEXT_MEMBERS + "|" + CONTEXT_REST
)
CONTEXT_DECLARED = re.compile(
    r"\b(\w+)\s*:\s*(?:android\.content\.|com\.nendo\.argosy\.platform\.Platform)?Context\b\??"
    r"|\b(?:val|var)\s+(\w+)\s*=\s*(?:LocalContext|LocalPlatformContext)\.current\b"
)


CONTEXT_NAMES = ("context", "ctx", "appContext", "applicationContext", "baseContext")
ANY_DECLARED = re.compile(r"\b(\w+)\s*:\s*([\w.]+)")


class ContextGuard:
    def __init__(self):
        self.rest = re.compile(CONTEXT_REST)

    def search(self, body):
        names = {a or b for a, b in CONTEXT_DECLARED.findall(body)}
        other = {n for n, t in ANY_DECLARED.findall(body) if not re.fullmatch(r"(?:android\.content\.|com\.nendo\.argosy\.platform\.Platform)?Context", t)}
        names |= {n for n in CONTEXT_NAMES if n not in other}
        declared = re.compile(r"\b(?:" + "|".join(sorted(names)) + r")\??\." + CONTEXT_MEMBERS)
        return declared.search(body) or self.rest.search(body)
GUARDED_REWRITES = {
    "android.content.Context": ContextGuard(),
    "androidx.compose.ui.platform.LocalContext": ContextGuard(),
    "dagger.hilt.android.qualifiers.ApplicationContext": re.compile(r"(?!)"),
    "androidx.hilt.navigation.compose.hiltViewModel": re.compile(r"hiltViewModel(?:<[^>]*>)?\((?!\))"),
    "androidx.compose.ui.platform.LocalConfiguration": re.compile(
        r"LocalConfiguration\.current\.(?!screenWidthDp\b|screenHeightDp\b|orientation\b)\w+"
        r"|\bconfiguration\.(?!screenWidthDp\b|screenHeightDp\b|orientation\b)\w+"
    ),
    "androidx.activity.compose.BackHandler": re.compile(r"BackHandler\((?!enabled = true\)|\))[^)]"),
    "android.graphics.Color": re.compile(
        r"\b(?:AndroidColor|android\.graphics\.Color|Color)\.(?!(?:colorToHSV|HSVToColor|parseColor|argb|rgb|red|green|blue|alpha|BLACK|WHITE|TRANSPARENT)\b)[a-z]\w*\("
        r"|\b(?:AndroidColor|android\.graphics\.Color)\.(?!(?:colorToHSV|HSVToColor|parseColor|argb|rgb|red|green|blue|alpha|BLACK|WHITE|TRANSPARENT)\b)\w+"
    ),
    "androidx.compose.ui.platform.LocalView": re.compile(
        r"LocalView\.current\.(?!getLocationOnScreen\b)\w+|\bview\.(?!getLocationOnScreen\b)\w+"
    ),
    "coil.compose.AsyncImage": re.compile(
        r"\b(?:onSuccess|onLoading|onState|transform|clipToBounds|imageLoader|modelEqualityDelegate)\s*="
        r"|onError\s*=\s*\{\s*\w+\s*->|\bAsyncImagePainter\b"
    ),
    "android.os.Build": re.compile(
        r"\bBuild\.(?!MODEL\b|MANUFACTURER\b|BRAND\b|DEVICE\b|PRODUCT\b|BOARD\b|FINGERPRINT\b"
        r"|SUPPORTED_64_BIT_ABIS\b|SUPPORTED_32_BIT_ABIS\b|VERSION\.SDK_INT\b|VERSION\.RELEASE\b"
        r"|VERSION_CODES\.(?:N|O|P|Q|R|S|TIRAMISU|UPSIDE_DOWN_CAKE)\b)\w+(?:\.\w+)?"
    ),
    "android.os.Process": re.compile(r"\bProcess\.(?!is64Bit\b|myUid\b)\w+"),
    "android.content.res.Configuration": re.compile(r"\bConfiguration\.[A-Za-z_]"),
    "androidx.core.graphics.ColorUtils": re.compile(r"\bColorUtils\.(?!colorToHSL\b|HSLToColor\b)\w+"),
    "android.view.KeyEvent": re.compile(r"\bKeyEvent\b(?!\.KEYCODE_\w|\.keyCodeToString\b)"),
    "android.view.MotionEvent": re.compile(r"\bMotionEvent\b(?!\.AXIS_\w)"),
    "com.nendo.argosy.util.DisplayAffinityHelper": re.compile(
        r"\bDisplayAffinityHelper\b(?!\.isKnownDualScreenDevice\(\))"
    ),
    "android.os.SystemClock": re.compile(r"\bSystemClock\.(?!elapsedRealtime\b|uptimeMillis\b)\w+"),
    "android.os.Environment": re.compile(
        r"\bEnvironment\.(?!MEDIA_MOUNTED\b|DIRECTORY_DOWNLOADS\b|getExternalStorageDirectory\b"
        r"|getExternalStoragePublicDirectory\b|getExternalStorageState\b|isExternalStorageManager\b)\w+"
    ),
    "android.os.StatFs": re.compile(
        r"\b(?:availableBlocks\w*|freeBlocks\w*|blockCount\w*|blockSize\w*|freeBytes|totalBytes|restat)\b"
    ),
    "android.text.format.DateFormat": re.compile(r"\bDateFormat\.(?!getBestDateTimePattern\b)\w+"),
    "android.graphics.Bitmap": re.compile(
        r"\bBitmap\.(?!Config\.(?:ARGB_8888|HARDWARE)\b)\w+"
        r"|\.(?:compress|eraseColor|setPixels?|getPixels|copyPixels\w*|isRecycled|byteCount|allocationByteCount"
        r"|density|hasAlpha|setHasAlpha|prepareToDraw|extractAlpha|reconfigure|rowBytes|generationId|isMutable"
        r"|colorSpace|asShared|hardwareBuffer|getHardwareBuffer|scale)\b"
    ),
    "android.graphics.BitmapFactory": re.compile(
        r"\bBitmapFactory\.(?!decodeFile\b|Options\b)\w+"
        r"|\b(?:inSampleSize|inPreferredConfig|inMutable|inScaled|inDensity|inTargetDensity|outMimeType|outConfig|inBitmap)\b"
    ),
    "android.content.SharedPreferences": re.compile(
        r"\b(?:getStringSet|putStringSet|getFloat|putFloat|getAll|registerOnSharedPreferenceChangeListener"
        r"|unregisterOnSharedPreferenceChangeListener|OnSharedPreferenceChangeListener)\b"
    ),
    "android.provider.Settings": re.compile(r"\bSettings\.(?!Secure\.(?:getString|ANDROID_ID)\b)\w+(?:\.\w+)?"),
    "android.content.Intent": re.compile(r"\bIntent\b(?!\.(?:ACTION_BATTERY_CHANGED|ACTION_BOOT_COMPLETED|ACTION_DELETE|ACTION_MAIN|ACTION_MY_PACKAGE_REPLACED|ACTION_PACKAGE_ADDED|ACTION_PACKAGE_REMOVED|ACTION_PACKAGE_REPLACED|ACTION_SCREEN_OFF|ACTION_SCREEN_ON|ACTION_USER_PRESENT|ACTION_VIEW|CATEGORY_DEFAULT|CATEGORY_HOME|CATEGORY_LAUNCHER|FLAG_ACTIVITY_CLEAR_TASK|FLAG_ACTIVITY_CLEAR_TOP|FLAG_ACTIVITY_MULTIPLE_TASK|FLAG_ACTIVITY_NEW_TASK|FLAG_ACTIVITY_NO_ANIMATION|FLAG_ACTIVITY_NO_HISTORY|FLAG_ACTIVITY_REORDER_TO_FRONT|FLAG_ACTIVITY_SINGLE_TOP|FLAG_GRANT_READ_URI_PERMISSION)\b)"),
    "android.net.Uri": re.compile(
        r"\bUri\.(?!parse\b|fromFile\b|encode\b|decode\b)\w+"
        r"|\.(?:buildUpon|normalizeScheme|isHierarchical|isOpaque|isRelative|isAbsolute|fragment|encodedQuery"
        r"|encodedFragment|port|userInfo|schemeSpecificPart|encodedSchemeSpecificPart|encodedAuthority"
        r"|getQueryParameters|getBooleanQueryParameter|encodedPathSegments|encodedUserInfo)\b"
    ),
    "android.net.ConnectivityManager": re.compile(
        r"\b(?:cm|connectivityManager|connectivity)\.(?!activeNetwork\b|getNetworkCapabilities\b"
        r"|registerDefaultNetworkCallback\b|unregisterNetworkCallback\b)\w+"
        r"|\bConnectivityManager\.(?!NetworkCallback\b)\w+|override fun on(?!Available\b|Lost\b)\w+\("
    ),
    "android.net.NetworkCapabilities": re.compile(
        r"\bNetworkCapabilities\.(?!NET_CAPABILITY_INTERNET\b)\w+"
        r"|\b(?:hasTransport|transportTypes|linkDownstreamBandwidthKbps|linkUpstreamBandwidthKbps|signalStrength)\b"
    ),
    "android.net.Network": re.compile(r"\b(?:bindSocket|socketFactory|openConnection|networkHandle|getAllByName)\b"),
    "android.os.UserManager": re.compile(r"\b(?:userManager|um)\.(?!isUserUnlocked\b)\w+|\bUserManager\.\w+"),
    "coil.request.ImageRequest": re.compile(
        r"\.(?:listener|target|placeholder|error|fallback|transformations|precision|scale|decoderFactory|fetcherFactory"
        r"|bitmapConfig|colorSpace|networkCachePolicy|lifecycle|parameters|tag|headers|setHeader|placeholderMemoryCacheKey"
        r"|allowConversionToBitmap|allowRgb565|premultipliedAlpha|dispatcher|interceptorDispatcher|fetcherDispatcher"
        r"|decoderDispatcher|transformationDispatcher|sizeResolver|newBuilder|defaults)\("
        r"|\bImageRequest\.(?!Builder\b)\w+"
    ),
    "coil.request.CachePolicy": re.compile(r"\bCachePolicy\.(?!ENABLED\b|READ_ONLY\b|WRITE_ONLY\b|DISABLED\b)\w+"),
    "android.view.InputDevice": re.compile(
        r"(?<!fun )\bInputDevice\.(?!getDevice\b|getDeviceIds\b|SOURCE_(?:CLASS_BUTTON|CLASS_JOYSTICK|DPAD|GAMEPAD|JOYSTICK|KEYBOARD)\b)\w+"
        r"|\.(?:motionRanges|getMotionRange|hasKeys|supportsSource|keyboardType|vibrator|vibratorManager|batteryState"
        r"|lightsManager|sensorManager|keyCharacterMap|getKeyCodeForKeyLocation)\b"
    ),
    "android.util.Base64": re.compile(
        r"\bBase64\.(?!decode\b|encode\b|encodeToString\b|DEFAULT\b|NO_PADDING\b|NO_WRAP\b|CRLF\b|URL_SAFE\b)\w+"
    ),
}
FQ_GUARDED = (
    "android.content.Context", "android.os.SystemClock", "dagger.hilt.android.qualifiers.ApplicationContext",
    "android.graphics.Bitmap", "android.graphics.BitmapFactory", "android.util.Base64",
    "android.provider.Settings", "android.net.Uri", "android.os.Process",
)
STRINGS = re.compile(r'"(?:[^"\\\n]|\\.)*"')
STAND_IN_GUARDS = {
    "com.nendo.argosy.platform.PlatformContext": "android.content.Context",
    "com.nendo.argosy.platform.LocalPlatformContext": "androidx.compose.ui.platform.LocalContext",
    "com.nendo.argosy.platform.LocalPlatformConfiguration": "androidx.compose.ui.platform.LocalConfiguration",
    "com.nendo.argosy.platform.PlatformConfiguration": "android.content.res.Configuration",
    "com.nendo.argosy.platform.PlatformColor": "android.graphics.Color",
    "com.nendo.argosy.platform.PlatformColorUtils": "androidx.core.graphics.ColorUtils",
    "com.nendo.argosy.di.hiltViewModel": "androidx.hilt.navigation.compose.hiltViewModel",
    "com.nendo.argosy.platform.BackHandler": "androidx.activity.compose.BackHandler",
    "com.nendo.argosy.core.input.AndroidKeyCodes": "android.view.KeyEvent",
    "com.nendo.argosy.util.DualScreenDeviceInfo": "com.nendo.argosy.util.DisplayAffinityHelper",
    "com.nendo.argosy.platform.PlatformBuild": "android.os.Build",
    "com.nendo.argosy.platform.image.AsyncImage": "coil.compose.AsyncImage",
    "com.nendo.argosy.platform.LocalPlatformView": "androidx.compose.ui.platform.LocalView",
    "com.nendo.argosy.platform.PlatformSystemClock": "android.os.SystemClock",
    "com.nendo.argosy.platform.PlatformEnvironment": "android.os.Environment",
    "com.nendo.argosy.platform.PlatformStatFs": "android.os.StatFs",
    "com.nendo.argosy.platform.PlatformDateFormat": "android.text.format.DateFormat",
    "com.nendo.argosy.platform.PlatformBitmap": "android.graphics.Bitmap",
    "com.nendo.argosy.platform.PlatformBitmapFactory": "android.graphics.BitmapFactory",
    "com.nendo.argosy.platform.PlatformBase64": "android.util.Base64",
    "com.nendo.argosy.platform.PlatformSharedPreferences": "android.content.SharedPreferences",
    "com.nendo.argosy.platform.PlatformSettings": "android.provider.Settings",
    "com.nendo.argosy.platform.PlatformIntent": "android.content.Intent",
    "com.nendo.argosy.platform.PlatformUri": "android.net.Uri",
    "com.nendo.argosy.platform.PlatformProcess": "android.os.Process",
    "com.nendo.argosy.platform.PlatformConnectivityManager": "android.net.ConnectivityManager",
    "com.nendo.argosy.platform.PlatformNetworkCapabilities": "android.net.NetworkCapabilities",
    "com.nendo.argosy.platform.PlatformNetwork": "android.net.Network",
    "com.nendo.argosy.platform.PlatformUserManager": "android.os.UserManager",
    "com.nendo.argosy.core.input.AndroidMotionAxes": "android.view.MotionEvent",
    "com.nendo.argosy.platform.image.ImageRequest": "coil.request.ImageRequest",
    "com.nendo.argosy.platform.image.CachePolicy": "coil.request.CachePolicy",
    "com.nendo.argosy.platform.PlatformInputDevice": "android.view.InputDevice",
}
COMMENTS = re.compile(r"/\*.*?\*/|^\s*//[^\n]*", re.S | re.M)
ANDROID_FQ = re.compile(r"(?<![\w.])(?:android|androidx|com|org|io|net|dagger|coil|in|okhttp3|retrofit2)\.[a-z]\w*(?:\.\w+)+")
ANDROID_MAIN_ALLOWED = ALLOWED + (
    "android.", "androidx.", "dagger.hilt.", "coil.", "org.xmlpull.", "com.swordfish.libretrodroid.",
    "com.goterl.lazysodium.", "com.sun.jna.",
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
        self.names, self.receivers, self.expects = set(), {}, set()
        for line in self.text.splitlines():
            m = DECL.match(line)
            if not m or re.search(r"\bactual\b", line.split("(")[0]):
                continue
            if re.search(r"\bprivate\b", re.split(r"\b(?:class|interface|object|fun|val|var|typealias)\b", line, maxsplit=1)[0]):
                continue
            self.names.add(m.group(2))
            if re.search(r"\bexpect\b", line.split("(")[0]):
                self.expects.add(m.group(2))
            if m.group(1):
                self.receivers[m.group(2)] = re.sub(r"<.*|\?", "", m.group(1)).split(".")[-1]
        self.imports = IMPORT.findall(self.text)
        self.body = COMMENTS.sub("", IMPORT.sub("", self.text))
        self.sealed = set(re.findall(r"^\s*(?:\w+\s+)*sealed\s+(?:class|interface)\s+(\w+)", self.text, re.M))


def load(root):
    return [Unit(p, root) for p in sorted((root / PKG_ROOT).rglob("*.kt"))]


GUARD_EXEMPT = Path(__file__).with_name("guard-exempt.txt")
EXEMPTIONS = [
    tuple(part.strip() for part in line.split("#")[0].split(":", 1))
    for line in (GUARD_EXEMPT.read_text().splitlines() if GUARD_EXEMPT.exists() else [])
    if line.split("#")[0].strip()
]


def exempt(u, imp):
    return any(imp == guarded and fnmatch.fnmatchcase(u.rel, glob) for glob, guarded in EXEMPTIONS)


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
            if GUARDED_REWRITES[imp].search(u.body) and not exempt(u, imp):
                common.append(f"uses {imp} beyond its portable stand-in")
            target = resolve(imp)
            if target:
                deps |= target
            continue
        if imp in REWRITTEN or imp in GENERATED:
            continue
        if imp in STAND_IN_GUARDS and GUARDED_REWRITES[STAND_IN_GUARDS[imp]].search(u.body) and not exempt(u, STAND_IN_GUARDS[imp]):
            common.append(f"uses {imp} beyond what it offers")
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
        if fq.startswith("android.os.Build.") and GUARDED_REWRITES["android.os.Build"].search(fq[len("android.os."):]):
            common.append(f"reference {fq} beyond PlatformBuild")
        guarded = next((k for k in FQ_GUARDED if fq == k or fq.startswith(k + ".")), None)
        if guarded:
            if GUARDED_REWRITES[guarded].search(u.body) and not exempt(u, guarded):
                common.append(f"reference {fq} beyond its portable stand-in")
            continue
        if not fq.startswith(REWRITTEN) and not fq.startswith("com.nendo.argosy.") and fq != "com.nendo.argosy":
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


KEEP_IN_APP = Path(__file__).with_name("keep-in-app.txt")


def main():
    args = sys.argv[1:]
    excluded = [
        line.split("#")[0].strip()
        for line in (KEEP_IN_APP.read_text().splitlines() if KEEP_IN_APP.exists() else [])
        if line.split("#")[0].strip()
    ]
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
    for u in common_shared:
        for n in u.expects:
            index[(u.pkg, n)] = {u}
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
        origin = by_rel[args[1]]
        if origin in movable:
            print(f"{origin.rel}: movable")
            return
        parents, queue = {origin: None}, [origin]
        while queue:
            current = queue.pop(0)
            if common_reasons[current]:
                path = []
                while current is not None:
                    path.append(current)
                    current = parents[current]
                for step in reversed(path[1:]):
                    print(f"{step.rel} ->")
                print(f"{path[0].rel}: {'; '.join(common_reasons[path[0]])}")
                return
            for dep in sorted(deps[current], key=lambda x: x.rel):
                if dep not in movable and dep not in in_common and dep not in parents:
                    parents[dep] = current
                    queue.append(dep)
        print(f"{origin.rel}: held only by excluded or androidMain files")
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
