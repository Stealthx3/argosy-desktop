package com.nendo.argosy.data.emulator

actual object RetroArchHostLayout {
    /**
     * The first four are the canonical ones and their order is load-bearing: an install whose
     * config is found today must keep resolving to the same file. Entries after them are the
     * layouts that only apply when those are absent.
     *
     * Upstream sources:
     * - `UserPreferences.getDefaultConfigPath` writes `getExternalFilesDir(null)/retroarch.cfg`,
     *   which is `Android/data/<package>/files/` on primary external storage.
     * - `platform_unix.c` (`frontend_android_get_env`) builds every default directory under a
     *   parent path, and places saved configurations in its `config` subdirectory.
     * - That parent path is `<external root>/RetroArch` when external storage is writable, and
     *   `MainMenuActivity.startRetroActivity` supplies `getExternalMediaDirs()[0]` as the
     *   external root for Play Store builds and for any build without all-files access, which
     *   puts the tree under `Android/media/<package>/`.
     */
    actual fun configPaths(primaryRoot: String, packages: List<String>): List<String> = buildList {
        packages.forEach { add("$primaryRoot/Android/data/$it/files/retroarch.cfg") }
        add("$primaryRoot/RetroArch/retroarch.cfg")
        add("$primaryRoot/RetroArch/config/retroarch.cfg")
        packages.forEach { pkg ->
            add("$primaryRoot/Android/media/$pkg/RetroArch/retroarch.cfg")
            add("$primaryRoot/Android/media/$pkg/RetroArch/config/retroarch.cfg")
            add("$primaryRoot/Android/data/$pkg/files/config/retroarch.cfg")
        }
    }

    actual fun packageConfigPath(primaryRoot: String, packageName: String): String? = when (packageName) {
        "com.retroarch" -> "$primaryRoot/Android/data/com.retroarch/files/retroarch.cfg"
        "com.retroarch.aarch64" -> "$primaryRoot/Android/data/com.retroarch.aarch64/files/retroarch.cfg"
        "com.retroarch.ra32" -> "$primaryRoot/Android/data/com.retroarch.ra32/files/retroarch.cfg"
        else -> null
    }

    actual fun portableConfigPath(primaryRoot: String): String? = "$primaryRoot/RetroArch/retroarch.cfg"

    actual fun defaultDirectory(primaryRoot: String, name: String): String = "$primaryRoot/RetroArch/$name"

    actual fun expandPath(value: String): String = value
}
