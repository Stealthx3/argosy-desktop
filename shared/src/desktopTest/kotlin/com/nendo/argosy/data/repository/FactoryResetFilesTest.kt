package com.nendo.argosy.data.repository

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FactoryResetFilesTest {
    private val base = Files.createTempDirectory("factory-prune").toFile()
    private val defaultRoot = base.resolve("Games/Argosy").apply { mkdirs() }
    private val customRoot = base.resolve("MyRoms").apply { mkdirs() }

    private fun prune(vararg deleted: java.io.File) = pruneEmptyParents(
        deleted = deleted.toList(),
        roots = setOf(defaultRoot, customRoot),
        removableRoots = setOf(defaultRoot)
    )

    @Test
    fun emptyPlatformFolderAndDefaultRootGo() {
        val nes = defaultRoot.resolve("nes").apply { mkdirs() }
        prune(nes.resolve("game.nes"))
        assertFalse(nes.exists())
        assertFalse(defaultRoot.exists())
        assertTrue(base.resolve("Games").exists())
    }

    @Test
    fun folderHoldingAnotherFileStays() {
        val nes = defaultRoot.resolve("nes").apply { mkdirs() }
        nes.resolve("mine.nes").writeText("x")
        prune(nes.resolve("game.nes"))
        assertTrue(nes.resolve("mine.nes").exists())
        assertTrue(defaultRoot.exists())
    }

    @Test
    fun customRootStaysWhenEmptied() {
        val snes = customRoot.resolve("snes").apply { mkdirs() }
        prune(snes.resolve("game.sfc"))
        assertFalse(snes.exists())
        assertTrue(customRoot.exists())
    }

    @Test
    fun pathOutsideEveryRootIsIgnored() {
        val outside = base.resolve("elsewhere").apply { mkdirs() }
        prune(outside.resolve("game.nes"))
        assertTrue(outside.exists())
    }
}
