package com.nendo.argosy.data.launcher

/**
 * User-facing grouping for the Mark-as-Installed modal. GameHub variants
 * (proper + every GH Lite flavor) collapse into one option: the specific
 * package stored is whichever variant is installed first.
 */
sealed class MarkOption(val launcherPackage: String, val displayName: String) {
    object GameNative : MarkOption(GameNativeLauncher.packageName, GameNativeLauncher.displayName)
    class GameHub(launcherPackage: String) : MarkOption(launcherPackage, "GameHub")
    object DroidDeck : MarkOption(DroidDeckLauncher.packageName, DroidDeckLauncher.displayName)
}
