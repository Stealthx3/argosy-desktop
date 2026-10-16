package coil.compose

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadSvgPainter
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image as SkiaImage

/**
 * Coil 2's AsyncImage, for the models upstream passes: a File (cached art), or a
 * file:///android_asset/... URI (bundled platform SVGs). Loads off the main thread, as Coil does.
 */
@Composable
fun AsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null,
    onError: ((AsyncImagePainter.State.Error) -> Unit)? = null,
) {
    val density = LocalDensity.current
    val result = produceState<Result<Painter>?>(null, model) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                when {
                    model is File -> BitmapPainter(SkiaImage.makeFromEncoded(model.readBytes()).toComposeImageBitmap())
                    model is String && model.startsWith(ASSET_PREFIX) -> {
                        val path = "/assets/" + model.removePrefix(ASSET_PREFIX)
                        AsyncImagePainter::class.java.getResourceAsStream(path)!!.use { loadSvgPainter(it, density) }
                    }
                    else -> error("unsupported image model: $model")
                }
            }
        }
    }.value ?: return
    result.onSuccess { Image(it, contentDescription, modifier, alignment, contentScale, alpha, colorFilter) }
    result.onFailure { LaunchedEffect(model) { onError?.invoke(AsyncImagePainter.State.Error()) } }
}

private const val ASSET_PREFIX = "file:///android_asset/"

/** Type-only stand-in so upstream's onError lambdas compile. */
class AsyncImagePainter {
    sealed class State {
        class Error : State()
    }
}
