package com.nendo.argosy.data.emulator

/**
 * Where the host's RetroArch keeps `retroarch.cfg` and its default folders, and how a path
 * written in that config reads on this host. [RetroArchConfigParser] owns the parsing and the
 * search order's use; this answers only the platform-specific locations.
 */
expect object RetroArchHostLayout {
    fun configPaths(primaryRoot: String, packages: List<String>): List<String>

    fun packageConfigPath(primaryRoot: String, packageName: String): String?

    fun portableConfigPath(primaryRoot: String): String?

    fun defaultDirectory(primaryRoot: String, name: String): String

    fun expandPath(value: String): String
}
