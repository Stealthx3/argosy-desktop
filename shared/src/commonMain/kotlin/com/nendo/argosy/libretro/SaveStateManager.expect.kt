package com.nendo.argosy.libretro

import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import com.nendo.argosy.data.repository.ActiveSaveRepository
import com.nendo.argosy.data.repository.SaveCacheManager
import java.io.File

expect class SaveStateManager(savesDir: File, statesDir: File, romPath: String, gameId: Long, activeSaveRepository: ActiveSaveRepository, saveCacheManager: SaveCacheManager, usesExternalMemcard: Boolean = false, channelName: String? = null, isVariant: Boolean = false, primarySavePath: String? = null, onLiveStateWritten: ((Int, File) -> Unit)? = null, onLiveStateRemoved: ((Int, File) -> Unit)? = null) {
    class RestoreResult {
        val sramData: ByteArray?
        val switchToHardcore: Boolean
        val casualSaveInHardcore: Boolean
        val rtcData: ByteArray?
    }

    class SlotInfo {
        val slotNumber: Int
        val file: File?
        val timestamp: Long?
        val size: Long
        val screenshotFile: File?
    }

    companion object {}

    fun getRtcFile(): File

    fun withRtcFromDisk(result: RestoreResult): RestoreResult

    fun initializeFromExistingSave(existingSram: ByteArray?)

    fun getSlotFile(slotNumber: Int): File

    fun getSlotScreenshotFile(slotNumber: Int): File

    fun getSramFile(): File

    fun watchCoreSaveFiles(files: List<File>)

    fun seedWrittenSaves()

    fun takeNewlyWrittenSaves(): Boolean

    fun adoptLegacySaveIfMissing()

    fun adoptSharedSegaCdBramIfMissing()

    fun getSlotInfoList(): List<SlotInfo>

    suspend fun restoreSaveForLaunchMode(launchMode: LaunchMode): RestoreResult

    fun getQuickRingInfoList(): List<SlotInfo>

    fun performQuickSave(stateData: ByteArray, screenshot: Bitmap? = null): Boolean

    fun performSlotSave(slotNumber: Int, stateData: ByteArray, screenshot: Bitmap? = null): Boolean

    fun deleteSlot(slotNumber: Int): Boolean
}
