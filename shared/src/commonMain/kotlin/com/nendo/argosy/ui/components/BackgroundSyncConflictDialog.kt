package com.nendo.argosy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import com.nendo.argosy.platform.LocalPlatformContext as LocalContext
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nendo.argosy.platform.PlatformContext as Context
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
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.data.sync.ConflictInfo
import com.nendo.argosy.domain.model.SnapshotConflictChoice
import com.nendo.argosy.ui.common.labelRes
import com.nendo.argosy.ui.common.subtitleRes
import com.nendo.argosy.ui.primitives.FocusIndicators
import com.nendo.argosy.ui.primitives.argosyFocusIndicators
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalArgosyTheme
import com.nendo.argosy.ui.util.clickableNoFocus
import java.time.Duration
import java.time.Instant

@Composable
fun BackgroundSyncConflictDialog(
    conflictInfo: ConflictInfo,
    focusIndex: Int,
    onKeepLocal: () -> Unit,
    onKeepServer: () -> Unit,
    onSkip: () -> Unit,
    snapshotConflict: Boolean = false,
    onSnapshotChoice: (SnapshotConflictChoice) -> Unit = {}
) {
    val context = LocalContext.current
    val localTimeStr = conflictInfo.localTimestamp.toRelativeString(context)
    val serverTimeStr = conflictInfo.serverTimestamp.toRelativeString(context)
    val localIsNewer = conflictInfo.localTimestamp.isAfter(conflictInfo.serverTimestamp)

    Modal(
        title = stringResource(R.string.ui_background_conflict_title),
        baseWidth = 400.dp,
        onDismiss = onSkip,
        titleContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                GameTitle(
                    title = conflictInfo.gameName,
                    titleStyle = MaterialTheme.typography.titleMedium,
                    titleColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(Dimens.spacingMd))
                Text(
                    text = conflictInfo.channelName
                        ?: stringResource(R.string.ui_background_conflict_default_slot),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    ) {
        Text(
            text = if (conflictInfo.isHashConflict) {
                stringResource(R.string.ui_background_conflict_message_local_changed)
            } else {
                stringResource(R.string.ui_background_conflict_message_server_newer)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(Dimens.spacingMd))

        if (snapshotConflict) Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs)) {
            SnapshotConflictChoice.entries.forEachIndexed { index, choice ->
                ConflictChoiceRow(
                    label = stringResource(choice.labelRes),
                    subtitle = stringResource(choice.subtitleRes),
                    isFocused = focusIndex == index,
                    onClick = { onSnapshotChoice(choice) }
                )
            }
            ConflictChoiceRow(
                label = stringResource(R.string.ui_background_conflict_choice_skip),
                subtitle = stringResource(R.string.ui_background_conflict_choice_skip_subtitle),
                isFocused = focusIndex == SnapshotConflictChoice.entries.size,
                onClick = onSkip
            )
        } else Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingXs)) {
            ConflictChoiceRow(
                icon = Icons.Default.PhoneAndroid,
                label = stringResource(R.string.ui_background_conflict_choice_local),
                timestamp = localTimeStr,
                isNewer = localIsNewer,
                isFocused = focusIndex == 0,
                onClick = onKeepLocal
            )
            ConflictChoiceRow(
                icon = Icons.Default.Cloud,
                label = stringResource(R.string.ui_background_conflict_choice_server),
                subtitle = conflictInfo.serverDeviceName,
                timestamp = serverTimeStr,
                isNewer = !localIsNewer,
                isFocused = focusIndex == 1,
                onClick = onKeepServer
            )
            ConflictChoiceRow(
                label = stringResource(R.string.ui_background_conflict_choice_skip),
                subtitle = stringResource(R.string.ui_background_conflict_choice_skip_subtitle),
                isFocused = focusIndex == 2,
                onClick = onSkip
            )
        }
    }
}

@Composable
internal fun ConflictChoiceRow(
    label: String,
    isFocused: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    subtitle: String? = null,
    timestamp: String? = null,
    isNewer: Boolean = false
) {
    val theme = LocalArgosyTheme.current
    val shape = RoundedCornerShape(Dimens.radiusControl)
    val labelColor = when {
        isFocused -> lerp(theme.focusAccent, Color.White, 0.45f)
        isNewer -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val metaColor = when {
        isFocused -> lerp(theme.focusAccent, Color.White, 0.45f).copy(alpha = 0.65f)
        isNewer -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .argosyFocusIndicators(
                focused = isFocused,
                indicators = FocusIndicators.NavRow,
                shape = shape
            )
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickableNoFocus(onClick = onClick)
            .padding(horizontal = Dimens.spacingMd, vertical = Dimens.spacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = labelColor,
                modifier = Modifier.size(Dimens.iconMd)
            )
            Spacer(modifier = Modifier.width(Dimens.spacingSm))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isNewer) FontWeight.Bold else FontWeight.Normal,
                color = labelColor
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = metaColor
                )
            }
        }
        if (timestamp != null) {
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = metaColor
            )
        }
    }
}

private fun Instant.toRelativeString(context: Context): String {
    val now = Instant.now()
    val duration = Duration.between(this, now)
    val resources = context.resources
    return when {
        duration.isNegative -> context.getString(R.string.ui_background_conflict_age_future)
        duration.toMinutes() < 1 -> context.getString(R.string.ui_background_conflict_age_now)
        duration.toHours() < 1 -> resources.getQuantityString(
            R.plurals.ui_background_conflict_age_minutes,
            duration.toMinutes().toInt(),
            duration.toMinutes().toInt()
        )
        duration.toDays() < 1 -> resources.getQuantityString(
            R.plurals.ui_background_conflict_age_hours,
            duration.toHours().toInt(),
            duration.toHours().toInt()
        )
        duration.toDays() < 30 -> resources.getQuantityString(
            R.plurals.ui_background_conflict_age_days,
            duration.toDays().toInt(),
            duration.toDays().toInt()
        )
        else -> resources.getQuantityString(
            R.plurals.ui_background_conflict_age_months,
            (duration.toDays() / 30).toInt(),
            (duration.toDays() / 30).toInt()
        )
    }
}
