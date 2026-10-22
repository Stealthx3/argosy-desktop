package com.nendo.argosy.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.nendo.argosy.platform.LocalPlatformConfiguration as LocalConfiguration
import com.nendo.argosy.platform.get
import com.nendo.argosy.platform.locales
import com.nendo.argosy.platform.orientation
import com.nendo.argosy.platform.screenHeightDp
import com.nendo.argosy.platform.screenWidthDp
import com.nendo.argosy.platform.LocalPlatformContext as LocalContext
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.data.emulator.BuiltinCoreResolver
import com.nendo.argosy.data.repository.CoreVersionRepository
import com.nendo.argosy.libretro.LibretroCoreRegistry
import com.nendo.argosy.ui.screens.settings.sections.formatCoreBuildStamp
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

/**
 * The core the built-in emulator loads for a platform or game, and the buildbot stamp of its
 * downloaded build. [buildStamp] is null when no build of the core was downloaded.
 */
data class BuiltinCoreSummary(
    val coreName: String,
    val buildStamp: String?
) {
    fun formattedBuild(zone: ZoneId, locale: Locale): String? =
        buildStamp?.takeIf { it.isNotBlank() }?.let { raw ->
            formatCoreBuildStamp(raw, zone, locale).takeIf { it != raw }
        }

    fun subtitle(zone: ZoneId, locale: Locale, withBuild: (coreName: String, build: String) -> String): String =
        formattedBuild(zone, locale)?.let { withBuild(coreName, it) } ?: coreName
}

fun builtinCoreSummaryOf(coreId: String, installedBuild: String?): BuiltinCoreSummary =
    BuiltinCoreSummary(
        coreName = LibretroCoreRegistry.getCoreById(coreId)?.displayName ?: coreId,
        buildStamp = installedBuild
    )

class BuiltinCoreSummaryLoader @Inject constructor(
    private val builtinCoreResolver: BuiltinCoreResolver,
    private val coreVersionRepository: CoreVersionRepository
) {
    suspend fun load(gameId: Long?, platformId: Long, platformSlug: String): BuiltinCoreSummary? {
        val coreId = builtinCoreResolver.resolveCoreId(gameId, platformId, platformSlug) ?: return null
        return builtinCoreSummaryOf(coreId, coreVersionRepository.installedBuild(coreId))
    }
}

@Composable
fun builtinCoreSubtitle(summary: BuiltinCoreSummary, @StringRes withBuildRes: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    val context = LocalContext.current
    return remember(summary, locale, withBuildRes) {
        summary.subtitle(ZoneId.systemDefault(), locale) { coreName, build ->
            context.getString(withBuildRes, coreName, build)
        }
    }
}
