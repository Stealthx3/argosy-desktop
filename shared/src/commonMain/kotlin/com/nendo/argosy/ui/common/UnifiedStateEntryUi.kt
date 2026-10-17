package com.nendo.argosy.ui.common

import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.domain.model.UnifiedStateEntry

/**
 * The name a save-state slot is known by. `UnifiedStateEntry` lives in `domain/` and must not
 * import `R`, so the label is built here instead of on the model.
 */
fun UnifiedStateEntry.displayName(context: Context): String = when {
    isAutoSlot -> context.getString(R.string.unified_state_entry_display_name_auto)
    channelName != null -> context.getString(
        R.string.unified_state_entry_display_name_channel,
        channelName,
        slotNumber
    )
    else -> context.getString(R.string.unified_state_entry_display_name_slot, slotNumber)
}

fun UnifiedStateEntry.slotLabel(context: Context): String = when {
    isAutoSlot -> context.getString(R.string.unified_state_entry_slot_label_auto)
    else -> slotNumber.toString()
}

fun UnifiedStateEntry.sizeFormatted(context: Context): String = when {
    size < 1024L -> context.getString(R.string.unified_state_entry_size_bytes, size)
    size < 1024L * 1024L -> context.getString(R.string.unified_state_entry_size_kb, size / 1024L)
    else -> context.getString(
        R.string.unified_state_entry_size_mb,
        size / (1024.0 * 1024.0)
    )
}
