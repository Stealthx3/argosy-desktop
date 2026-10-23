package com.nendo.argosy.platform

import kotlinx.coroutines.runBlocking
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.connections.AbstractConnection
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.types.UInt32
import org.freedesktop.dbus.types.Variant
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PortalFileChooserTest {
    private val onPrivateBus = System.getenv("ARGOSY_PRIVATE_DBUS") == "1"

    private class FakePortal(
        private val connection: DBusConnection,
        private val response: Int,
        private val uris: List<String>
    ) : PortalFileChooser.FileChooser {
        var options: Map<String, Variant<*>> = emptyMap()

        override fun getObjectPath() = "/org/freedesktop/portal/desktop"

        override fun openFile(parentWindow: String, title: String, options: Map<String, Variant<*>>): DBusPath {
            this.options = options
            val sender = AbstractConnection.getCallInfo().source.removePrefix(":").replace('.', '_')
            val handle = "/org/freedesktop/portal/desktop/request/$sender/${options.getValue("handle_token").value}"
            thread {
                Thread.sleep(200)
                val results = mapOf<String, Variant<*>>("uris" to Variant(uris, "as"))
                connection.sendMessage(PortalFileChooser.PortalRequest.Response(handle, UInt32(response.toLong()), results))
            }
            return DBusPath(handle)
        }
    }

    private fun withPortal(response: Int, uris: List<String>, block: (FakePortal) -> Unit) {
        DBusConnectionBuilder.forSessionBus().build().use { connection ->
            val portal = FakePortal(connection, response, uris)
            connection.exportObject(portal)
            connection.requestBusName("org.freedesktop.portal.Desktop")
            block(portal)
        }
    }

    @Test
    fun pickedFileComesBackAsUriWithMimeFilters() {
        if (!onPrivateBus) return
        withPortal(0, listOf("file:///tmp/Back%20drop.png")) { portal ->
            val uri = runBlocking { PortalFileChooser.openFile(arrayOf("image/*")) }
            assertEquals("file:///tmp/Back%20drop.png", uri.toString())
            assertEquals("a(sa(us))", portal.options.getValue("filters").sig)
            assertEquals(true, portal.options.getValue("modal").value)
        }
    }

    @Test
    fun cancelledDialogReturnsNull() {
        if (!onPrivateBus) return
        withPortal(1, emptyList()) {
            assertNull(runBlocking { PortalFileChooser.openFile(arrayOf("font/ttf")) })
        }
    }
}
