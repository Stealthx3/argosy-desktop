package com.nendo.argosy.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.nendo.argosy.ui.theme.generated.ComponentDefaults
import com.nendo.argosy.platform.LocalPlatformContext as LocalContext
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalUiScale
import com.nendo.argosy.ui.theme.generated.TypographyTokens
import com.nendo.argosy.util.formatClockTime
import kotlinx.coroutines.delay

/**
 * Whether artwork is still being fetched, for the status bar to show without every surface that
 * draws one having to be told. The bar appears on both displays and in the drawer; a parameter
 * would mean each caller remembering to pass it, which is how the companion screen ends up missing
 * what the main one shows.
 */
val LocalArtworkScraping = androidx.compose.runtime.compositionLocalOf { false }

data class StatusBarItems(
    val clock: Boolean = true,
    val battery: Boolean = true,
    val network: Boolean = false
)

val LocalStatusBarItems = androidx.compose.runtime.compositionLocalOf { StatusBarItems() }

val LocalTransientHomeStatusVisible = androidx.compose.runtime.compositionLocalOf { false }

fun statusBarItemsOf(prefs: com.nendo.argosy.data.preferences.UserPreferences) = StatusBarItems(
    clock = prefs.showStatusClock,
    battery = prefs.showStatusBattery,
    network = prefs.showStatusNetwork
)

/**
 * Provides [LocalStatusBarItems] from [preferences] for a surface that renders outside ArgosyApp,
 * so its status bar follows the Interface toggles as they change.
 */
@Composable
fun ProvideStatusBarItems(
    preferences: kotlinx.coroutines.flow.Flow<com.nendo.argosy.data.preferences.UserPreferences>,
    content: @Composable () -> Unit
) {
    val prefs by preferences.collectAsState(initial = null)
    val items = prefs?.let(::statusBarItemsOf) ?: StatusBarItems()
    androidx.compose.runtime.CompositionLocalProvider(LocalStatusBarItems provides items, content = content)
}

/**
 * [scrim] backs the bar with a plate so it reads over artwork. Turn it off on a flat surface,
 * where the plate has nothing to separate the bar from and only shows as a panel of its own.
 * [allowWrap] places network and battery below the clock when network status is shown.
 */
@Composable
fun SystemStatusBar(
    modifier: Modifier = Modifier,
    contentColor: Color = Color.Unspecified,
    scrim: Boolean = true,
    allowWrap: Boolean = false
) {
    val isScrapingArtwork = LocalArtworkScraping.current
    val shown = LocalStatusBarItems.current
    if (!hasStatusBarContent(shown, isScrapingArtwork)) return
    val effectiveColor = if (contentColor == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurface
    } else {
        contentColor
    }
    val currentTime = remember { mutableLongStateOf(System.currentTimeMillis()) }
    val batteryState by rememberBatteryState()

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L - (System.currentTimeMillis() % 60_000L))
            currentTime.longValue = System.currentTimeMillis()
        }
    }

    val statusModifier = modifier
        .then(if (scrim) Modifier.heightIn(min = frostedVisualChromeHeight).frostedSurface() else Modifier)
        .padding(
            horizontal = if (scrim) {
                ComponentDefaults.FrostedSurface.statusPaddingHorizontalDp.dp * LocalUiScale.current.scale
            } else ComponentDefaults.OverlayLegibility.scrimPaddingHorizDp.dp,
            vertical = if (scrim) Dimens.spacingSm else ComponentDefaults.OverlayLegibility.scrimPaddingVertDp.dp
        )
    val content: @Composable (Modifier, Modifier) -> Unit = { clockModifier, itemModifier ->
        if (isScrapingArtwork) {
            ArtworkScrapeIndicator(color = effectiveColor, modifier = itemModifier)
        }

        if (shown.clock) {
            Text(
                text = formatClockTime(LocalContext.current, currentTime.longValue),
                modifier = clockModifier,
                style = chromeTextStyle(
                    MaterialTheme.typography.titleMedium,
                    TypographyTokens.titleMedium,
                    ComponentDefaults.FrostedSurface.chromeFontSizeSp.sp
                ),
                color = effectiveColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

    }
    val indicators: @Composable (Modifier) -> Unit = { itemModifier ->
        if (shown.network || shown.battery) {
            Row(
                modifier = itemModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                if (shown.network) {
                    NetworkIndicator(color = effectiveColor)
                }
                if (shown.battery) {
                    BatteryIndicator(
                        level = batteryState.level,
                        isCharging = batteryState.isCharging,
                        color = effectiveColor
                    )
                }
            }
        }
    }
    if (allowWrap && shown.network) {
        Column(
            modifier = statusModifier,
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
        ) {
            if (shown.clock || isScrapingArtwork) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    content(Modifier.weight(1f, fill = false), Modifier)
                }
            }
            indicators(Modifier)
        }
    } else {
        Row(
            modifier = statusModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
        ) {
            content(Modifier.weight(1f, fill = false), Modifier)
            indicators(Modifier)
        }
    }
}

internal fun hasStatusBarContent(items: StatusBarItems, scrapingArtwork: Boolean): Boolean =
    items.clock || items.battery || items.network || scrapingArtwork

/**
 * Says that artwork is still being fetched, without saying what. It pulses so it reads as work in
 * progress rather than a warning; the detail lives in Settings > Sync for anyone who wants it.
 */
@Composable
private fun ArtworkScrapeIndicator(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "artworkScrape")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "artworkScrapeAlpha"
    )
    Icon(
        imageVector = Icons.Outlined.Image,
        contentDescription = null,
        tint = color.copy(alpha = alpha),
        modifier = modifier.size(Dimens.iconSm)
    )
}

@Composable
private fun NetworkIndicator(color: Color, modifier: Modifier = Modifier) {
    val link by rememberNetworkLink()
    val icon = when (link) {
        NetworkLink.OFFLINE -> Icons.Outlined.WifiOff
        NetworkLink.WIFI -> Icons.Outlined.Wifi
        NetworkLink.ETHERNET -> Icons.Outlined.SettingsEthernet
        NetworkLink.CELLULAR -> Icons.Outlined.SignalCellularAlt
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = modifier.size(Dimens.iconSm)
    )
}

@Composable
private fun BatteryIndicator(
    level: Int,
    isCharging: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingXs)
    ) {
        BatteryIcon(
            level = level,
            isCharging = isCharging,
            color = color,
            modifier = Modifier.size(width = Dimens.iconMd, height = Dimens.radiusLg)
        )
        Text(
            text = "$level%",
            style = chromeTextStyle(
                MaterialTheme.typography.titleMedium,
                TypographyTokens.titleMedium,
                ComponentDefaults.FrostedSurface.chromeFontSizeSp.sp *
                    ComponentDefaults.FrostedSurface.statusBatteryFontSizeRatio
            ),
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}
