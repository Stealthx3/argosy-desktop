package com.nendo.argosy.platform.mpv

import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.StringArray
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * One libmpv player. With [video] it renders frames with mpv's software renderer into two Skia
 * bitmaps on its own thread: [frame] counts finished frames and [drawFront] hands the latest one to
 * the UI; without it only audio plays. Every call after [release] does nothing.
 */
class MpvPlayer private constructor(private val mpv: LibMpv, private val handle: Pointer, private val video: Boolean) {
    private val lock = Any()
    private val wake = Semaphore(0)
    private val updateCallback = LibMpv.UpdateCallback { wake.release() }
    private var renderContext: Pointer? = null
    private var front: Bitmap? = null
    private var back: Bitmap? = null
    private var thread: Thread? = null

    @Volatile private var released = false
    @Volatile private var width = 0
    @Volatile private var height = 0

    private val _frame = MutableStateFlow(0L)
    val frame: StateFlow<Long> = _frame.asStateFlow()

    fun load(path: String, startMs: Long = 0L) {
        if (released) return
        if (startMs > 0) mpv.mpv_set_property_string(handle, "start", "%.3f".format(java.util.Locale.ROOT, startMs / 1000.0))
        mpv.mpv_command(handle, StringArray(arrayOf("loadfile", path)))
    }

    fun setPaused(paused: Boolean) = setProperty("pause", if (paused) "yes" else "no")

    fun setMuted(muted: Boolean) = setProperty("mute", if (muted) "yes" else "no")

    fun setVolume(percent: Int) = setProperty("volume", percent.coerceIn(0, 100).toString())

    fun positionMs(): Long {
        if (released) return 0L
        val value = mpv.mpv_get_property_string(handle, "time-pos") ?: return 0L
        val seconds = value.getString(0).toDoubleOrNull()
        mpv.mpv_free(value)
        return ((seconds ?: 0.0) * 1000).toLong()
    }

    fun resize(newWidth: Int, newHeight: Int) {
        width = newWidth.coerceAtLeast(0)
        height = newHeight.coerceAtLeast(0)
        wake.release()
    }

    fun drawFront(draw: (Bitmap) -> Unit) {
        synchronized(lock) { front?.let(draw) }
    }

    fun release() {
        if (released) return
        released = true
        wake.release()
        thread?.join(JOIN_TIMEOUT_MS)
        renderContext?.let {
            mpv.mpv_render_context_set_update_callback(it, null, null)
            mpv.mpv_render_context_free(it)
        }
        mpv.mpv_terminate_destroy(handle)
        synchronized(lock) {
            front?.close()
            back?.close()
            front = null
            back = null
        }
    }

    private fun setProperty(name: String, value: String) {
        if (!released) mpv.mpv_set_property_string(handle, name, value)
    }

    private fun startRendering(): Boolean {
        val apiType = Memory(RENDER_API_TYPE_BYTES).apply { setString(0, LibMpv.RENDER_API_TYPE_SW) }
        val params = renderParams(LibMpv.RENDER_PARAM_API_TYPE to apiType)
        val result = PointerByReference()
        if (mpv.mpv_render_context_create(result, handle, params) < 0) return false
        val context = result.value
        renderContext = context
        mpv.mpv_render_context_set_update_callback(context, updateCallback, null)
        thread = Thread({ renderLoop(context) }, "mpv-render").apply {
            isDaemon = true
            start()
        }
        return true
    }

    private fun renderLoop(context: Pointer) {
        val format = Memory(FORMAT_BYTES).apply { setString(0, PIXEL_FORMAT) }
        val size = Memory(SIZE_BYTES)
        val stride = Memory(STRIDE_BYTES)
        while (!released) {
            wake.tryAcquire(IDLE_WAIT_MS, TimeUnit.MILLISECONDS)
            if (released) break
            val flags = mpv.mpv_render_context_update(context)
            val w = width
            val h = height
            if (flags and LibMpv.RENDER_UPDATE_FRAME == 0L || w <= 0 || h <= 0) continue
            val target = backBitmap(w, h)
            val pixels = target.peekPixels() ?: continue
            size.setInt(0, w)
            size.setInt(INT_BYTES, h)
            stride.setLong(0, target.rowBytes.toLong())
            val params = renderParams(
                LibMpv.RENDER_PARAM_SW_SIZE to size,
                LibMpv.RENDER_PARAM_SW_FORMAT to format,
                LibMpv.RENDER_PARAM_SW_STRIDE to stride,
                LibMpv.RENDER_PARAM_SW_POINTER to Pointer(pixels.addr),
            )
            if (mpv.mpv_render_context_render(context, params) < 0) continue
            target.notifyPixelsChanged()
            synchronized(lock) {
                back = front
                front = target
            }
            _frame.value = _frame.value + 1
        }
    }

    private fun backBitmap(w: Int, h: Int): Bitmap {
        val current = synchronized(lock) { back.also { back = null } }
        if (current != null && current.width == w && current.height == h) return current
        current?.close()
        return Bitmap().apply { allocPixels(ImageInfo(w, h, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)) }
    }

    private fun renderParams(vararg entries: Pair<Int, Pointer>): Memory {
        val memory = Memory(PARAM_BYTES * (entries.size + 1).toLong())
        memory.clear()
        entries.forEachIndexed { index, (type, data) ->
            memory.setInt(index * PARAM_BYTES, type)
            memory.setPointer(index * PARAM_BYTES + POINTER_OFFSET, data)
        }
        memory.setInt(entries.size * PARAM_BYTES, LibMpv.RENDER_PARAM_INVALID)
        return memory
    }

    companion object {
        private const val PIXEL_FORMAT = "bgr0"
        private const val PARAM_BYTES = 16L
        private const val POINTER_OFFSET = 8L
        private const val INT_BYTES = 4L
        private const val SIZE_BYTES = 8L
        private const val STRIDE_BYTES = 8L
        private const val FORMAT_BYTES = 8L
        private const val RENDER_API_TYPE_BYTES = 8L
        private const val IDLE_WAIT_MS = 250L
        private const val JOIN_TIMEOUT_MS = 1000L

        val isAvailable: Boolean get() = LibMpv.instance != null

        fun create(video: Boolean, muted: Boolean): MpvPlayer? {
            val mpv = LibMpv.instance ?: return null
            val handle = mpv.mpv_create() ?: return null
            val options = buildList {
                add("terminal" to "no")
                add("input-default-bindings" to "no")
                add("loop-file" to "inf")
                add("hwdec" to "no")
                add("mute" to if (muted) "yes" else "no")
                if (video) {
                    add("vo" to "libmpv")
                    add("panscan" to "1.0")
                } else {
                    add("vid" to "no")
                }
            }
            options.forEach { (name, value) -> mpv.mpv_set_option_string(handle, name, value) }
            if (mpv.mpv_initialize(handle) < 0) {
                mpv.mpv_terminate_destroy(handle)
                return null
            }
            val player = MpvPlayer(mpv, handle, video)
            if (video && !player.startRendering()) {
                player.release()
                return null
            }
            return player
        }
    }
}
