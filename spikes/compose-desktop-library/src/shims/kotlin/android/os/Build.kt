package android.os

/**
 * Desktop answers to upstream's API-level checks. The desktop renderer (Skia) supports what API 31+
 * gates in upstream (RenderEffect blur), so the desktop reports the app's targetSdk.
 */
object Build {
    object VERSION { const val SDK_INT = 35 }
    object VERSION_CODES { const val Q = 29; const val S = 31 }
}
