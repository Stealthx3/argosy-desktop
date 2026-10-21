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
    (re.compile(r"(?<!import )\bandroidx\.compose\.ui\.platform\.LocalConfiguration\b"), "com.nendo.argosy.platform.LocalPlatformConfiguration"),
    (re.compile(r"\bUri::(encode|decode)\b"), r"com.nendo.argosy.platform.PlatformUris::\1"),
    (re.compile(r"(?<!import )\bandroidx\.compose\.ui\.platform\.LocalContext\b"), "com.nendo.argosy.platform.LocalPlatformContext"),
    (re.compile(r"(?<!import )\bandroid\.content\.Intent\b(?!\s*\(|\.)"), "com.nendo.argosy.platform.PlatformIntentType"),
    (
        re.compile(r"\bandroid\.content\.Intent\((?!\s*(?:this|context|ctx|\w*[cC]ontext|applicationContext)\b)"),
        "com.nendo.argosy.platform.PlatformIntentType(",
    ),
    (re.compile(r"(?<!import )\bandroid\.hardware\.display\.DisplayManager\b(?!\.)"), "com.nendo.argosy.platform.PlatformDisplayManager"),
    (re.compile(r"\bandroid\.view\.Display\.DEFAULT_DISPLAY\b"), "com.nendo.argosy.platform.DEFAULT_DISPLAY"),
    (re.compile(r"^import android\.os\.Looper$", re.M), "import com.nendo.argosy.platform.PlatformLooper as Looper"),
    (re.compile(r"^import android\.os\.Handler$", re.M), "import com.nendo.argosy.platform.PlatformHandler as Handler"),
    (
        re.compile(r"^import android\.database\.ContentObserver$", re.M),
        "import com.nendo.argosy.platform.PlatformContentObserver as ContentObserver",
    ),
    (re.compile(r"^import android\.widget\.Toast$", re.M), "import com.nendo.argosy.platform.PlatformToasts as Toast"),
    (re.compile(r"\bandroid\.widget\.Toast\."), "com.nendo.argosy.platform.PlatformToasts."),
    (
        re.compile(r"\b(\w+)\.getSystemService\(\s*(?:android\.view\.inputmethod\.)?InputMethodManager::class\.java\s*\)"),
        r"com.nendo.argosy.platform.PlatformInputMethod.manager(\1)",
    ),
    (re.compile(r"\b(?:android\.view\.inputmethod\.)?InputMethodManager\.SHOW_FORCED\b"), "com.nendo.argosy.platform.PlatformInputMethod.SHOW_FORCED"),
    (re.compile(r"^import com\.nendo\.argosy\.shared\.R$", re.M), "import com.nendo.argosy.res.ArgosyR as R"),
    (re.compile(r"\bcom\.nendo\.argosy\.shared\.R\."), "com.nendo.argosy.res.ArgosyR."),
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
        "import com.nendo.argosy.platform.get\n"
        "import com.nendo.argosy.platform.locales\n"
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
    (re.compile(r"\bcoil\.compose\.AsyncImage\("), "com.nendo.argosy.platform.image.AsyncImage("),
    (re.compile(r"^import coil\.compose\.SubcomposeAsyncImage$", re.M), "import com.nendo.argosy.platform.image.SubcomposeAsyncImage"),
    (re.compile(r"^import android\.os\.Build$", re.M), "import com.nendo.argosy.platform.PlatformBuild as Build"),
    (re.compile(r"\bandroid\.os\.Build\."), "com.nendo.argosy.platform.PlatformBuild."),
    (re.compile(r"^import android\.graphics\.Color$", re.M), "import com.nendo.argosy.platform.PlatformColor as Color"),
    (re.compile(r"^import android\.graphics\.Color as (\w+)$", re.M), r"import com.nendo.argosy.platform.PlatformColor as \1"),
    (re.compile(r"\bandroid\.graphics\.Color\."), "com.nendo.argosy.platform.PlatformColor."),
    (
        re.compile(r"^import androidx\.core\.graphics\.ColorUtils$", re.M),
        "import com.nendo.argosy.platform.PlatformColorUtils as ColorUtils",
    ),
    (re.compile(r"\bandroidx\.core\.graphics\.ColorUtils\."), "com.nendo.argosy.platform.PlatformColorUtils."),
    (re.compile(r"^import android\.content\.res\.Resources$", re.M), "import com.nendo.argosy.platform.PlatformResources as Resources"),
    (
        re.compile(r"^import android\.content\.ActivityNotFoundException$", re.M),
        "import com.nendo.argosy.platform.PlatformActivityNotFoundException as ActivityNotFoundException",
    ),
    (
        re.compile(r"^import androidx\.core\.content\.ContextCompat$", re.M),
        "import com.nendo.argosy.platform.PlatformContextCompat as ContextCompat",
    ),
    (re.compile(r"^import android\.content\.pm\.PackageManager$", re.M), "import com.nendo.argosy.platform.PlatformPackageManagerConstants as PackageManager"),
    (re.compile(r"^import android\.Manifest$", re.M), "import com.nendo.argosy.platform.PlatformManifest as Manifest"),
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
    (
        re.compile(r"^import android\.content\.Intent$", re.M),
        "import com.nendo.argosy.platform.PlatformIntentType as Intent\nimport com.nendo.argosy.platform.PlatformIntent",
    ),
    (re.compile(r"\bandroid\.net\.Uri\.(parse|fromFile|encode|decode)\("), r"com.nendo.argosy.platform.PlatformUris.\1("),
    (re.compile(r"^import android\.net\.Uri$", re.M), "import com.nendo.argosy.platform.PlatformUri as Uri"),
    (re.compile(r"(?<!import )\bandroid\.net\.Uri\b"), "com.nendo.argosy.platform.PlatformUri"),
    (re.compile(r"^import android\.os\.Process$", re.M), "import com.nendo.argosy.platform.PlatformProcess as Process"),
    (re.compile(r"\bandroid\.os\.Process\.(is64Bit|myUid)\b"), r"com.nendo.argosy.platform.PlatformProcess.\1"),
    (
        re.compile(r"^import android\.net\.ConnectivityManager$", re.M),
        "import com.nendo.argosy.platform.PlatformConnectivityManager as ConnectivityManager",
    ),
    (re.compile(r"^import android\.net\.Network$", re.M), "import com.nendo.argosy.platform.PlatformNetwork as Network"),
    (
        re.compile(r"^import android\.net\.NetworkCapabilities$", re.M),
        "import com.nendo.argosy.platform.PlatformNetworkCapabilities as NetworkCapabilities",
    ),
    (re.compile(r"^import android\.os\.UserManager$", re.M), "import com.nendo.argosy.platform.PlatformUserManager as UserManager"),
    (re.compile(r"^import android\.annotation\.SuppressLint$", re.M), "import com.nendo.argosy.platform.SuppressLint"),
    (
        re.compile(r"^import android\.view\.MotionEvent$", re.M),
        "import com.nendo.argosy.platform.PlatformMotionEvent as MotionEvent\nimport com.nendo.argosy.core.input.AndroidMotionAxes",
    ),
    (re.compile(r"^import coil\.request\.ImageRequest$", re.M), "import com.nendo.argosy.platform.image.ImageRequest"),
    (re.compile(r"^import coil\.request\.CachePolicy$", re.M), "import com.nendo.argosy.platform.image.CachePolicy"),
    (re.compile(r"^import coil\.imageLoader$", re.M), "import com.nendo.argosy.platform.image.imageLoader"),
    (
        re.compile(
            r"val (\w+) = ([\w.]+)\.getSystemService\(android\.app\.LocaleManager::class\.java\)\n\s*\1\?\.applicationLocales = "
            r"if \((.+?)\) \{\n\s*android\.os\.LocaleList\.getEmptyLocaleList\(\)\n\s*\} else \{\n"
            r"\s*android\.os\.LocaleList\.forLanguageTags\((\w+)\)\n\s*\}"
        ),
        r"com.nendo.argosy.platform.setApplicationLocales(\2, if (\3) null else \4)",
    ),
    (re.compile(r"^import android\.view\.InputDevice$", re.M), "import com.nendo.argosy.platform.PlatformInputDevice as InputDevice"),
    (re.compile(r"\bandroid\.graphics\.BlurMaskFilter\.Blur\."), "com.nendo.argosy.platform.PlatformBlur."),
    (re.compile(r"\bandroid\.graphics\.BlurMaskFilter\("), "com.nendo.argosy.platform.PlatformBlurMaskFilter("),
    (re.compile(r"\bandroid\.graphics\.LinearGradient\("), "com.nendo.argosy.platform.PlatformLinearGradient("),
    (re.compile(r"\bandroid\.graphics\.Shader\.TileMode\."), "com.nendo.argosy.platform.PlatformTileMode."),
    (re.compile(r"\bandroid\.graphics\.Paint\(\)"), "com.nendo.argosy.platform.PlatformNativePaint()"),
    (
        re.compile(r"^import android\.graphics\.BlurMaskFilter$", re.M),
        "import com.nendo.argosy.platform.PlatformBlurMaskFilter as BlurMaskFilter\nimport com.nendo.argosy.platform.PlatformBlur",
    ),
    (re.compile(r"^import android\.app\.Application$", re.M), "import com.nendo.argosy.platform.PlatformApplication as Application"),
    (re.compile(r"^import android\.text\.Html$", re.M), "import com.nendo.argosy.platform.PlatformHtml as Html"),
    (re.compile(r"^import androidx\.core\.graphics\.toColorInt$", re.M), "import com.nendo.argosy.platform.toColorInt"),
    (re.compile(r"^import androidx\.room\.withTransaction$", re.M), "import com.nendo.argosy.platform.withTransaction"),
    (
        re.compile(r"^import androidx\.datastore\.preferences\.preferencesDataStoreFile$", re.M),
        "import com.nendo.argosy.platform.preferencesDataStoreFile",
    ),
    (
        re.compile(r"^import android\.graphics\.drawable\.BitmapDrawable$", re.M),
        "import com.nendo.argosy.platform.image.PlatformBitmapDrawable as BitmapDrawable\nimport com.nendo.argosy.platform.image.bitmap",
    ),
    (re.compile(r"\bandroid\.graphics\.drawable\.BitmapDrawable\b"), "com.nendo.argosy.platform.image.PlatformBitmapDrawable"),
    (re.compile(r"^import android\.os\.Bundle$", re.M), "import com.nendo.argosy.platform.PlatformBundle as Bundle"),
    (re.compile(r"\bandroid\.os\.Bundle\b"), "com.nendo.argosy.platform.PlatformBundle"),
    (re.compile(r"^import android\.view\.InputEvent$", re.M), "import com.nendo.argosy.platform.PlatformInputEvent as InputEvent"),
    (
        re.compile(r"^import android\.view\.KeyEvent$", re.M),
        "import com.nendo.argosy.platform.PlatformKeyEvent as KeyEvent\nimport com.nendo.argosy.core.input.AndroidKeyCodes",
    ),
]


