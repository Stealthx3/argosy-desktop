package com.nendo.argosy.platform

import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.runBlocking
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.util.Properties
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrandRelocationTest {

    private val home: File = createTempDirectory("brand-relocation").toFile()
    private val oldLibrary = home.resolve("Games/Argosy")
    private val library = home.resolve("Games/IceBreaker")

    @AfterTest
    fun tearDown() {
        home.deleteRecursively()
    }

    @Test
    fun rewriterReplacesWholePathPrefixesOnly() {
        val rewriter = PathRewriter(listOf(PathMove("/h/Games/Argosy", "/h/Games/IceBreaker")))
        assertEquals("/h/Games/IceBreaker/nes/a.nes", rewriter.rewrite("/h/Games/Argosy/nes/a.nes"))
        assertEquals("/h/Games/IceBreaker", rewriter.rewrite("/h/Games/Argosy"))
        assertEquals("""{"p":"/h/Games/IceBreaker/x"}""", rewriter.rewrite("""{"p":"/h/Games/Argosy/x"}"""))
        assertEquals("/h/Games/ArgosyOld/x", rewriter.rewrite("/h/Games/ArgosyOld/x"))
        assertEquals("/mnt/h/Games/Argosy/x", rewriter.rewrite("/mnt/h/Games/Argosy/x"))
    }

    @Test
    fun movesTheLibraryAndOldFlatpakDataAndRewritesStoredPaths() {
        oldLibrary.resolve("nes").mkdirs()
        oldLibrary.resolve("nes/a.nes").writeText("a")
        oldLibrary.resolve("gb").mkdirs()
        oldLibrary.resolve("gb/b.gb").writeText("b")
        library.resolve("nes").mkdirs()

        val dataHome = home.resolve(".var/app/io.github.stealthx3.icebreaker-client/data")
        val oldData = home.resolve(".var/app/io.github.stealthx3.ArgosyDesktop/data/argosy-desktop")
        val oldDataPath = oldData.path
        oldData.resolve("datastore").mkdirs()
        oldData.resolve("files/shared_prefs").mkdirs()
        writeDatabase(
            oldData.resolve("alauncher.db"),
            listOf("${oldLibrary.path}/nes/a.nes", "$oldDataPath/external/downloads/gb/c.gb", "/elsewhere/d.gb"),
        )
        writePreferences(oldData.resolve("datastore/settings.preferences_pb"))
        Properties().apply { setProperty("last", "${oldLibrary.path}/gb/b.gb") }
            .let { p -> oldData.resolve("files/shared_prefs/s.properties").outputStream().use { p.store(it, null) } }

        relocation(dataHome, sandboxed = true).run()

        val data = dataHome.resolve("icebreaker")
        assertTrue(library.resolve("nes/a.nes").isFile)
        assertTrue(library.resolve("gb/b.gb").isFile)
        assertFalse(oldLibrary.exists())
        assertFalse(oldData.exists())
        assertEquals(
            listOf("${library.path}/nes/a.nes", "${data.path}/external/downloads/gb/c.gb", "/elsewhere/d.gb"),
            readDatabase(data.resolve("alauncher.db")),
        )
        val prefs = readPreferences(data.resolve("datastore/settings.preferences_pb"))
        assertEquals(library.path, prefs[stringPreferencesKey("rom_storage_path")])
        assertEquals(setOf("${library.path}/nes", "/other"), prefs[stringSetPreferencesKey("paths")])
        val props = Properties().apply { data.resolve("files/shared_prefs/s.properties").inputStream().use(::load) }
        assertEquals("${library.path}/gb/b.gb", props.getProperty("last"))
        assertTrue(data.resolve(".brand-relocation").isFile)
        assertEquals(
            listOf("${oldLibrary.path}/nes/a.nes", "$oldDataPath/external/downloads/gb/c.gb", "/elsewhere/d.gb"),
            readDatabase(data.resolve("alauncher.db.before-relocation")),
        )
    }

    @Test
    fun keepsAFileThatExistsInBothFoldersAndStillPointsAtTheNewOne() {
        oldLibrary.resolve("nes").mkdirs()
        oldLibrary.resolve("nes/a.nes").writeText("old")
        library.resolve("nes").mkdirs()
        library.resolve("nes/a.nes").writeText("new")
        val dataHome = home.resolve(".local/share")
        val data = dataHome.resolve("icebreaker").apply { mkdirs() }
        writeDatabase(data.resolve("alauncher.db"), listOf("${oldLibrary.path}/nes/a.nes"))

        relocation(dataHome, sandboxed = false).run()

        assertEquals("new", library.resolve("nes/a.nes").readText())
        assertEquals("old", oldLibrary.resolve("nes/a.nes").readText())
        assertEquals(listOf("${library.path}/nes/a.nes"), readDatabase(data.resolve("alauncher.db")))
    }

    @Test
    fun movesTheIceBreakerFlatpakDataToTheNewAppIdAndFlagsTheSteamShortcut() {
        val dataHome = home.resolve(".var/app/io.github.stealthx3.icebreaker-client/data")
        val oldData = home.resolve(".var/app/io.github.stealthx3.IceBreaker/data/icebreaker")
        writeDatabase(oldData.resolve("alauncher.db"), listOf("${oldData.path}/external/downloads/gb/c.gb", "/elsewhere/d.gb"))
        oldData.resolve(".brand-relocation").writeText("1")

        relocation(dataHome, sandboxed = true).run()

        val data = dataHome.resolve("icebreaker")
        assertFalse(oldData.exists())
        assertEquals(
            listOf("${data.path}/external/downloads/gb/c.gb", "/elsewhere/d.gb"),
            readDatabase(data.resolve("alauncher.db")),
        )
        assertEquals("2", data.resolve(".brand-relocation").readText())
        assertTrue(data.resolve(BrandRelocation.STEAM_SHORTCUT_STALE).isFile)
    }

    @Test
    fun aFreshInstallDoesNotFlagTheSteamShortcut() {
        val dataHome = home.resolve(".local/share")
        val data = dataHome.resolve("icebreaker").apply { mkdirs() }
        writeDatabase(data.resolve("alauncher.db"), listOf("/elsewhere/d.gb"))

        relocation(dataHome, sandboxed = false).run()

        assertEquals("2", data.resolve(".brand-relocation").readText())
        assertFalse(data.resolve(BrandRelocation.STEAM_SHORTCUT_STALE).exists())
    }

    @Test
    fun aSecondStartChangesNothing() {
        val dataHome = home.resolve(".local/share")
        val data = dataHome.resolve("icebreaker").apply { mkdirs() }
        writeDatabase(data.resolve("alauncher.db"), listOf("${home.path}/.local/share/argosy-desktop/x.gb"))
        relocation(dataHome, sandboxed = false).run()
        assertEquals(listOf("${data.path}/x.gb"), readDatabase(data.resolve("alauncher.db")))

        writeDatabase(data.resolve("alauncher.db"), listOf("${home.path}/.local/share/argosy-desktop/y.gb"), create = false)
        relocation(dataHome, sandboxed = false).run()
        assertEquals(listOf("${data.path}/x.gb", "${home.path}/.local/share/argosy-desktop/y.gb"), readDatabase(data.resolve("alauncher.db")))
    }

    private fun relocation(dataHome: File, sandboxed: Boolean) = BrandRelocation(
        home = home,
        dataHome = dataHome,
        cacheHome = home.resolve(".cache"),
        sandboxed = sandboxed,
        runHostScript = HostShell::runScript,
    )

    private fun writeDatabase(file: File, paths: List<String>, create: Boolean = true) {
        file.parentFile.mkdirs()
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            if (create) connection.execSQL("CREATE TABLE games (id INTEGER PRIMARY KEY, localPath TEXT, size INTEGER)")
            paths.forEach { path ->
                connection.prepare("INSERT INTO games (localPath, size) VALUES (?, 1)").use { it.bindText(1, path); it.step() }
            }
        } finally {
            connection.close()
        }
    }

    private fun readDatabase(file: File): List<String> {
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            return connection.prepare("SELECT localPath FROM games ORDER BY id").use { stmt ->
                buildList { while (stmt.step()) add(stmt.getText(0)) }
            }
        } finally {
            connection.close()
        }
    }

    private fun writePreferences(file: File) = runBlocking {
        val prefs = mutablePreferencesOf(
            stringPreferencesKey("rom_storage_path") to oldLibrary.path,
            stringSetPreferencesKey("paths") to setOf("${oldLibrary.path}/nes", "/other"),
        )
        file.sink().buffer().use { PreferencesSerializer.writeTo(prefs, it) }
    }

    private fun readPreferences(file: File) = runBlocking {
        file.source().buffer().use { PreferencesSerializer.readFrom(it) }
    }
}
