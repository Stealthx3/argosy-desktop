package com.nendo.argosy.platform.mpv

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.StringArray
import com.sun.jna.ptr.PointerByReference

internal interface LibMpv : Library {
    fun mpv_client_api_version(): Long
    fun mpv_create(): Pointer?
    fun mpv_initialize(ctx: Pointer): Int
    fun mpv_set_option_string(ctx: Pointer, name: String, value: String): Int
    fun mpv_set_property_string(ctx: Pointer, name: String, value: String): Int
    fun mpv_get_property_string(ctx: Pointer, name: String): Pointer?
    fun mpv_free(data: Pointer?)
    fun mpv_command(ctx: Pointer, args: StringArray): Int
    fun mpv_terminate_destroy(ctx: Pointer)
    fun mpv_render_context_create(res: PointerByReference, mpv: Pointer, params: Pointer): Int
    fun mpv_render_context_set_update_callback(ctx: Pointer, callback: UpdateCallback?, data: Pointer?)
    fun mpv_render_context_update(ctx: Pointer): Long
    fun mpv_render_context_render(ctx: Pointer, params: Pointer): Int
    fun mpv_render_context_free(ctx: Pointer)

    fun interface UpdateCallback : Callback {
        fun invoke(data: Pointer?)
    }

    companion object {
        const val RENDER_PARAM_INVALID = 0
        const val RENDER_PARAM_API_TYPE = 1
        const val RENDER_PARAM_SW_SIZE = 17
        const val RENDER_PARAM_SW_FORMAT = 18
        const val RENDER_PARAM_SW_STRIDE = 19
        const val RENDER_PARAM_SW_POINTER = 20
        const val RENDER_API_TYPE_SW = "sw"
        const val RENDER_UPDATE_FRAME = 1L

        val instance: LibMpv? by lazy {
            runCatching {
                Native.load("c", LibC::class.java).setlocale(LC_NUMERIC, "C")
                Native.load("mpv", LibMpv::class.java)
            }.getOrNull()
        }

        private const val LC_NUMERIC = 1
    }
}

private interface LibC : Library {
    fun setlocale(category: Int, locale: String): Pointer?
}
