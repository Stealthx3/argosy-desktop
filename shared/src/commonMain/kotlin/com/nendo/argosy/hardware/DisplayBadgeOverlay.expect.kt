package com.nendo.argosy.hardware

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources

expect class DisplayBadgeOverlay(context: Context) {
    fun show(numbers: Map<Int, Int>, size: DisplayBadgeSize)

    fun hide()
}
