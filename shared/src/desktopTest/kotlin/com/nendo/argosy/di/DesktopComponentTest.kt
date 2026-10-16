package com.nendo.argosy.di

import com.nendo.argosy.data.local.entity.PlatformEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Instant
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopComponentTest {

    private val dir: File = createTempDirectory("argosy-di-test").toFile()
    private val component: DesktopComponent = DaggerDesktopComponent.factory().create(dir)

    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun daoFromTheGraphReadsWhatItWrote() = runBlocking {
        val dao = component.platformDao()
        val gb = PlatformEntity(
            id = 7,
            slug = "gb",
            name = "Game Boy",
            shortName = "GB",
            romExtensions = "gb",
            lastScanned = Instant.parse("2026-10-16T00:00:00Z"),
            gameCount = 4,
        )
        dao.insert(gb)
        assertEquals(listOf(gb), dao.observeAllPlatforms().first())
    }

    @Test
    fun preferenceRepositoryFromTheGraphPersistsToTheDataDirectory() = runBlocking {
        val prefs = component.syncPreferences()
        assertNull(prefs.getRommSnapshotSupport("server-a"))
        prefs.setRommSnapshotSupport("server-a", true)
        assertEquals(true, prefs.getRommSnapshotSupport("server-a"))
        assertTrue(File(dir, "datastore/settings.preferences_pb").isFile)
    }
}
