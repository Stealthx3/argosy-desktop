package com.nendo.argosy.data.emulator

import java.io.File

/**
 * RetroArch on Linux keeps its tree in `$XDG_CONFIG_HOME/retroarch` (`platform_unix.c`,
 * `frontend_unix_get_env`), and the Flatpak `org.libretro.RetroArch` gets its own
 * `XDG_CONFIG_HOME` under `~/.var/app`. The Flatpak comes first, matching detection's
 * Flatpak > AppImage > PATH order. RetroArch writes paths under the home folder as `~/...`.
 */
actual object RetroArchHostLayout {
    private const val FLATPAK_ID = "org.libretro.RetroArch"

    private val home: String get() = System.getProperty("user.home")

    private val treeRoots: List<String>
        get() = buildList {
            add("$home/.var/app/$FLATPAK_ID/config/retroarch")
            System.getenv("XDG_CONFIG_HOME")?.takeIf { it.startsWith("/") }?.let { add("$it/retroarch") }
            add("$home/.config/retroarch")
            add("$home/snap/retroarch/current/.config/retroarch")
        }.distinct()

    actual fun configPaths(primaryRoot: String, packages: List<String>): List<String> =
        treeRoots.map { "$it/retroarch.cfg" }

    actual fun packageConfigPath(primaryRoot: String, packageName: String): String? = null

    actual fun portableConfigPath(primaryRoot: String): String? = null

    actual fun defaultDirectory(primaryRoot: String, name: String): String {
        val root = treeRoots.firstOrNull { File(it, "retroarch.cfg").exists() } ?: treeRoots.first()
        return "$root/$name"
    }

    actual fun expandPath(value: String): String = when {
        value == "~" -> home
        value.startsWith("~/") -> home + value.substring(1)
        else -> value
    }
}
