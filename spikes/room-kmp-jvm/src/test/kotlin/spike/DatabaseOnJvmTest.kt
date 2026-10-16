package spike

import androidx.room.Room
import androidx.room.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.nendo.argosy.data.local.ALauncherDatabase
import com.nendo.argosy.data.local.entity.PlatformEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Instant
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pass criteria for the Room KMP spike: upstream's ALauncherDatabase (v214) builds and opens on
 * the JVM with BundledSQLiteDriver, a DAO insert works, and a Flow query sees the inserted row.
 * The schema identity check lives in the build (see README.md): the exported schemas/.../214.json
 * must equal app/schemas/.../214.json.
 */
class DatabaseOnJvmTest {

    private val dir: File = createTempDirectory("argosy-room-spike").toFile()
    private val db: ALauncherDatabase =
        Room.databaseBuilder<ALauncherDatabase>(name = File(dir, "alauncher.db").absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    @AfterTest
    fun tearDown() {
        db.close()
        dir.deleteRecursively()
    }

    @Test
    fun opensInsertsAndObserves() = runBlocking {
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
    fun onDiskVersionMatchesUpstream() = runBlocking {
        db.platformDao().getAllPlatforms()
        val version = db.useReaderConnection { conn ->
            conn.usePrepared("PRAGMA user_version") { it.step(); it.getLong(0) }
        }
        assertEquals(214L, version)
    }
}
