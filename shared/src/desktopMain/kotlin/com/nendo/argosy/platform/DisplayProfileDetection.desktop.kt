package com.nendo.argosy.platform

import com.nendo.argosy.data.preferences.DisplayProfile
import java.io.File
import kotlin.math.sqrt

actual object DisplayProfileDetection {
    private val detected: DisplayProfile by lazy {
        classify(
            productName = readLine(DMI_DIR, "product_name"),
            vendor = readLine(DMI_DIR, "sys_vendor"),
            gamescope = DesktopWindowMode.isGamescope(System.getenv()),
            largestDiagonalInches = connectedEdids().mapNotNull(::diagonalInches).maxOrNull(),
        )
    }

    actual fun detect(): DisplayProfile? = detected

    fun classify(productName: String?, vendor: String?, gamescope: Boolean, largestDiagonalInches: Double?): DisplayProfile =
        when {
            isHandheld(productName, vendor) -> DisplayProfile.HANDHELD
            largestDiagonalInches != null && largestDiagonalInches >= TV_MIN_DIAGONAL_INCHES -> DisplayProfile.TV
            gamescope -> DisplayProfile.TV
            else -> DisplayProfile.DESKTOP
        }

    fun isHandheld(productName: String?, vendor: String?): Boolean =
        HANDHELD_PRODUCTS.any { productName?.contains(it, ignoreCase = true) == true } ||
            HANDHELD_VENDORS.any { vendor?.contains(it, ignoreCase = true) == true }

    fun diagonalInches(edid: ByteArray): Double? {
        if (edid.size < EDID_MIN_SIZE) return null
        val widthCm = edid[EDID_WIDTH_CM].toInt() and 0xFF
        val heightCm = edid[EDID_HEIGHT_CM].toInt() and 0xFF
        if (widthCm == 0 || heightCm == 0) return null
        return sqrt((widthCm * widthCm + heightCm * heightCm).toDouble()) / CM_PER_INCH
    }

    private fun connectedEdids(): List<ByteArray> =
        File(DRM_DIR).listFiles().orEmpty()
            .filter { File(it, "status").readTextOrNull()?.trim() == "connected" }
            .mapNotNull { runCatching { File(it, "edid").readBytes() }.getOrNull()?.takeIf { bytes -> bytes.isNotEmpty() } }

    private fun readLine(dir: String, name: String): String? = File(dir, name).readTextOrNull()?.trim()

    private fun File.readTextOrNull(): String? = runCatching { readText() }.getOrNull()

    private const val DMI_DIR = "/sys/class/dmi/id"
    private const val DRM_DIR = "/sys/class/drm"
    private const val EDID_MIN_SIZE = 23
    private const val EDID_WIDTH_CM = 21
    private const val EDID_HEIGHT_CM = 22
    private const val CM_PER_INCH = 2.54
    private const val TV_MIN_DIAGONAL_INCHES = 40.0
    private val HANDHELD_PRODUCTS = listOf("Jupiter", "Galileo", "RC71L", "RC72L", "83E1", "83L3", "83N6")
    private val HANDHELD_VENDORS = listOf("AYANEO", "ONE-NETBOOK", "GPD", "AYN")
}
