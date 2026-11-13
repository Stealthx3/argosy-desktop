package com.nendo.argosy.data.social.uploader

import com.nendo.argosy.data.local.dao.GameDao
import com.nendo.argosy.data.local.dao.PlaySessionDao
import com.nendo.argosy.data.preferences.SyncPreferencesRepository
import com.nendo.argosy.data.remote.romm.RomMApi
import com.nendo.argosy.data.remote.romm.RomMConnectionManager
import com.nendo.argosy.data.remote.romm.RomMDevice
import com.nendo.argosy.data.remote.romm.RomMPlaySession
import com.nendo.argosy.util.Logger
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "PlaySessionPull"
private const val PAGE_SIZE = 200
private val FULL_PULL_INTERVAL: Duration = Duration.ofHours(24)

const val UNASSIGNED_PLAY_DEVICE_ID = "romm-unassigned"
private val UNASSIGNED_DEVICE = RomMDevice(
    id = UNASSIGNED_PLAY_DEVICE_ID,
    name = "",
    platform = null,
    client = null,
    clientVersion = null
)

/**
 * Copies down the play sessions RomM holds for the account's other devices, so hours played
 * elsewhere count here. This device is skipped: its sessions originate locally, and a row the
 * ingest acknowledged as a duplicate carries no server id to match on. A full account pass,
 * which also takes sessions whose device was removed, runs until one completes and then once
 * per [FULL_PULL_INTERVAL]; runs in between ask each device only for sessions newer than the
 * latest one held. A pulled session whose game and start time are already held is skipped.
 * Runs after the first-connect backfill, after each upload, on the [PlaySessionPullScheduler]
 * cadence, and on demand from the Play Time hub.
 */
