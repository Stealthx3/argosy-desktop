package com.nendo.argosy.ui.screens.media.components

import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import com.nendo.argosy.platform.asImageBitmap
import com.nendo.argosy.platform.config
import com.nendo.argosy.platform.copy
import com.nendo.argosy.platform.getPixel
import com.nendo.argosy.platform.height
import com.nendo.argosy.platform.recycle
import com.nendo.argosy.platform.setPixels
import com.nendo.argosy.platform.width
import com.nendo.argosy.platform.image.PlatformBitmapDrawable as BitmapDrawable
import com.nendo.argosy.platform.image.bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.nendo.argosy.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.SolidColor
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
import com.nendo.argosy.platform.image.AsyncImage
import com.nendo.argosy.platform.image.intrinsicHeight
import com.nendo.argosy.platform.image.intrinsicWidth
import com.nendo.argosy.platform.image.ImageRequest
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.ui.common.AlwaysCrossfadeFactory
import com.nendo.argosy.ui.components.boxArtFrame
import com.nendo.argosy.ui.components.boxart.UnbadgedBoxArtBorder
import com.nendo.argosy.ui.theme.generated.ComponentDefaults
import com.nendo.argosy.ui.screens.media.MediaItemUi
import com.nendo.argosy.ui.theme.Dimens
import com.nendo.argosy.ui.theme.LocalArgosyTheme
import com.nendo.argosy.ui.util.clickableNoFocus

/**
 * How many lines a title always occupies, whether or not it needs them.
 *
 * A tile that grows for a long title changes the height of the row it sits in, and in a lazy row
 * that height is recomputed from whatever is currently composed - so titles entering and leaving
 * view make the whole row rise and fall. Reserving the space costs a little air under short titles
 * and buys a row that does not move.
 */
private const val TITLE_LINES = 2

/**
 * One movie or series in the browse grid. Long press mirrors the gamepad's hold-confirm, so the
 * resume prompt is reachable without a controller.
 */
@Composable
fun MediaPosterCard(
    item: MediaItemUi,
    isFocused: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    scaleOverride: Float? = null,
    onPosterLoaded: ((itemId: String, bitmap: Bitmap) -> Unit)? = null
) {
    val theme = LocalArgosyTheme.current
    Column(
        modifier = modifier
            .width(Dimens.mediaPosterWidth)
            .clickableNoFocus(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(Dimens.mediaPosterWidth)
                .height(Dimens.mediaPosterHeight)
                .boxArtFrame(
                    isFocused = isFocused,
                    scaleOverride = scaleOverride,
                    artworkGradient = item.gradientColors,
                    background = SolidColor(theme.surfaceRaised)
                )
        ) {
            val context = LocalContext.current
            val posterRequest = remember(context, item.posterUrl) {
                ImageRequest.Builder(context)
                    .data(item.posterUrl)
                    .transitionFactory(AlwaysCrossfadeFactory(ComponentDefaults.MediaCover.crossfadeMs))
                    .build()
            }
            AsyncImage(
                model = posterRequest,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onSuccess = { state ->
                    val bitmap = (state.result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) onPosterLoaded?.invoke(item.itemId, bitmap)
                }
            )
            if (item.posterUrl.isBlank()) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = theme.textMute,
                    modifier = Modifier.size(Dimens.iconLg).align(Alignment.Center)
                )
            }
            UnbadgedBoxArtBorder(
                imageModel = item.posterUrl.takeIf { it.isNotBlank() },
                gradientColors = item.gradientColors,
                isFocused = isFocused
            )
            MediaTileBadges(
                item = item,
                modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.spacingXs)
            )
            if (item.progressFraction > 0f) {
                MediaProgressBar(
                    fraction = item.progressFraction,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        Spacer(modifier = Modifier.height(Dimens.spacingXs))
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (isFocused) theme.textPrimary else theme.textDim,
            minLines = TITLE_LINES,
            maxLines = TITLE_LINES,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MediaTileBadges(item: MediaItemUi, modifier: Modifier = Modifier) {
    val theme = LocalArgosyTheme.current
    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        if (item.played) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.media_poster_card_watched),
                tint = theme.focusAccent,
                modifier = Modifier.size(Dimens.iconSm)
            )
        }
        MediaDownloadBadge(availability = item.availability, size = Dimens.iconSm)
    }
}

@Composable
fun MediaProgressBar(fraction: Float, modifier: Modifier = Modifier) {
    val theme = LocalArgosyTheme.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.borderThick)
            .background(theme.hairlineLow)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(Dimens.borderThick)
                .background(theme.focusAccent)
        )
    }
}
