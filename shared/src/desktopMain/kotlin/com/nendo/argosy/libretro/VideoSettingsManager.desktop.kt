package com.nendo.argosy.libretro

import com.nendo.argosy.core.emulator.LibretroSettingDef
import com.nendo.argosy.data.preferences.BuiltinEmulatorSettings
import javax.inject.Inject
import com.swordfish.libretrodroid.ShaderConfig

actual class VideoSettingsManager @Inject constructor() {
    actual companion object {
        actual fun parseRotation(value: String): Int = 0

        actual fun parseOverscan(value: String): Int = 0
    }

    actual var aspectRatioMode: String = ""

    actual var fastForwardEnabled: Boolean = false

    actual var fastForwardSpeed: Int = 0

    actual var fastForwardMode: com.nendo.argosy.data.local.entity.FastForwardMode = TODO("desktop: fastForwardMode")

    actual var fastForwardPreservePitch: Boolean = false

    actual var overscanCrop: Int = 0

    actual var rotationDegrees: Int = 0

    actual var rewindEnabled: Boolean = false

    actual var onRewindToggled: ((enabled: Boolean) -> Unit)? = null

    actual var onRewindConfigChanged: (() -> Unit)? = null

    actual var onPortraitPositionChanged: ((String) -> Unit)? = null

    actual fun applySettings(settings: BuiltinEmulatorSettings) {
    }

    actual fun resolveCustomShader(settings: BuiltinEmulatorSettings) {
    }

    actual fun setScreenSize(width: Int, height: Int) {
    }

    actual fun getVideoSettingValue(setting: LibretroSettingDef): String = ""

    actual fun getGlobalVideoSettingValue(setting: LibretroSettingDef): String = ""

    actual var framesSuppressed: Boolean = false

    actual fun isFrameAdjustable(frameId: String?): Boolean = false

    actual fun adjustFrame(frameId: String?, offsetX: Float, offsetY: Float, zoom: Float) {
    }

    actual fun resetFrameAdjustment(frameId: String?) {
    }

    actual fun persistFrameAdjustment() {
    }

    actual fun applyFrame(frameId: String?) {
    }

    actual fun resetVideoSetting(setting: LibretroSettingDef) {
    }

    actual fun cycleVideoSetting(setting: LibretroSettingDef, direction: Int) {
    }

    actual fun toggleVideoSetting(setting: LibretroSettingDef) {
    }

    actual fun applyVideoSettingChange(setting: LibretroSettingDef, value: String) {
    }

    actual fun applyAspectRatio() {
    }

    actual fun applyOverscanCrop() {
    }

    actual fun applyRotation() {
    }

    actual fun persistVideoSetting(setting: LibretroSettingDef, value: String) {
    }

    actual fun persistControlSetting(field: String, value: Boolean) {
    }

    actual fun persistShaderChain(json: String) {
    }

    actual fun persistFrame(frameId: String?) {
    }

    actual var resolvedCustomShader: ShaderConfig = TODO("desktop: resolvedCustomShader")
}
