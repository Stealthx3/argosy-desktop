package com.nendo.argosy.platform

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FactoryResetWipeTest {
    private val base = Files.createTempDirectory("factory-reset").toFile()
    private val data = base.resolve("data").apply { resolve("db").mkdirs(); resolve("db/a").writeText("x") }
    private val cache = base.resolve("cache").apply { mkdirs(); resolve("b").writeText("x") }
    private val other = base.resolve("other").apply { mkdirs(); resolve("c").writeText("x") }
    private val marker = base.resolve("marker")

    @Test
    fun withoutMarkerNothingIsDeleted() {
        assertFalse(FactoryResetWipe.wipe(marker, listOf(data, cache)))
        assertTrue(data.resolve("db/a").exists())
        assertTrue(cache.resolve("b").exists())
    }

    @Test
    fun markerWipesOnlyTheListedFoldersAndIsRemovedLast() {
        marker.writeText("")
        assertTrue(FactoryResetWipe.wipe(marker, listOf(data, cache)))
        assertFalse(data.exists())
        assertFalse(cache.exists())
        assertFalse(marker.exists())
        assertTrue(other.resolve("c").exists())
    }
}
