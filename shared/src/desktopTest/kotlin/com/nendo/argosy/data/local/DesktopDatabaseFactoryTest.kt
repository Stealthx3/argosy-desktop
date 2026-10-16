package com.nendo.argosy.data.local

import androidx.room.useReaderConnection
import com.nendo.argosy.data.local.entity.PlatformEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Instant
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopDatabaseFactoryTest {

    private val dir: File = createTempDirectory("argosy-db-test").toFile()
    private val db: ALauncherDatabase = DesktopDatabaseFactory.create(File(dir, "alauncher.db"))

    @AfterTest
    fun tearDown() {
        db.close()
        dir.deleteRecursively()
    }

    @Test
    fun insertedPlatformReachesFlowQueries() = runBlocking {
        val dao = db.platformDao()
        assertEquals(emptyList(), dao.observeAllPlatforms().first())

        val snes = PlatformEntity(
            id = 1,
            slug = "snes",
            name = "Super Nintendo Entertainment System",
            shortName = "SNES",
            romExtensions = "sfc,smc",
            lastScanned = Instant.parse("2026-10-16T00:00:00Z"),
            gameCount = 3,
        )
        dao.insert(snes)

        assertEquals(listOf(snes), dao.observeAllPlatforms().first())
        assertEquals(listOf(snes), dao.observeVisiblePlatforms().first())
    }

    @Test
    fun createdDatabaseIsAtSchemaVersion214() = runBlocking {
        db.platformDao().getAllPlatforms()
        val version = db.useReaderConnection { conn ->
            conn.usePrepared("PRAGMA user_version") { it.step(); it.getLong(0) }
        }
        assertEquals(214L, version)
    }
}
