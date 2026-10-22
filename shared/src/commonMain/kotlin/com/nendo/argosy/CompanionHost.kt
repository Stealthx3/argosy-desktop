package com.nendo.argosy

interface CompanionHost {
    fun onForegroundChanged(isForeground: Boolean)
    fun onSessionStarted(gameId: Long, isHardcore: Boolean, channelName: String?)
    fun onSessionHardcoreChanged(isHardcore: Boolean, channelName: String?)
    fun onSessionEnded()
    fun onLibraryRefresh()
    fun onAccountSwitched()
    fun onOverlayRequested(eventName: String)
    fun onRoleSwapped(isSwapped: Boolean)
    fun onOverlayClosed()
    fun onBackgroundForward()
    /**
     * A key from the primary display. [action] and [repeatCount] come straight from the source
     * event because the companion cannot tell a tap from a hold without them, and a forwarded
     * down-only stream makes every press look instantaneous.
     */
    fun onForwardKey(
        keyCode: Int,
        action: Int,
        repeatCount: Int,
        swapAB: Boolean,
        swapXY: Boolean,
        swapStartSelect: Boolean
    )
    fun refocusSelf()
    fun onDownloadCompleted(gameId: Long)
    fun finishCompanion()
}
