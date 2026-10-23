package com.nendo.argosy.data.emulator

import com.nendo.argosy.data.storage.DesktopFileAccessLayer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RetroArchHostLayoutTest {
    private val home = System.getProperty("user.home")
    private val parser = RetroArchConfigParser(DesktopFileAccessLayer())

    @Test
    fun flatpakConfigIsSearchedFirst() {
        val paths = RetroArchHostLayout.configPaths("", emptyList())
        assertEquals("$home/.var/app/org.libretro.RetroArch/config/retroarch/retroarch.cfg", paths.first())
        assertTrue("$home/.config/retroarch/retroarch.cfg" in paths)
    }

    @Test
    fun homeRelativePathsExpand() {
        assertEquals("$home/.config/retroarch/saves", RetroArchHostLayout.expandPath("~/.config/retroarch/saves"))
        assertEquals("/srv/saves", RetroArchHostLayout.expandPath("/srv/saves"))
    }

    @Test
    fun linuxConfigResolvesToItsSaveFolder() {
        val cfg = File.createTempFile("retroarch", ".cfg")
        try {
            cfg.writeText(
                """
                savefile_directory = "~/.var/app/org.libretro.RetroArch/config/retroarch/saves"
                savefiles_in_content_dir = "false"
                sort_savefiles_enable = "false"
                sort_savefiles_by_content_enable = "false"
                libretro_path = "~/.var/app/org.libretro.RetroArch/config/retroarch/cores/snes9x_libretro.so"
                """.trimIndent()
            )
            val config = parser.parseFile(cfg)
            assertEquals("snes9x", config.lastLoadedCore)
            val paths = parser.resolveSavePathsWithConfig(config, "snes", "snes9x")
            assertEquals("$home/.var/app/org.libretro.RetroArch/config/retroarch/saves", paths.first())
        } finally {
            cfg.delete()
        }
    }
}
