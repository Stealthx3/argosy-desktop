package com.nendo.argosy.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformBitmapTest {
    @Test
    fun setPixelsWritesArgbValues() {
        val bitmap = PlatformBitmaps.createBitmap(2, 2, PlatformBitmapConfig.ARGB_8888)
        val pixels = intArrayOf(0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF0000.toInt(), 0x00000000)
        bitmap.setPixels(pixels, 0, 2, 0, 0, 2, 2)
        assertEquals(0xFF000000.toInt(), bitmap.getPixel(0, 0))
        assertEquals(0xFFFFFFFF.toInt(), bitmap.getPixel(1, 0))
        assertEquals(0xFFFF0000.toInt(), bitmap.getPixel(0, 1))
        assertEquals(0, bitmap.getPixel(1, 1))
    }
}
