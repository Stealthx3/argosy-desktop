package com.nendo.argosy.data.steam

import android.content.Context
import android.util.Log
import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.dao.SteamDownloadQueueDao
import com.nendo.argosy.data.local.entity.GameEntity
import com.nendo.argosy.data.local.entity.SteamDownloadDbState
import com.nendo.argosy.data.local.entity.SteamDownloadQueueEntity
import com.nendo.argosy.data.model.FileOrigin
import com.nendo.argosy.data.model.GameSource
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

class SteamDownloadLauncherRecoveryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val snapshot = GameEntity(
        id = 1L,
        platformId = 1L,
        platformSlug = "steam",
        title = "Example",
        sortTitle = "Example",
        localPath = null,
        rommId = null,
        igdbId = null,
        steamAppId = 1L,
        source = GameSource.STEAM,
        steamLauncher = "app.gamenative",
        launcherSetManually = true,
        addedAt = Instant.EPOCH
    )
    private val recovered = snapshot.copy(
        steamLauncher = "com.droiddeck.launcher",
        launcherSetManually = false,
        playCount = 7,
        playTimeMinutes = 42,
        lastPlayed = Instant.ofEpochSecond(100),
        saveTarget = "current-save-target",
        saveId = "current-save-id"
    )

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `completed deploy cannot restore retired launcher or overwrite concurrent history and saves`() = runTest {
        val install = temporaryFolder.newFolder("completed")
        File(install, ".download_complete").writeText("")
        val gameDao = mockk<GameDao>(relaxed = true)
        val queueDao = mockk<SteamDownloadQueueDao>(relaxed = true)
        val paths = mockk<SteamPathResolver> {
            coEvery { getInstallDirByName("Example") } returns install
        }
        var current = snapshot
        coEvery { gameDao.getBySteamAppId(1L) } coAnswers {
            current = recovered
            snapshot
        }
        coEvery { gameDao.update(any()) } coAnswers { current = firstArg() }
        coEvery { gameDao.updateLocalPath(any(), any(), any(), any(), any()) } coAnswers {
            current = current.copy(
                localPath = secondArg(),
                source = thirdArg(),
                fileOrigin = arg(3),
                addedAt = arg(4)
            )
        }
        val manager = manager(gameDao, queueDao, paths)
        val deploy = SteamDownloadQueueEntity(
            appId = 1L,
            gameName = "Example",
            coverPath = null,
            installDir = "Example",
            installPath = null,
            totalBytes = 0L,
            bytesDownloaded = 0L,
            state = SteamDownloadDbState.DEPLOYING.name,
            errorReason = null,
            createdAt = Instant.EPOCH
        )

        manager.resumeInterruptedDeploy(deploy)

        assertEquals(recovered.copy(
            localPath = install.absolutePath,
            fileOrigin = FileOrigin.STEAM_DOWNLOAD
        ), current)
        assertTrue(File(install, ".download_complete").exists())
        coVerify(exactly = 0) { gameDao.update(any()) }
        coVerify(timeout = 5_000) { queueDao.getPendingDownloads() }
    }

    @Test
    fun `incomplete cleanup cannot restore retired launcher or overwrite concurrent history and saves`() = runTest {
        val install = temporaryFolder.newFolder("incomplete")
        File(install, "download.part").writeText("partial")
        val steamDir = temporaryFolder.newFolder("steam")
        val observed = snapshot.copy(localPath = install.absolutePath)
        val retired = recovered.copy(localPath = install.absolutePath)
        var current = observed
        var reads = 0
        val gameDao = mockk<GameDao>(relaxed = true)
        val queueDao = mockk<SteamDownloadQueueDao>(relaxed = true)
        val paths = mockk<SteamPathResolver> {
            coEvery { getInstallDir(1L) } returns install
            coEvery { getSteamDir() } returns steamDir
        }
        coEvery { gameDao.getBySteamAppId(1L) } coAnswers {
            if (++reads == 2) current = retired
            observed
        }
        coEvery { gameDao.update(any()) } coAnswers { current = firstArg() }
        coEvery { gameDao.updateLocalPath(any(), any(), any(), any(), any()) } coAnswers {
            current = current.copy(
                localPath = secondArg(),
                source = thirdArg(),
                fileOrigin = arg(3),
                addedAt = arg(4)
            )
        }
        val manager = manager(gameDao, queueDao, paths)

        assertTrue(manager.cleanupIncompleteDownload(1L))

        assertEquals(retired.copy(localPath = null), current)
        assertFalse(install.exists())
        coVerify(exactly = 0) { gameDao.update(any()) }
        coVerify(timeout = 5_000) { queueDao.getPendingDownloads() }
    }

    private fun manager(
        gameDao: GameDao,
        queueDao: SteamDownloadQueueDao,
        paths: SteamPathResolver
    ): SteamContentManager {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        val context = mockk<Context> {
            every { filesDir } returns temporaryFolder.root
        }
        return SteamContentManager(
            context = context,
            gameDao = gameDao,
            steamAuthManager = mockk(relaxed = true),
            steamLibraryManager = mockk(relaxed = true),
            notificationManager = mockk(relaxed = true),
            steamDownloadQueueDao = queueDao,
            downloadManager = mockk(relaxed = true),
            depotManager = mockk(relaxed = true),
            pathResolver = paths,
            progressTracker = mockk(relaxed = true),
            downloadTracker = mockk(relaxed = true),
            fileAccessLayer = mockk(relaxed = true),
            attributionRepository = mockk(relaxed = true),
            gameArtDao = mockk(relaxed = true)
        )
    }
}
