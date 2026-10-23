package com.nendo.argosy.data.emulator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FlatpakFilesystemsTest {
    private val home = "/home/pat"
    private val folder = "/home/pat/Games/Argosy"

    @Test
    fun parsesTheFilesystemsLine() {
        val output = "[Context]\nshared=network;\nfilesystems=xdg-run/pipewire-0;host;~/Games:ro;\n"
        assertEquals(listOf("xdg-run/pipewire-0", "host", "~/Games:ro"), FlatpakFilesystems.parse(output))
    }

    @Test
    fun hostHomeAndParentFoldersCoverTheGamesFolder() {
        assertTrue(FlatpakFilesystems.covers(listOf("host"), folder, home))
        assertTrue(FlatpakFilesystems.covers(listOf("home"), folder, home))
        assertTrue(FlatpakFilesystems.covers(listOf("~/Games:ro"), folder, home))
        assertTrue(FlatpakFilesystems.covers(listOf("/home/pat/Games/Argosy/"), folder, home))
    }

    @Test
    fun siblingsXdgNamesAndNegationsDoNot() {
        assertFalse(FlatpakFilesystems.covers(listOf("~/Games/ArgosyOld"), folder, home))
        assertFalse(FlatpakFilesystems.covers(listOf("xdg-download"), folder, home))
        assertFalse(FlatpakFilesystems.covers(emptyList(), folder, home))
        assertFalse(FlatpakFilesystems.covers(listOf("home", "!~/Games"), folder, home))
    }
}
