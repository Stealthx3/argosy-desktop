package com.nendo.argosy.di

import com.nendo.argosy.data.local.entity.PlatformEntity
import com.nendo.argosy.platform.DesktopPlatformContext
import com.nendo.argosy.platform.PlatformApplication
import androidx.lifecycle.SavedStateHandle
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
    private val component: DesktopComponent = DaggerDesktopComponent.factory().create(
        dir,
        PlatformApplication(DesktopPlatformContext(platformFilesDir = dir.resolve("files"), platformCacheDir = dir.resolve("cache"))),
    )

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

    @Test
    fun everyViewModelIsBuiltFromTheGraph() {
        val viewModels = component.viewModelComponentFactory().create(SavedStateHandle()).viewModels()
        assertTrue(viewModels.keys.any { it.simpleName == "FirstRunViewModel" })
        assertTrue(viewModels.size >= 32)
        val failures = viewModels.mapNotNull { (key, provider) ->
            runCatching { provider.get() }.exceptionOrNull()?.let { key.simpleName to it }
        }.toMap()
        assertEquals(ROUTE_ARGUMENT_VIEW_MODELS, failures.keys, failures.toString())
        failures.values.forEach { assertTrue(it is IllegalStateException, it.toString()) }
    }

    private companion object {
        val ROUTE_ARGUMENT_VIEW_MODELS = setOf(
            "CollectionDetailViewModel",
            "VirtualBrowserViewModel",
            "VirtualCategoryViewModel",
            "SaveTimelineViewModel",
        )
    }
}
