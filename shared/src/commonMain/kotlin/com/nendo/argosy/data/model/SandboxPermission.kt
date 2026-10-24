package com.nendo.argosy.data.model

/**
 * A Flatpak permission Argosy uses on the desktop. [flag] is the `flatpak override` argument that
 * grants it; portal permissions ([isPortal]) need no grant.
 */
enum class SandboxPermission(val flag: String, val isPortal: Boolean = false) {
    GAMES_FOLDER("--filesystem=~/Games/Argosy:create"),
    EMULATOR_FOLDERS("--filesystem=~/Emulation"),
    HOST_ACCESS("--talk-name=org.freedesktop.Flatpak"),
    NOTIFICATIONS("org.freedesktop.portal.Notification", isPortal = true),
    SCREENSHOTS("org.freedesktop.portal.Screenshot", isPortal = true),
    CONTROLLERS("--device=all"),
    NETWORK("--share=network"),
    AUDIO("--socket=pulseaudio"),
    GPU("--device=dri"),
    DISPLAY("--socket=wayland");

    companion object {
        const val APP_ID = "io.github.stealthx3.ArgosyDesktop"

        fun overrideArgs(flags: List<String>): List<String> = listOf("flatpak", "override", "--user") + flags + APP_ID
    }
}

/**
 * What the sandbox grants Argosy. Outside a Flatpak ([sandboxed] false) nothing is restricted.
 * [missingFlags] lists the override arguments that would grant each missing permission.
 */
data class SandboxPermissionReport(
    val sandboxed: Boolean = false,
    val granted: Map<SandboxPermission, Boolean> = emptyMap(),
    val missingFlags: Map<SandboxPermission, List<String>> = emptyMap()
)