@Singleton
class PlaySessionPull @Inject constructor(
    private val playSessionDao: PlaySessionDao,
    private val gameDao: GameDao,
    private val syncPreferencesRepository: SyncPreferencesRepository,
    private val connectionManager: RomMConnectionManager
) {
    data class Summary(
        val added: Int,
        val devices: Int,
        val stoppedBy: String? = null
    )

    private val runMutex = Mutex()

    val canPull: Boolean
        get() = connectionManager.getCapabilities().supportsPlaySessionIngest &&
            connectionManager.isConnected()

    suspend fun run(): Summary = runMutex.withLock {
        if (!canPull) return Summary(0, 0, "Capability or connection unavailable")
        val api = connectionManager.getApi() ?: return Summary(0, 0, "No api")
        val ownerUserId = syncPreferencesRepository.getRommUserId() ?: return Summary(0, 0, "No account")
        val thisDeviceId = connectionManager.getDeviceId()

        val relinked = playSessionDao.relinkPulledByRommId(ownerUserId)
        if (relinked > 0) Logger.info(TAG, "run: relinked $relinked pulled sessions by rom id")

        val devices = try {
            val response = api.getDevices()
            if (!response.isSuccessful) return Summary(0, 0, "Devices returned ${response.code()}")
            response.body().orEmpty()
        } catch (e: Exception) {
            Logger.error(TAG, "run: devices failed", e)
            return Summary(0, 0, e.message ?: "Network failure")
        }

        val romCache = HashMap<Long, PulledGameRef>()
        val resolve: suspend (Long) -> PulledGameRef = { romId -> romCache.getOrPut(romId) { resolveGame(api, romId) } }
        val now = Instant.now()
        val lastFull = syncPreferencesRepository.getRommPlaySessionFullPull()
        val summary = if (lastFull == null || Duration.between(lastFull, now) >= FULL_PULL_INTERVAL) {
            pullAccount(api, devices, thisDeviceId, ownerUserId, resolve)
        } else {
            pullDevices(api, devices, thisDeviceId, ownerUserId, resolve)
        }
        if (summary.stoppedBy != null) return Summary(summary.added, summary.devices, summary.stoppedBy)
        if (summary.full) syncPreferencesRepository.setRommPlaySessionFullPull(now)
        syncPreferencesRepository.setRommPlaySessionLastPull(now)
        Logger.info(TAG, "run: added=${summary.added} devices=${summary.devices} full=${summary.full}")
        Summary(summary.added, summary.devices)
    }

    private data class PassResult(
        val added: Int,
        val devices: Int,
        val full: Boolean,
        val stoppedBy: String? = null
    )

    private suspend fun pullAccount(
        api: RomMApi,
        devices: List<RomMDevice>,
        thisDeviceId: String?,
        ownerUserId: Long,
        resolve: suspend (Long) -> PulledGameRef
    ): PassResult {
        val byId = devices.associateBy { it.id }
        val others = devices.count { it.id != thisDeviceId }
        var added = 0
        var offset = 0
        while (true) {
            val page = when (val result = fetchPage(api, null, null, offset)) {
                is PageResult.Page -> result.sessions
                is PageResult.Stopped -> return PassResult(added, others, full = false, stoppedBy = result.reason)
            }
            if (page.isEmpty()) break
            page.filter { it.deviceId != thisDeviceId }
                .groupBy { it.deviceId }
                .forEach { (deviceId, sessions) ->
                    val device = deviceId?.let { byId[it] ?: unknownDevice(it) } ?: UNASSIGNED_DEVICE
                    added += insertMissing(sessions, device, ownerUserId, resolve)
                }
            if (page.size < PAGE_SIZE) break
            offset += page.size
        }
        return PassResult(added, others, full = true)
    }

    private suspend fun pullDevices(
        api: RomMApi,
        devices: List<RomMDevice>,
        thisDeviceId: String?,
        ownerUserId: Long,
        resolve: suspend (Long) -> PulledGameRef
    ): PassResult {
        var added = 0
        var devicesDone = 0
        for (device in devices) {
            if (device.id == thisDeviceId) continue
            val since = playSessionDao.getLatestRommStartForDevice(device.id, ownerUserId)
            var offset = 0
            while (true) {
                val page = when (val result = fetchPage(api, device.id, since, offset)) {
                    is PageResult.Page -> result.sessions
                    is PageResult.Stopped -> return PassResult(added, devicesDone, full = false, stoppedBy = result.reason)
                }
                if (page.isEmpty()) break
                added += insertMissing(page, device, ownerUserId, resolve)
                if (page.size < PAGE_SIZE) break
                offset += page.size
            }
            devicesDone++
        }
        return PassResult(added, devicesDone, full = false)
    }

    private sealed interface PageResult {
        data class Page(val sessions: List<RomMPlaySession>) : PageResult
        data class Stopped(val reason: String) : PageResult
    }

    private suspend fun fetchPage(api: RomMApi, deviceId: String?, since: Instant?, offset: Int): PageResult =
        try {
            val response = api.getPlaySessions(
                deviceId = deviceId,
                startAfter = since?.toString(),
                limit = PAGE_SIZE,
                offset = offset
            )
            if (response.isSuccessful) {
                PageResult.Page(response.body().orEmpty())
            } else {
                PageResult.Stopped("Play sessions returned ${response.code()}")
            }
        } catch (e: Exception) {
            Logger.error(TAG, "run: page failed | device=$deviceId offset=$offset", e)
            PageResult.Stopped(e.message ?: "Network failure")
        }

    private fun unknownDevice(id: String) = RomMDevice(id = id, name = null, platform = null, client = null, clientVersion = null)

    /**
     * Inserts every session in [page] whose server id is not already held, filed under the
     * game [resolveGame] answers for its rom. Returns how many rows were written.
     */
    internal suspend fun insertMissing(
        page: List<RomMPlaySession>,
        device: RomMDevice,
        ownerUserId: Long,
        resolveGame: suspend (Long) -> PulledGameRef
    ): Int {
        val held = playSessionDao.getHeldRommSessionIds(page.map { it.id }).toSet()
        var added = 0
        var alreadyHeld = 0
        var noRom = 0
        var unmappable = 0
        var sameStart = 0
        for (remote in page) {
            if (remote.id in held) {
                alreadyHeld++
                continue
            }
            val romId = remote.romId
            if (romId == null) {
                noRom++
                continue
            }
            val entity = PlaySessionRemoteMapper.toEntity(remote, device, ownerUserId, resolveGame(romId))
            if (entity == null) {
                unmappable++
                continue
            }
            if (playSessionDao.existsForOwnerByGameAndStart(ownerUserId, entity.gameId, entity.startTime)) {
                sameStart++
                continue
            }
            playSessionDao.insert(entity)
            added++
        }
        if (added < page.size) {
            Logger.info(
                TAG,
                "insertMissing: device=${device.id} page=${page.size} added=$added held=$alreadyHeld " +
                    "noRom=$noRom unmappable=$unmappable sameStart=$sameStart sample=${page.firstOrNull()?.startTime}"
            )
        }
        return added
    }

    private suspend fun resolveGame(api: RomMApi, romId: Long): PulledGameRef {
        gameDao.getByRommId(romId)?.let { game ->
            return PulledGameRef(
                gameId = game.id,
                igdbId = game.igdbId,
                title = game.title,
                platformSlug = game.platformSlug
            )
        }
        val rom = try {
            api.getRom(romId).takeIf { it.isSuccessful }?.body()
        } catch (e: Exception) {
            Logger.debug(TAG, "resolveGame: rom $romId lookup failed | ${e.message}")
            null
        } ?: return PulledGameRef.unresolved(romId)
        return PulledGameRef(
            gameId = -romId,
            igdbId = rom.igdbId,
            title = rom.name,
            platformSlug = rom.platformSlug
        )
    }
}
