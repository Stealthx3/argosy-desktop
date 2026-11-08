package com.nendo.argosy.platform

import com.nendo.argosy.util.Logger
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference

/**
 * Under Steam Game Mode, the marker that gives every top-level window appearing on Argosy's display
 * during a game Argosy's Steam app id (`STEAM_GAME`): the property gamescope reads to treat a window
 * as part of Argosy's launch. Windows present at [start] and windows with an app id of their own are
 * left unmarked.
 */
object GameModeWindowTagger {
    private const val TAG = "GameModeWindowTagger"
    private const val POLL_MS = 250L
    private const val STEAM_GAME = "STEAM_GAME"
    private const val ATOM_CARDINAL = 6
    private const val PROP_MODE_REPLACE: Byte = 0
    private const val FORMAT_32: Byte = 32

    @Volatile
    private var worker: Thread? = null

    @Synchronized
    fun start(environment: Map<String, String>) {
        stop()
        val appId = GameModeWindowRules.appId(environment) ?: return
        val xcb = Xcb.library ?: return Logger.warn(TAG, "libxcb not available; emulator windows stay untagged")
        worker = Thread({ run(xcb, appId) }, "game-mode-window-tagger").apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stop() {
        worker?.interrupt()
        worker = null
    }

    private fun run(xcb: Xcb, appId: Int) {
        val connection = xcb.xcb_connect(null, null) ?: return
        try {
            if (xcb.xcb_connection_has_error(connection) != 0) return
            val root = xcb.xcb_setup_roots_iterator(xcb.xcb_get_setup(connection)).data?.getInt(0) ?: return
            val atom = internAtom(xcb, connection, STEAM_GAME) ?: return
            val value = Memory(Int.SIZE_BYTES.toLong()).apply { setInt(0, appId) }
            val baseline = topLevelWindows(xcb, connection, root).toSet()
            val handled = HashSet<Int>()
            while (!Thread.currentThread().isInterrupted) {
                val fresh = topLevelWindows(xcb, connection, root).filter { it !in baseline && it !in handled }
                fresh.forEach { window ->
                    handled += window
                    if (!hasProperty(xcb, connection, window, atom)) {
                        xcb.xcb_change_property(connection, PROP_MODE_REPLACE, window, atom, ATOM_CARDINAL, FORMAT_32, 1, value)
                    }
                }
                if (fresh.isNotEmpty()) xcb.xcb_flush(connection)
                drainEvents(xcb, connection)
                Thread.sleep(POLL_MS)
            }
        } catch (_: InterruptedException) {
        } catch (e: Exception) {
            Logger.error(TAG, "Window tagging stopped", e)
        } finally {
            xcb.xcb_disconnect(connection)
        }
    }

    private fun internAtom(xcb: Xcb, connection: Pointer, name: String): Int? {
        val reply = xcb.xcb_intern_atom_reply(connection, xcb.xcb_intern_atom(connection, 0, name.length.toShort(), name), null)
            ?: return null
        return reply.getInt(ATOM_OFFSET).also { Native.free(Pointer.nativeValue(reply)) }
    }

    private fun topLevelWindows(xcb: Xcb, connection: Pointer, root: Int): List<Int> {
        val reply = xcb.xcb_query_tree_reply(connection, xcb.xcb_query_tree(connection, root), null) ?: return emptyList()
        return try {
            val count = xcb.xcb_query_tree_children_length(reply)
            if (count <= 0) emptyList() else xcb.xcb_query_tree_children(reply).getIntArray(0, count).toList()
        } finally {
            Native.free(Pointer.nativeValue(reply))
        }
    }

    private fun hasProperty(xcb: Xcb, connection: Pointer, window: Int, atom: Int): Boolean {
        val reply = xcb.xcb_get_property_reply(connection, xcb.xcb_get_property(connection, 0, window, atom, 0, 0, 1), null)
            ?: return true
        return (reply.getInt(PROPERTY_TYPE_OFFSET) != 0).also { Native.free(Pointer.nativeValue(reply)) }
    }

    private fun drainEvents(xcb: Xcb, connection: Pointer) {
        while (true) {
            val event = xcb.xcb_poll_for_event(connection) ?: return
            Native.free(Pointer.nativeValue(event))
        }
    }

    private const val ATOM_OFFSET = 8L
    private const val PROPERTY_TYPE_OFFSET = 8L
}

/**
 * The Steam app id Argosy runs under in Game Mode, as the 32-bit value `STEAM_GAME` holds; null
 * outside Game Mode or without one.
 */
object GameModeWindowRules {
    fun appId(environment: Map<String, String>): Int? {
        if (!DesktopWindowMode.isGamescope(environment)) return null
        return environment["SteamAppId"]?.trim()?.toLongOrNull()?.takeIf { it in 1..UINT_MAX }?.toInt()
    }

    private const val UINT_MAX = 0xFFFFFFFFL
}

@Suppress("FunctionName")
internal interface Xcb : Library {
    fun xcb_connect(displayName: String?, screen: Pointer?): Pointer?
    fun xcb_connection_has_error(connection: Pointer): Int
    fun xcb_disconnect(connection: Pointer)
    fun xcb_flush(connection: Pointer): Int
    fun xcb_get_setup(connection: Pointer): Pointer
    fun xcb_setup_roots_iterator(setup: Pointer): ScreenIterator
    fun xcb_intern_atom(connection: Pointer, onlyIfExists: Byte, nameLength: Short, name: String): Int
    fun xcb_intern_atom_reply(connection: Pointer, cookie: Int, error: PointerByReference?): Pointer?
    fun xcb_query_tree(connection: Pointer, window: Int): Int
    fun xcb_query_tree_reply(connection: Pointer, cookie: Int, error: PointerByReference?): Pointer?
    fun xcb_query_tree_children(reply: Pointer): Pointer
    fun xcb_query_tree_children_length(reply: Pointer): Int
    fun xcb_get_property(connection: Pointer, delete: Byte, window: Int, property: Int, type: Int, offset: Int, length: Int): Int
    fun xcb_get_property_reply(connection: Pointer, cookie: Int, error: PointerByReference?): Pointer?
    fun xcb_change_property(connection: Pointer, mode: Byte, window: Int, property: Int, type: Int, format: Byte, length: Int, data: Pointer): Int
    fun xcb_poll_for_event(connection: Pointer): Pointer?

    @Structure.FieldOrder("data", "rem", "index")
    class ScreenIterator : Structure(), Structure.ByValue {
        @JvmField var data: Pointer? = null
        @JvmField var rem: Int = 0
        @JvmField var index: Int = 0
    }

    companion object {
        val library: Xcb? by lazy {
            listOf("xcb", "libxcb.so.1").firstNotNullOfOrNull { name ->
                runCatching { Native.load(name, Xcb::class.java) }.getOrNull()
            }
        }
    }
}
