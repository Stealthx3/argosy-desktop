package com.nendo.argosy.platform

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        val name = "root-scripts/systemize-argosy.sh"
        val expected = androidAssets.resolve(name).readBytes()
        val actual = assets.open(name).use { it.readBytes() }
        assertTrue(expected.contentEquals(actual))
    }

    @Test
    fun svgClassStylesAreServedAsPresentationAttributes() {
        val android = androidAssets.resolve("platforms/gb.svg").readText()
        val desktop = assets.open("platforms/gb.svg").use { it.readBytes().decodeToString() }
        assertTrue("<style" in android)
        assertFalse("<style" in desktop)
        assertEquals(
            Regex("<path\\b").findAll(android).count(),
            Regex("<path\\b").findAll(desktop).count(),
        )
        assertTrue(Regex("class=\"cls-1\"[^>]*fill=\"#cccccb\"").containsMatchIn(desktop))
    }
}
