package com.nendo.argosy.libretro

import com.nendo.argosy.data.preferences.BuiltinEmulatorSettings
import com.nendo.argosy.core.emulator.LibretroSettingDef
import com.swordfish.libretrodroid.ShaderConfig

expect class VideoSettingsManager {
    companion object {
        fun parseRotation(value: String): Int

        fun parseOverscan(value: String): Int
    }

    var resolvedCustomShader: ShaderConfig

    var aspectRatioMode: String

    var fastForwardEnabled: Boolean

    var fastForwardSpeed: Int

    var fastForwardMode: com.nendo.argosy.data.local.entity.FastForwardMode

    var fastForwardPreservePitch: Boolean

    var overscanCrop: Int

    var rotationDegrees: Int

    var rewindEnabled: Boolean

    var onRewindToggled: ((enabled: Boolean) -> Unit)?

    var onRewindConfigChanged: (() -> Unit)?

    var onPortraitPositionChanged: ((String) -> Unit)?

    fun applySettings(settings: BuiltinEmulatorSettings)

    fun resolveCustomShader(settings: BuiltinEmulatorSettings)

    fun setScreenSize(width: Int, height: Int)

    fun getVideoSettingValue(setting: LibretroSettingDef): String

    fun getGlobalVideoSettingValue(setting: LibretroSettingDef): String

    var framesSuppressed: Boolean

    fun isFrameAdjustable(frameId: String?): Boolean

    fun adjustFrame(frameId: String?, offsetX: Float, offsetY: Float, zoom: Float)

    fun resetFrameAdjustment(frameId: String?)

    fun persistFrameAdjustment()

    fun applyFrame(frameId: String?)

    fun resetVideoSetting(setting: LibretroSettingDef)

    fun cycleVideoSetting(setting: LibretroSettingDef, direction: Int)

    fun toggleVideoSetting(setting: LibretroSettingDef)

    fun applyVideoSettingChange(setting: LibretroSettingDef, value: String)

    fun applyAspectRatio()

    fun applyOverscanCrop()

    fun applyRotation()

    fun persistVideoSetting(setting: LibretroSettingDef, value: String)

    fun persistControlSetting(field: String, value: Boolean)

    fun persistShaderChain(json: String)

    fun persistFrame(frameId: String?)
}
