package com.nendo.argosy.ui.input

import com.nendo.argosy.core.input.SoundConfig
import com.nendo.argosy.core.input.SoundType
import com.nendo.argosy.platform.DesktopDirectories
import com.nendo.argosy.platform.audio.PcmClip
import com.nendo.argosy.platform.audio.PcmDecoder
import com.nendo.argosy.platform.audio.PulseOutput
import com.nendo.argosy.shared.resources.AndroidResourceIds
import com.nendo.argosy.shared.resources.Res
import com.nendo.argosy.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class SoundFeedbackManager @Inject constructor() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadLock = Mutex()
    private val players = (Executors.newFixedThreadPool(MAX_STREAMS) { runnable ->
        Thread(runnable, "argosy-ui-sound").apply { isDaemon = true }
    } as ThreadPoolExecutor).apply { setKeepAliveTime(IDLE_SECONDS, TimeUnit.SECONDS); allowCoreThreadTimeOut(true) }

    @Volatile
    private var enabled = false

    @Volatile
    private var volume = DEFAULT_VOLUME

    @Volatile
    private var soundConfigs: Map<SoundType, SoundConfig> = emptyMap()

    @Volatile
    private var lastBoundaryPlayMs = 0L

    private val presetClips = ConcurrentHashMap<SoundPreset, PcmClip>()
    private val customClips = ConcurrentHashMap<SoundType, Pair<String, PcmClip>>()

    actual fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        if (enabled) preload() else release()
    }

    actual fun setVolume(volume: Int) {
        val linear = (volume / VOLUME_SCALE).coerceIn(0f, 1f)
        this.volume = linear * linear
    }

    actual fun setSoundConfigs(configs: Map<SoundType, SoundConfig>) {
        soundConfigs = configs
        refreshCustomSounds()
    }

    actual fun setSoundConfig(type: SoundType, config: SoundConfig) {
        soundConfigs = soundConfigs + (type to config)
        refreshCustomSounds()
    }

    actual fun clearSoundConfig(type: SoundType) {
        soundConfigs = soundConfigs - type
        refreshCustomSounds()
    }

    actual fun play(type: SoundType) {
        playSound(type)
    }

    actual fun playSound(type: SoundType) {
        if (!enabled || type == SoundType.SILENT) return
        val config = soundConfigs[type]
        if (config?.presetName == SoundPreset.SILENT.name) return
        if (type == SoundType.BOUNDARY) {
            val now = System.currentTimeMillis()
            if (now - lastBoundaryPlayMs < BOUNDARY_RATE_LIMIT_MS) return
            lastBoundaryPlayMs = now
        }
        val custom = config?.customFilePath?.let { path -> customClips[type]?.takeIf { it.first == path }?.second }
        if (custom != null) {
            submit(custom, type.name)
            return
        }
        val preset = config?.presetName
            ?.let { name -> SoundPreset.entries.find { it.name == name } }
            ?.takeIf { it.resourceId != null }
            ?: SoundDefaults.presetFor(type)
            ?: return
        val clip = presetClips[preset]
        if (clip == null) preload() else submit(clip, preset.name)
    }

    actual suspend fun clearSfxCache() = withContext(Dispatchers.IO) {
        cacheDir.deleteRecursively()
        Unit
    }

    private fun submit(clip: PcmClip, name: String) {
        if (players.activeCount >= MAX_STREAMS) return
        val level = volume
        players.execute {
            runCatching { PulseOutput.play(clip, level, name) }
                .onFailure { Logger.warn(TAG, "UI sound $name failed", it) }
        }
    }

    private fun preload() {
        scope.launch {
            loadLock.withLock {
                if (!enabled || !PulseOutput.available) return@withLock
                SoundPreset.entries.forEach { preset ->
                    val id = preset.resourceId ?: return@forEach
                    if (presetClips.containsKey(preset)) return@forEach
                    decodeResource(id)?.let { presetClips[preset] = it }
                        ?: Logger.warn(TAG, "UI sound ${preset.name} did not decode")
                }
                syncCustomSounds()
            }
        }
    }

    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    private suspend fun decodeResource(id: Int): PcmClip? = runCatching {
        val path = AndroidResourceIds.rawPath(id)
        val file = File(cacheDir, path.substringAfterLast('/'))
        if (!file.exists()) {
            cacheDir.mkdirs()
            file.writeBytes(Res.readBytes(path))
        }
        PcmDecoder.decode(file)
    }.getOrNull()

    private fun refreshCustomSounds() {
        scope.launch { loadLock.withLock { syncCustomSounds() } }
    }

    private fun syncCustomSounds() {
        val desired = soundConfigs.mapNotNull { (type, config) -> config.customFilePath?.let { type to it } }.toMap()
        customClips.keys.filterNot { it in desired }.forEach { customClips.remove(it) }
        desired.forEach { (type, path) ->
            if (customClips[type]?.first == path) return@forEach
            val clip = File(path).takeIf { it.exists() }?.let { runCatching { PcmDecoder.decode(it) }.getOrNull() }
            if (clip == null) {
                customClips.remove(type)
                Logger.warn(TAG, "Custom sound for ${type.name} is not Ogg Vorbis or PCM WAV: $path")
            } else {
                customClips[type] = path to clip
            }
        }
    }

    private fun release() {
        presetClips.clear()
        customClips.clear()
    }

    private val cacheDir: File get() = DesktopDirectories.cache.resolve("sfx")

    private companion object {
        const val TAG = "SoundFeedback"
        const val MAX_STREAMS = 4
        const val IDLE_SECONDS = 30L
        const val DEFAULT_VOLUME = 0.4f
        const val VOLUME_SCALE = 100f
        const val BOUNDARY_RATE_LIMIT_MS = 500L
    }
}

/**
 * The preset each app event plays when the user has not chosen one; the same table Android uses.
 */
object SoundDefaults {
    private val presets = mapOf(
        SoundType.NAVIGATE to SoundPreset.TAP_LIGHT,
        SoundType.BOUNDARY to SoundPreset.BUZZ_ERROR,
        SoundType.SECTION_CHANGE to SoundPreset.HOVER_SOFT,
        SoundType.SELECT to SoundPreset.POP_CONFIRM,
        SoundType.BACK to SoundPreset.SWIPE_BACK,
        SoundType.OPEN_MODAL to SoundPreset.CHIME_OPEN,
        SoundType.CLOSE_MODAL to SoundPreset.POP_CLOSE,
        SoundType.FAVORITE to SoundPreset.DING_PICKUP,
        SoundType.UNFAVORITE to SoundPreset.TICK_ACCEPT,
        SoundType.DOWNLOAD_START to SoundPreset.NOTIFY_START,
        SoundType.DOWNLOAD_COMPLETE to SoundPreset.CHIME_SUCCESS,
        SoundType.DOWNLOAD_CANCEL to SoundPreset.DISMISS_FAIL,
        SoundType.ERROR to SoundPreset.BUZZ_ERROR,
        SoundType.VOLUME_PREVIEW to SoundPreset.CLICK_SOFT,
        SoundType.TOGGLE to SoundPreset.CLICK_SOFT,
        SoundType.LAUNCH_GAME to SoundPreset.COLLECT
    )

    fun presetFor(type: SoundType): SoundPreset? = presets[type]
}
