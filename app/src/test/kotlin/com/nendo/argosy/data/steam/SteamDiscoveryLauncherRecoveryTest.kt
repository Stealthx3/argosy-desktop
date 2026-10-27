package com.nendo.argosy.data.steam

import android.content.Context
import android.os.Environment
import android.util.Log
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.dao.SteamDownloadQueueDao
import com.nendo.argosy.data.local.entity.GameEntity
import com.nendo.argosy.data.model.FileOrigin
import com.nendo.argosy.data.model.GameSource
import com.nendo.argosy.data.storage.FileAccessLayer
import com.nendo.argosy.data.storage.FileInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SteamDiscoveryLauncherRecoveryTest {
    private enum class DiscoveryPath { MANAGED, EXTERNAL, RECLAIM }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `managed install discovery cannot restore a retired pin`() = runTest {
        verifyDiscoveryPreservesRecovery(DiscoveryPath.MANAGED)
    }

    @Test
    fun `external install discovery cannot restore a retired pin`() = runTest {
        verifyDiscoveryPreservesRecovery(DiscoveryPath.EXTERNAL)
    }

    @Test
    fun `reclaim cannot restore a pin retired while moving files`() = runTest {
        verifyDiscoveryPreservesRecovery(DiscoveryPath.RECLAIM)
    }

    private suspend fun verifyDiscoveryPreservesRecovery(mode: DiscoveryPath) {
        mockkStatic(Environment::class, Log::class)
        every { Environment.getExternalStorageDirectory() } returns temporaryFolder.root
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        val context = mockk<Context> {
            every { filesDir } returns temporaryFolder.root
        }
        val gameDao = mockk<GameDao>(relaxed = true)
        val queueDao = mockk<SteamDownloadQueueDao>(relaxed = true)
        val paths = mockk<SteamPathResolver>()
        val files = mockk<FileAccessLayer>(relaxed = true)
        val steamDir = temporaryFolder.newFolder("steam")
        val strandedPath = temporaryFolder.root.absolutePath + "/Android/data/app.gamenative/Example"
        val snapshot = GameEntity(
            id = 1L,
            platformId = 1L,
            platformSlug = "steam",
            title = "Example",
            sortTitle = "Example",
            localPath = if (mode == DiscoveryPath.RECLAIM) strandedPath else null,
            rommId = null,
            igdbId = null,
            steamAppId = 1L,
            steamInstallDir = "Example",
            source = GameSource.STEAM,
            steamLauncher = "app.gamenative",
            launcherSetManually = true
        )
        var current = snapshot
        val recovered = snapshot.copy(steamLauncher = "com.droiddeck.launcher", launcherSetManually = false)
        coEvery { paths.getSteamDir() } returns steamDir
        coEvery { paths.getResolvedSteamBase() } returns null
        coEvery { paths.findAllGnStoragePaths() } returns
            if (mode == DiscoveryPath.EXTERNAL) listOf("/gn") else emptyList()
        coEvery { gameDao.update(any()) } coAnswers { current = firstArg() }
        coEvery { gameDao.updateLocalPath(any(), any(), any(), any(), any()) } coAnswers {
            current = current.copy(
                localPath = secondArg(),
                source = thirdArg(),
                fileOrigin = arg(3),
                addedAt = arg(4)
            )
        }
        val installPath = if (mode == DiscoveryPath.MANAGED) {
            val install = File(steamDir, "1").apply { mkdir() }
            File(install, ".download_complete").writeText("")
            coEvery { gameDao.getBySteamAppId(1L) } coAnswers {
                current = recovered
                snapshot
            }
            coEvery { gameDao.getAllWithSteamAppId() } returns emptyList()
            install.absolutePath
        } else if (mode == DiscoveryPath.EXTERNAL) {
            var firstRead = true
            coEvery { gameDao.getAllWithSteamAppId() } coAnswers {
                if (firstRead) {
                    firstRead = false
                    current = recovered
                    listOf(snapshot)
                } else emptyList()
            }
            val scanRoot = "/gn/Steam/steamapps/common"
            every { files.exists(any()) } returns true
            every { files.listFilesUnion(scanRoot) } returns listOf(
                FileInfo("$scanRoot/Example", "Example", true, false, 0L, 0L)
            )
            "$scanRoot/Example"
        } else {
            val source = File(strandedPath).apply { mkdirs() }
            File(source, ".download_complete").writeText("")
            val target = File(temporaryFolder.root, "reclaimed/Example")
            coEvery { gameDao.getAllWithSteamAppId() } returns listOf(snapshot)
            coEvery { paths.snapshotInstallDirAtEnqueue(1L, "Example") } returns target
            coEvery { paths.getAvailableBytes(any()) } returns Long.MAX_VALUE
            coEvery { paths.getDirectorySize(source) } returns 0L
            coEvery { paths.moveDirectory(source, target) } coAnswers {
                current = recovered
                source.renameTo(target)
            }
            target.absolutePath
        }
        val manager = SteamContentManager(
            context, gameDao, mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true),
            queueDao, mockk(relaxed = true), mockk(relaxed = true), paths, mockk(relaxed = true),
            mockk(relaxed = true), files, mockk(relaxed = true), mockk(relaxed = true)
        )

        assertEquals(if (mode == DiscoveryPath.RECLAIM) 0 else 1, manager.discoverLocalSteamGames())

        assertEquals(installPath, current.localPath)
        assertEquals(if (mode == DiscoveryPath.RECLAIM) snapshot.fileOrigin else FileOrigin.ADOPTED, current.fileOrigin)
        assertEquals("com.droiddeck.launcher", current.steamLauncher)
        assertFalse(current.launcherSetManually)
        assertEquals(snapshot.addedAt, current.addedAt)
        coVerify(exactly = 0) { gameDao.update(any()) }
        coVerify(timeout = 5_000) { queueDao.getPendingDownloads() }
    }
}
