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
import kotlinx.coroutines.CoroutineScope

expect class ShaderChainManager(shaderRegistry: ShaderRegistry, shaderDownloader: ShaderDownloader, previewRenderer: ShaderPreviewRenderer, scope: CoroutineScope, previewInputProvider: suspend () -> Bitmap?, onChainChanged: ((ShaderChainConfig) -> Unit)? = null) {
    companion object {}

    fun getShaderRegistry(): ShaderRegistry

    fun loadChain(chainJson: String)

    fun getChainConfig(): ShaderChainConfig

    fun addShaderToStack(shaderId: String, displayName: String)

    fun removeShaderFromStack()

    fun liftShader()

    fun moveHeldShader(delta: Int): Boolean

    fun moveHeldShaderTo(index: Int): Boolean

    fun dropShader()

    fun cancelShaderLift()

    fun selectShaderInStack(index: Int)

    fun cycleShaderTab(direction: Int)

    fun moveShaderParamFocus(delta: Int)

    fun adjustShaderParam(direction: Int)

    fun resetShaderParam()

    fun showShaderPicker()

    fun dismissShaderPicker()

    fun setShaderPickerFocusIndex(index: Int)

    fun moveShaderPickerFocus(delta: Int)

    fun jumpShaderPickerSection(direction: Int)

    fun confirmShaderPickerSelection()

    fun renderPreview()

    fun debouncedRenderPreview()

    fun destroy()
}
