package com.nendo.argosy.platform.image

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.nendo.argosy.platform.PlatformBitmap

actual typealias PlatformDrawable = Drawable

actual val PlatformDrawable.intrinsicWidth: Int get() = intrinsicWidth

actual val PlatformDrawable.intrinsicHeight: Int get() = intrinsicHeight

actual typealias PlatformBitmapDrawable = BitmapDrawable

actual val PlatformBitmapDrawable.bitmap: PlatformBitmap? get() = bitmap
