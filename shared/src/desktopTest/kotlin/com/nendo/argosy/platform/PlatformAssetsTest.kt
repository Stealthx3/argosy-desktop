package com.nendo.argosy.platform

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformAssetsTest {

    private val assets = DesktopPlatformContext().assets
    private val androidAssets = File("src/androidMain/assets")

    @Test
    fun listingMatchesTheAndroidAssetFolder() {
        val expected = androidAssets.resolve("platforms").list()!!.sorted()
        assertEquals(expected, assets.list("platforms")!!.sorted())
    }

    @Test
    fun openReturnsTheSameBytesAsTheAndroidAsset() {
        val name = androidAssets.resolve("platforms").list()!!.sorted().first()
        val expected = androidAssets.resolve("platforms/$name").readBytes()
        val actual = assets.open("platforms/$name").use { it.readBytes() }
        assertTrue(expected.contentEquals(actual))
    }
}
