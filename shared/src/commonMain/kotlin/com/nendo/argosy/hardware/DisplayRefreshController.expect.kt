package com.nendo.argosy.hardware

expect class DisplayRefreshController {
    fun supportedRatesHz(): List<Int>

    fun currentRateHz(): Int?

    fun setRateHz(hz: Int?): Boolean
}
