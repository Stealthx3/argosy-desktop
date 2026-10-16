package androidx.compose.ui.platform

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.staticCompositionLocalOf

/** Android-only composition locals, provided on the desktop by spike.DesktopAndroidLocals. */
val LocalContext = staticCompositionLocalOf<Context> { error("LocalContext not provided") }
val LocalConfiguration = staticCompositionLocalOf<Configuration> { error("LocalConfiguration not provided") }
val LocalView = staticCompositionLocalOf<View> { error("LocalView not provided") }

/** The one android.view.View call upstream makes from composables. The desktop has a single root view at 0,0. */
class View {
    fun getLocationOnScreen(outLocation: IntArray) { outLocation[0] = 0; outLocation[1] = 0 }
}
