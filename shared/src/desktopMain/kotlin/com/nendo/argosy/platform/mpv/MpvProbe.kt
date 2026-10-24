package com.nendo.argosy.platform.mpv

/**
 * Plays [path] without a window for [seconds] and reports what libmpv produced, for checking a
 * build where only the Flatpak has libmpv: `argosy-desktop --probe-video=<file>`.
 */
object MpvProbe {
    fun run(path: String, seconds: Long = 4): String {
        val player = MpvPlayer.create(video = true, muted = true) ?: return "probe: libmpv unavailable"
        player.resize(PROBE_WIDTH, PROBE_HEIGHT)
        player.load(path)
        Thread.sleep(seconds * MILLIS)
        var center = "none"
        player.drawFront { bitmap -> center = "%06x".format(bitmap.getColor(PROBE_WIDTH / 2, PROBE_HEIGHT / 2) and RGB_MASK) }
        val result = "probe: frames=${player.frame.value} position=${player.positionMs()}ms center=#$center"
        player.release()
        return result
    }

    private const val PROBE_WIDTH = 320
    private const val PROBE_HEIGHT = 180
    private const val MILLIS = 1000L
    private const val RGB_MASK = 0xFFFFFF
}
