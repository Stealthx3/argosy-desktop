package com.nendo.argosy.libretro.shader

import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import com.nendo.argosy.platform.asImageBitmap
import com.nendo.argosy.platform.config
import com.nendo.argosy.platform.copy
import com.nendo.argosy.platform.getPixel
import com.nendo.argosy.platform.height
import com.nendo.argosy.platform.recycle
import com.nendo.argosy.platform.setPixels
import com.nendo.argosy.platform.width
import com.swordfish.libretrodroid.ShaderConfig

expect class ShaderPreviewRenderer {
    companion object {}

    suspend fun render(inputBitmap: Bitmap, shaderConfig: ShaderConfig.Custom, passParams: List<Map<String, Float>> = emptyList(), outputWidth: Int, outputHeight: Int): Bitmap?

    fun destroy()
}