COMPANION_IMPORTS = {
    "com.nendo.argosy.platform.PlatformContext": [
        "com.nendo.argosy.platform.startActivity", "com.nendo.argosy.platform.getApplicationInfo", "com.nendo.argosy.platform.getApplicationLabel",
        "com.nendo.argosy.platform.applicationContext", "com.nendo.argosy.platform.packageName",
        "com.nendo.argosy.platform.getSystemService", "com.nendo.argosy.platform.getExternalFilesDir",
        "com.nendo.argosy.platform.externalCacheDir", "com.nendo.argosy.platform.dataDir",
        "com.nendo.argosy.platform.packageManager", "com.nendo.argosy.platform.getPackageInfo",
        "com.nendo.argosy.platform.versionName",
        "com.nendo.argosy.platform.assets", "com.nendo.argosy.platform.contentResolver", "com.nendo.argosy.platform.cacheDir", "com.nendo.argosy.platform.filesDir",
        "com.nendo.argosy.platform.getSharedPreferences",
        "com.nendo.argosy.platform.list", "com.nendo.argosy.platform.open",
        "com.nendo.argosy.platform.getQuantityString", "com.nendo.argosy.platform.getString",
        "com.nendo.argosy.platform.resources",
    ],
    "com.nendo.argosy.platform.LocalPlatformContext": [
        "com.nendo.argosy.platform.startActivity", "com.nendo.argosy.platform.getApplicationInfo", "com.nendo.argosy.platform.getApplicationLabel",
        "com.nendo.argosy.platform.applicationContext", "com.nendo.argosy.platform.packageName",
        "com.nendo.argosy.platform.getSystemService", "com.nendo.argosy.platform.getExternalFilesDir",
        "com.nendo.argosy.platform.externalCacheDir", "com.nendo.argosy.platform.dataDir",
        "com.nendo.argosy.platform.packageManager", "com.nendo.argosy.platform.getPackageInfo",
        "com.nendo.argosy.platform.versionName",
        "com.nendo.argosy.platform.assets", "com.nendo.argosy.platform.contentResolver", "com.nendo.argosy.platform.cacheDir", "com.nendo.argosy.platform.filesDir",
        "com.nendo.argosy.platform.getSharedPreferences",
        "com.nendo.argosy.platform.list", "com.nendo.argosy.platform.open",
        "com.nendo.argosy.platform.getQuantityString", "com.nendo.argosy.platform.getString",
        "com.nendo.argosy.platform.resources",
    ],
    "com.nendo.argosy.platform.LocalPlatformView": ["com.nendo.argosy.platform.getLocationOnScreen"],
    "com.nendo.argosy.platform.PlatformConnectivityManager": [
        "com.nendo.argosy.platform.activeNetwork", "com.nendo.argosy.platform.getNetworkCapabilities",
        "com.nendo.argosy.platform.registerDefaultNetworkCallback", "com.nendo.argosy.platform.unregisterNetworkCallback",
        "com.nendo.argosy.platform.hasCapability",
    ],
    "com.nendo.argosy.platform.PlatformNetworkCapabilities": ["com.nendo.argosy.platform.hasCapability"],
    "com.nendo.argosy.platform.PlatformUserManager": ["com.nendo.argosy.platform.isUserUnlocked"],
    "com.nendo.argosy.platform.PlatformDisplayManager": [
        "com.nendo.argosy.platform.getDisplay", "com.nendo.argosy.platform.refreshRate", "com.nendo.argosy.platform.supportedModes",
    ],
    "com.nendo.argosy.platform.PlatformInputDevice": [
        "com.nendo.argosy.platform.controllerNumber", "com.nendo.argosy.platform.descriptor",
        "com.nendo.argosy.platform.id", "com.nendo.argosy.platform.isEnabled", "com.nendo.argosy.platform.isExternal",
        "com.nendo.argosy.platform.isVirtual", "com.nendo.argosy.platform.name", "com.nendo.argosy.platform.productId",
        "com.nendo.argosy.platform.sources", "com.nendo.argosy.platform.vendorId",
    ],
    "androidx.compose.ui.graphics.nativeCanvas": ["com.nendo.argosy.platform.drawRoundRect"],
    "com.nendo.argosy.platform.PlatformApplication": [
        "com.nendo.argosy.platform.applicationContext", "com.nendo.argosy.platform.cacheDir",
        "com.nendo.argosy.platform.contentResolver", "com.nendo.argosy.platform.filesDir",
        "com.nendo.argosy.platform.getSharedPreferences", "com.nendo.argosy.platform.getString",
        "com.nendo.argosy.platform.packageName", "com.nendo.argosy.platform.resources",
        "com.nendo.argosy.platform.startActivity",
    ],
    "com.nendo.argosy.platform.image.AsyncImage": [
        "com.nendo.argosy.platform.image.bitmap", "com.nendo.argosy.platform.image.intrinsicHeight",
        "com.nendo.argosy.platform.image.intrinsicWidth",
    ],
    "com.nendo.argosy.platform.PlatformToasts": ["com.nendo.argosy.platform.show"],
    "com.nendo.argosy.platform.PlatformResources": [
        "com.nendo.argosy.platform.getQuantityString", "com.nendo.argosy.platform.getString",
    ],
    "com.nendo.argosy.platform.PlatformContextCompat": ["com.nendo.argosy.platform.displayId"],
    "com.nendo.argosy.platform.PlatformContentObserver": [
        "com.nendo.argosy.platform.registerContentObserver", "com.nendo.argosy.platform.unregisterContentObserver",
    ],
    "com.nendo.argosy.platform.PlatformInputMethod": ["com.nendo.argosy.platform.toggleSoftInput"],
    "com.nendo.argosy.platform.LocalPlatformConfiguration": [
        "com.nendo.argosy.platform.get", "com.nendo.argosy.platform.locales", "com.nendo.argosy.platform.orientation", "com.nendo.argosy.platform.screenHeightDp",
        "com.nendo.argosy.platform.screenWidthDp",
    ],
    "com.nendo.argosy.platform.PlatformIntentType": [
        "com.nendo.argosy.platform.action", "com.nendo.argosy.platform.addFlags", "com.nendo.argosy.platform.data",
        "com.nendo.argosy.platform.flags", "com.nendo.argosy.platform.putExtra",
        "com.nendo.argosy.platform.component", "com.nendo.argosy.platform.`package`",
        "com.nendo.argosy.platform.getStringExtra", "com.nendo.argosy.platform.packageName",
        "com.nendo.argosy.platform.className",
    ],
    "com.nendo.argosy.platform.PlatformInputEvent": [
        "com.nendo.argosy.platform.device", "com.nendo.argosy.platform.deviceId", "com.nendo.argosy.platform.eventTime",
        "com.nendo.argosy.platform.isFromSource", "com.nendo.argosy.platform.source",
    ],
    "com.nendo.argosy.platform.PlatformKeyEvent": [
        "com.nendo.argosy.platform.name",
        "com.nendo.argosy.platform.action", "com.nendo.argosy.platform.device", "com.nendo.argosy.platform.deviceId",
        "com.nendo.argosy.platform.downTime", "com.nendo.argosy.platform.eventTime", "com.nendo.argosy.platform.isFromSource",
        "com.nendo.argosy.platform.keyCode", "com.nendo.argosy.platform.metaState", "com.nendo.argosy.platform.repeatCount",
        "com.nendo.argosy.platform.scanCode", "com.nendo.argosy.platform.source",
    ],
    "com.nendo.argosy.platform.PlatformMotionEvent": [
        "com.nendo.argosy.platform.action", "com.nendo.argosy.platform.actionMasked", "com.nendo.argosy.platform.device",
        "com.nendo.argosy.platform.deviceId", "com.nendo.argosy.platform.eventTime", "com.nendo.argosy.platform.getAxisValue",
        "com.nendo.argosy.platform.isFromSource", "com.nendo.argosy.platform.source",
    ],
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
        "com.nendo.argosy.platform.setPixels",
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


STATIC_STAND_INS = [
    ("PlatformKeyEvent as KeyEvent", r"KeyEvent\.(?=(?:KEYCODE_|ACTION_|META_|FLAG_)\w+)", r"AndroidKeyCodes."),
    ("PlatformLooper as Looper", r"Looper\.(getMainLooper)\(", r"PlatformLoopers.\1("),
    ("PlatformKeyEvent as KeyEvent", r"KeyEvent\.(keyCodeToString)\b", r"\1"),
    ("PlatformMotionEvent as MotionEvent", r"MotionEvent\.(?=(?:AXIS_|ACTION_)\w+)", r"AndroidMotionAxes."),
    ("PlatformIntentType as Intent", r"Intent\.(?=[A-Z][A-Z0-9_]+\b)", r"PlatformIntent."),
    ("PlatformConfiguration as Configuration", r"Configuration\.(ORIENTATION_\w+|UI_MODE_NIGHT_\w+)\b", r"\1"),
    ("PlatformBitmap as Bitmap", r"Bitmap\.(createBitmap)\(", r"PlatformBitmaps.\1("),
    ("PlatformBlurMaskFilter as BlurMaskFilter", r"BlurMaskFilter\.Blur\.", r"PlatformBlur."),
    ("PlatformInputDevice as InputDevice", r"InputDevice\.(getDevice|getDeviceIds)\(", r"PlatformInputDevices.\1("),
    ("PlatformInputDevice as InputDevice", r"InputDevice\.(SOURCE_\w+)\b", r"\1"),
    ("core.input.AndroidKeyCodes as KeyEvent", r"KeyEvent\.(keyCodeToString)\b", r"\1"),
    ("PlatformContext as Context", r"Context\.(MODE_PRIVATE|CONNECTIVITY_SERVICE|USER_SERVICE|DISPLAY_SERVICE)\b", r"\1"),
    ("PlatformNetworkCapabilities as NetworkCapabilities", r"NetworkCapabilities\.(NET_CAPABILITY_INTERNET)\b", r"\1"),
    ("PlatformConnectivityManager as ConnectivityManager", r"ConnectivityManager\.(NetworkCallback)\b", r"Platform\1"),
]


def rewrite_static_stand_ins(text):
    for anchor, pattern, replacement in STATIC_STAND_INS:
        anchor_import = anchor if anchor.startswith("core.") else f"platform.{anchor}"
        if not re.search(rf"^import com\.nendo\.argosy\.{re.escape(anchor_import)}$", text, re.M):
            continue
        regex = re.compile(r"(?<!\w)(?<!\w\.)" + pattern)
        names = {re.match(r"\w+", m.expand(replacement)).group(0) for m in regex.finditer(text)}
        if not names:
            continue
        text = regex.sub(replacement, text)
        missing = [n for n in sorted(names) if not re.search(rf"^import [\w.]+\.{n}(?: as \w+)?$", text, re.M)]
        if missing:
            text = re.sub(
                rf"^(import com\.nendo\.argosy\.{re.escape(anchor_import)})$",
                lambda m: m.group(1) + "".join(f"\nimport com.nendo.argosy.platform.{n}" for n in missing),
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


UNUSED_ANDROID_IMPORT = re.compile(r"^import ((?:android|coil)\.[\w.]+|androidx\.(?:core|media3|activity)\.[\w.]+)$", re.M)


def drop_unused_android_imports(text):
    body = UNUSED_ANDROID_IMPORT.sub("", text)
    for m in UNUSED_ANDROID_IMPORT.finditer(text):
        name = m.group(1).rsplit(".", 1)[1]
        if not re.search(rf"\b{re.escape(name)}\b", body):
            text = re.sub(rf"^import {re.escape(m.group(1))}\n", "", text, count=1, flags=re.M)
    return text


def main():
    changed = 0
    for path in sorted(COMMON.rglob("*.kt")):
        text = path.read_text()
        new = drop_unused_android_imports(text)
        for pattern, replacement in RULES:
            new = pattern.sub(replacement, new)
        new = rewrite_bitmap_config(new)
        new = rewrite_static_stand_ins(new)
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
        if re.search(r"^package com\.nendo\.argosy$", new, re.M):
            body = re.sub(r"^import .*$", "", new, flags=re.M)
            additions = []
            if re.search(r"(?<![\w.])R\.\w", body) and not re.search(r"^import [\w.]+\.R$", new, re.M):
                additions.append("import com.nendo.argosy.shared.R")
            if re.search(r"(?<![\w.])BuildConfig\.\w", body) and not re.search(r"^import [\w.]+ as BuildConfig$|^import [\w.]+\.BuildConfig$", new, re.M):
                additions.append("import com.nendo.argosy.config.AppBuildConfig as BuildConfig")
            if additions:
                new = re.sub(r"^(package com\.nendo\.argosy)$", r"\1\n\n" + "\n".join(additions), new, count=1, flags=re.M)
        if new != text:
            path.write_text(new)
            changed += 1
    print(f"rewrote imports in {changed} files")


if __name__ == "__main__":
    main()
