package androidx.compose.ui.graphics

/** Compose-Android's Bitmap.asImageBitmap(), for the android.graphics.Bitmap shim. */
fun android.graphics.Bitmap.asImageBitmap(): ImageBitmap = image
