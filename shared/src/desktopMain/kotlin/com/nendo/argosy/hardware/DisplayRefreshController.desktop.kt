package com.nendo.argosy.hardware

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class DisplayRefreshController @Inject constructor() {
    actual fun supportedRatesHz(): List<Int> = emptyList()

    actual fun currentRateHz(): Int? = null

    actual fun setRateHz(hz: Int?): Boolean = false
}
