package com.nendo.argosy.libretro

import com.nendo.argosy.platform.PlatformBitmap as Bitmap
import com.nendo.argosy.data.repository.ActiveSaveRepository
import com.nendo.argosy.data.repository.SaveCacheManager
import java.io.File
import javax.inject.Inject

actual class SaveStateManager actual constructor(savesDir: File, statesDir: File, romPath: String, gameId: Long, activeSaveRepository: ActiveSaveRepository, saveCacheManager: SaveCacheManager, usesExternalMemcard: Boolean, channelName: String?, isVariant: Boolean, primarySavePath: String?, onLiveStateWritten: ((Int, File) -> Unit)?, onLiveStateRemoved: ((Int, File) -> Unit)?) {
    actual data class RestoreResult(
        actual val sramData: ByteArray?,
        actual val switchToHardcore: Boolean = false,
        actual val casualSaveInHardcore: Boolean = false,
        actual val rtcData: ByteArray? = null
    )

    actual data class SlotInfo(
        actual val slotNumber: Int,
        actual val file: File?,
        actual val timestamp: Long?,
        actual val size: Long,
        actual val screenshotFile: File?
    )

    actual companion object {}

    actual fun getRtcFile(): File = TODO("desktop: getRtcFile")

    actual fun withRtcFromDisk(result: RestoreResult): RestoreResult = TODO("desktop: withRtcFromDisk")

    actual fun initializeFromExistingSave(existingSram: ByteArray?) {
    }

    actual fun getSlotFile(slotNumber: Int): File = TODO("desktop: getSlotFile")

    actual fun getSlotScreenshotFile(slotNumber: Int): File = TODO("desktop: getSlotScreenshotFile")

    actual fun getSramFile(): File = TODO("desktop: getSramFile")

    actual fun watchCoreSaveFiles(files: List<File>) {
    }

    actual fun seedWrittenSaves() {
    }

    actual fun takeNewlyWrittenSaves(): Boolean = false

    actual fun adoptLegacySaveIfMissing() {
    }

    actual fun adoptSharedSegaCdBramIfMissing() {
    }

    actual fun getSlotInfoList(): List<SlotInfo> = emptyList()

    actual suspend fun restoreSaveForLaunchMode(launchMode: LaunchMode): RestoreResult = TODO("desktop: restoreSaveForLaunchMode")

    actual fun getQuickRingInfoList(): List<SlotInfo> = emptyList()

    actual fun performQuickSave(stateData: ByteArray, screenshot: Bitmap?): Boolean = false

    actual fun performSlotSave(slotNumber: Int, stateData: ByteArray, screenshot: Bitmap?): Boolean = false

    actual fun deleteSlot(slotNumber: Int): Boolean = false
}
