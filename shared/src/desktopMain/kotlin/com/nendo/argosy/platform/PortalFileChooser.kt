package com.nendo.argosy.platform

import com.nendo.argosy.util.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.Struct
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.annotations.DBusMemberName
import org.freedesktop.dbus.annotations.Position
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.DBusSigHandler
import org.freedesktop.dbus.messages.DBusSignal
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant
import java.util.UUID

internal object PortalFileChooser {
    private const val TAG = "PortalFileChooser"
    private const val BUS_NAME = "org.freedesktop.portal.Desktop"
    private const val OBJECT_PATH = "/org/freedesktop/portal/desktop"
    private const val MIME_RULE = 1
    private const val RESPONSE_SUCCESS = 0
    private const val DIALOG_TITLE = "Argosy"

    suspend fun openFile(mimeTypes: Array<String>): PlatformUri? = withContext(Dispatchers.IO) {
        runCatching { request(mimeTypes) }
            .onFailure { Logger.error(TAG, "File chooser portal unavailable", it) }
            .getOrNull()
    }

    private suspend fun request(mimeTypes: Array<String>): PlatformUri? =
        DBusConnectionBuilder.forSessionBus().build().use { connection ->
            val token = "argosy" + UUID.randomUUID().toString().replace("-", "")
            val sender = connection.uniqueName.removePrefix(":").replace('.', '_')
            val handle = "$OBJECT_PATH/request/$sender/$token"
            val answer = CompletableDeferred<PortalRequest.Response>()
            val handler = DBusSigHandler<PortalRequest.Response> { signal ->
                if (signal.path == handle) answer.complete(signal)
            }
            connection.addSigHandler(PortalRequest.Response::class.java, handler)
            try {
                val chooser = connection.getRemoteObject(BUS_NAME, OBJECT_PATH, FileChooser::class.java)
                val options = buildMap<String, Variant<*>> {
                    put("handle_token", Variant(token))
                    put("modal", Variant(true))
                    if (mimeTypes.isNotEmpty()) {
                        val filter = Filter(mimeTypes.joinToString(", "), mimeTypes.map { Rule(UInt32(MIME_RULE.toLong()), it) })
                        put("filters", Variant(listOf(filter), "a(sa(us))"))
                    }
                }
                chooser.openFile("", DIALOG_TITLE, options)
                val response = answer.await()
                if (response.response.toInt() != RESPONSE_SUCCESS) return@use null
                firstUri(response.results["uris"]?.value)?.let(PlatformUris::parse)
            } finally {
                connection.removeSigHandler(PortalRequest.Response::class.java, handler)
            }
        }

    private fun firstUri(value: Any?): String? = when (value) {
        is List<*> -> value.firstOrNull() as? String
        is Array<*> -> value.firstOrNull() as? String
        else -> null
    }

    @DBusInterfaceName("org.freedesktop.portal.FileChooser")
    interface FileChooser : DBusInterface {
        @DBusMemberName("OpenFile")
        fun openFile(parentWindow: String, title: String, options: Map<String, @JvmSuppressWildcards Variant<*>>): DBusPath
    }

    @DBusInterfaceName("org.freedesktop.portal.Request")
    interface PortalRequest : DBusInterface {
        class Response(
            path: String,
            val response: UInt32,
            val results: Map<String, @JvmSuppressWildcards Variant<*>>
        ) : DBusSignal(path, response, results)
    }

    class Filter(
        @field:Position(0) @JvmField val name: String,
        @field:Position(1) @JvmField val rules: List<Rule>
    ) : Struct()

    class Rule(
        @field:Position(0) @JvmField val kind: UInt32,
        @field:Position(1) @JvmField val pattern: String
    ) : Struct()
}
