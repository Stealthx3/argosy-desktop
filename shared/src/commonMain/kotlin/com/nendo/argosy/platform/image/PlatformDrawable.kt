package com.nendo.argosy.platform.image

import com.nendo.argosy.platform.PlatformBitmap
import com.nendo.argosy.platform.asImageBitmap
import com.nendo.argosy.platform.config
import com.nendo.argosy.platform.copy
import com.nendo.argosy.platform.getPixel
import com.nendo.argosy.platform.height
import com.nendo.argosy.platform.recycle
import com.nendo.argosy.platform.setPixels
import com.nendo.argosy.platform.width

expect abstract class PlatformDrawable

expect val PlatformDrawable.intrinsicWidth: Int

expect val PlatformDrawable.intrinsicHeight: Int

expect open class PlatformBitmapDrawable : PlatformDrawable

expect val PlatformBitmapDrawable.bitmap: PlatformBitmap?

class AsyncImageSuccess(val result: AsyncImageSuccessResult)

class AsyncImageSuccessResult(val drawable: PlatformDrawable)
