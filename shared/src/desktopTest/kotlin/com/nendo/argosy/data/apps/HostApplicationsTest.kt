package com.nendo.argosy.data.apps

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostApplicationsTest {
    private val home = "/home/deck/.local/share"

    private val output = """
        @@BASE /home/deck/.local/share/applications
        @@FILE /home/deck/.local/share/applications/org.kde.konsole.desktop
        Type=Application
        Name=Konsole
        Hidden=true
        @@FILE /home/deck/.local/share/applications/steam/game.desktop
        Type=Application
        Name=Hollow Knight
        Exec=steam steam://rungameid/367520
        Categories=Game;
        @@BASE /var/lib/flatpak/exports/share/applications
        @@FILE /var/lib/flatpak/exports/share/applications/org.libretro.RetroArch.desktop
        Type=Application
        Name=RetroArch
        Name[de]=RetroArch DE
        Exec=/usr/bin/flatpak run --branch=stable --command=retroarch org.libretro.RetroArch %f
        Icon=org.libretro.RetroArch
        Categories=Game;Emulator;
        @@BASE /usr/share/applications
        @@FILE /usr/share/applications/org.kde.konsole.desktop
        Type=Application
        Name=Konsole
        Exec=konsole
        @@FILE /usr/share/applications/firefox.desktop
        Type=Application
        Name=Firefox
        Exec=firefox %u
        TryExec=firefox
        @@FILE /usr/share/applications/gnome-only.desktop
        Type=Application
        Name=Gnome Thing
        Exec=gnome-thing
        OnlyShowIn=GNOME;
        @@FILE /usr/share/applications/htop.desktop
        Type=Application
        Name=Htop
        Exec=htop
        Terminal=true
        @@FILE /usr/share/applications/helper.desktop
        Type=Application
        Name=Helper
        Exec=helper
        NoDisplay=true
        @@FILE /usr/share/applications/link.desktop
        Type=Link
        Name=Website
        URL=https://example.com
    """.trimIndent()

    private fun visible(missing: Set<String> = emptySet(), locale: Locale = Locale.ENGLISH) =
        HostApplications.selectVisible(HostApplications.parseEntries(output, home), listOf("KDE"), missing, locale)

    @Test
    fun ids_follow_the_path_below_applications() {
        val ids = HostApplications.parseEntries(output, home).map { it.id }
        assertTrue("steam-game" in ids)
        assertTrue("org.libretro.RetroArch" in ids)
    }

    @Test
    fun higher_precedence_hidden_entry_masks_the_system_one() {
        assertFalse(visible().any { it.id == "org.kde.konsole" })
    }

    @Test
    fun visibility_rules_drop_nodisplay_terminal_links_and_other_desktops() {
        assertEquals(
            setOf("steam-game", "org.libretro.RetroArch", "firefox"),
            visible().map { it.id }.toSet()
        )
    }

    @Test
    fun missing_try_exec_hides_the_entry() {
        assertFalse(visible(missing = setOf("firefox")).any { it.id == "firefox" })
    }

    @Test
    fun flatpak_and_user_entries_are_not_system_apps() {
        val apps = visible().associateBy { it.id }
        assertFalse(apps.getValue("org.libretro.RetroArch").isSystem)
        assertFalse(apps.getValue("steam-game").isSystem)
        assertTrue(apps.getValue("firefox").isSystem)
        assertTrue(apps.getValue("org.libretro.RetroArch").isGame)
    }

    @Test
    fun localized_name_wins_when_present() {
        val retroArch = visible(locale = Locale.GERMANY).first { it.id == "org.libretro.RetroArch" }
        assertEquals("RetroArch DE", retroArch.name)
    }

    @Test
    fun exec_drops_file_field_codes() {
        val retroArch = visible().first { it.id == "org.libretro.RetroArch" }
        assertEquals(
            listOf("/usr/bin/flatpak", "run", "--branch=stable", "--command=retroarch", "org.libretro.RetroArch"),
            retroArch.exec
        )
    }

    @Test
    fun exec_quoting_and_expansions() {
        assertEquals(
            listOf("sh", "-c", "echo \"a b\" \$HOME", "--icon", "app", "My App", "50%"),
            DesktopEntryValues.execArguments(
                """sh -c "echo \\"a b\\" \\${'$'}HOME" %i %c 50%%""",
                icon = "app",
                name = "My App",
                entryPath = "/x.desktop"
            )
        )
    }

    @Test
    fun file_field_codes_take_the_given_files() {
        val files = listOf("/roms/a b.iso", "/roms/c.iso")
        assertEquals(
            listOf("app", "/roms/a b.iso", "--all", "/roms/a b.iso", "/roms/c.iso", "file:///roms/a%20b.iso"),
            DesktopEntryValues.execArguments("app %f --all %F %u", null, "App", "/x.desktop", files)
        )
        assertEquals(listOf("app"), DesktopEntryValues.execArguments("app %U", null, "App", "/x.desktop"))
        assertEquals(true, DesktopEntryValues.takesFiles("app %U"))
        assertEquals(false, DesktopEntryValues.takesFiles("app \"%f\" --flag"))
    }

    @Test
    fun icon_choice_prefers_large_hicolor_png() {
        val best = HostIcons.bestByName(
            listOf(
                "/usr/share/icons/breeze/apps/48/retroarch.svg",
                "/usr/share/icons/hicolor/64x64/apps/retroarch.png",
                "/usr/share/icons/hicolor/scalable/apps/retroarch.svg",
                "/usr/share/icons/hicolor/256x256/apps/retroarch.png",
                "/usr/share/pixmaps/retroarch.png"
            )
        )
        assertEquals("/usr/share/icons/hicolor/256x256/apps/retroarch.png", best["retroarch"])
    }
}
