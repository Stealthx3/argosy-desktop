package com.nendo.argosy.libretro.shader

import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

actual class ShaderChainManager actual constructor(shaderRegistry: ShaderRegistry, shaderDownloader: ShaderDownloader, previewRenderer: ShaderPreviewRenderer, scope: CoroutineScope, previewInputProvider: suspend () -> Bitmap?, onChainChanged: ((ShaderChainConfig) -> Unit)?) {
    actual companion object {}

    actual fun getShaderRegistry(): ShaderRegistry = TODO("desktop: getShaderRegistry")

    actual fun loadChain(chainJson: String) {
    }

    actual fun getChainConfig(): ShaderChainConfig = ShaderChainConfig()

    actual fun addShaderToStack(shaderId: String, displayName: String) {
    }

    actual fun removeShaderFromStack() {
    }

    actual fun liftShader() {
    }

    actual fun moveHeldShader(delta: Int): Boolean = false

    actual fun moveHeldShaderTo(index: Int): Boolean = false

    actual fun dropShader() {
    }

    actual fun cancelShaderLift() {
    }

    actual fun selectShaderInStack(index: Int) {
    }

    actual fun cycleShaderTab(direction: Int) {
    }

    actual fun moveShaderParamFocus(delta: Int) {
    }

    actual fun adjustShaderParam(direction: Int) {
    }

    actual fun resetShaderParam() {
    }

    actual fun showShaderPicker() {
    }

    actual fun dismissShaderPicker() {
    }

    actual fun setShaderPickerFocusIndex(index: Int) {
    }

    actual fun moveShaderPickerFocus(delta: Int) {
    }

    actual fun jumpShaderPickerSection(direction: Int) {
    }

    actual fun confirmShaderPickerSelection() {
    }

    actual fun renderPreview() {
    }

    actual fun debouncedRenderPreview() {
    }

    actual fun destroy() {
    }
}
