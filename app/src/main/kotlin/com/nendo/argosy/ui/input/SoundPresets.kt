package com.nendo.argosy.ui.input

import com.nendo.argosy.R
import java.io.File

enum class SoundPreset(val resourceId: Int?, val displayName: String) {
    CLICK_SOFT(R.raw.click_soft, "Click Soft"),
    TAP_LIGHT(R.raw.tap_light, "Tap Light"),
    POP_CONFIRM(R.raw.pop_confirm, "Pop Confirm"),
    SWIPE_BACK(R.raw.swipe_back, "Swipe Back"),
    POP_CLOSE(R.raw.pop_close, "Pop Close"),
    BUZZ_ERROR(R.raw.buzz_error, "Buzz Error"),
    CHIME_OPEN(R.raw.chime_open, "Chime Open"),
    HOVER_SOFT(R.raw.hover_soft, "Hover Soft"),
    DING_PICKUP(R.raw.ding_pickup, "Ding Pickup"),
    NOTIFY_START(R.raw.notify_start, "Notify Start"),
    CHIME_SUCCESS(R.raw.chime_success, "Chime Success"),
    TICK_ACCEPT(R.raw.tick_accept, "Tick Accept"),
    BELL_HIGH(R.raw.bell_high, "Bell High"),
    COLLECT(R.raw.collect, "Collect"),
    DISMISS_FAIL(R.raw.dismiss_fail, "Dismiss Fail"),
    SILENT(null, "Silent"),
    CUSTOM(null, "Custom File"),
    ROMM_MUSIC(null, "RomM Music"),
    DEFAULT(null, "Default");
}
