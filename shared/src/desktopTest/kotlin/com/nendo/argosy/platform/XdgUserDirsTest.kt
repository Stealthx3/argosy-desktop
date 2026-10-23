package com.nendo.argosy.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class XdgUserDirsTest {
    private val home = "/home/pat"

    @Test
    fun resolvesHomeRelativeAndAbsoluteEntries() {
        val text = """
            # This file is written by xdg-user-dirs-update
            XDG_DOWNLOAD_DIR="${'$'}HOME/Téléchargements"
            XDG_MUSIC_DIR="/mnt/media/music/"
        """.trimIndent()
        val dirs = XdgUserDirs.parse(text, home)
        assertEquals("/home/pat/Téléchargements", dirs["DOWNLOAD"])
        assertEquals("/mnt/media/music", dirs["MUSIC"])
    }

    @Test
    fun unescapesShellEscapes() {
        val dirs = XdgUserDirs.parse("XDG_MUSIC_DIR=\"\$HOME/My \\\"Tunes\\\"\"", home)
        assertEquals("/home/pat/My \"Tunes\"", dirs["MUSIC"])
    }

    @Test
    fun bareHomeMapsToHome() {
        assertEquals(home, XdgUserDirs.parse("XDG_DESKTOP_DIR=\"\$HOME/\"", home)["DESKTOP"])
    }

    @Test
    fun ignoresCommentsAndRelativeValues() {
        val dirs = XdgUserDirs.parse("# XDG_MUSIC_DIR=\"\$HOME/A\"\nXDG_VIDEOS_DIR=\"Videos\"", home)
        assertFalse("MUSIC" in dirs)
        assertFalse("VIDEOS" in dirs)
    }
}
