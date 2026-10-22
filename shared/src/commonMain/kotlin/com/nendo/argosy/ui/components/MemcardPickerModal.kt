package com.nendo.argosy.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.nendo.argosy.platform.LocalPlatformContext as LocalContext
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import com.nendo.argosy.res.stringResource
import com.nendo.argosy.res.ArgosyR as R
import com.nendo.argosy.data.sync.platform.MemcardInfo
import com.nendo.argosy.ui.screens.gamedetail.components.OptionItem
import com.nendo.argosy.platform.PlatformContext as Context
import com.nendo.argosy.platform.getLaunchIntentForPackage
import com.nendo.argosy.platform.addFlags
import com.nendo.argosy.platform.externalCacheDir
import com.nendo.argosy.platform.dataDir
import com.nendo.argosy.platform.startActivity
import com.nendo.argosy.platform.getApplicationInfo
import com.nendo.argosy.platform.getApplicationLabel
import com.nendo.argosy.platform.applicationContext
import com.nendo.argosy.platform.packageName
import com.nendo.argosy.platform.getSystemService
import com.nendo.argosy.platform.getExternalFilesDir
import com.nendo.argosy.platform.packageManager
import com.nendo.argosy.platform.getPackageInfo
import com.nendo.argosy.platform.versionName
import com.nendo.argosy.platform.assets
import com.nendo.argosy.platform.contentResolver
import com.nendo.argosy.platform.getSharedPreferences
import com.nendo.argosy.platform.list
import com.nendo.argosy.platform.open
import com.nendo.argosy.platform.cacheDir
import com.nendo.argosy.platform.filesDir
import com.nendo.argosy.platform.getQuantityString
import com.nendo.argosy.platform.getString
import com.nendo.argosy.platform.resources
import java.text.DateFormat
import java.util.Date

@Composable
fun MemcardPickerModal(
    cards: List<MemcardInfo>,
    focusIndex: Int,
    selectedCardPath: String?,
    onSelectCard: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    val context = LocalContext.current
    Modal(
        title = stringResource(R.string.ui_memcard_picker_title),
        subtitle = stringResource(R.string.ui_memcard_picker_subtitle),
        onDismiss = onDismiss
    ) {
        val listState = rememberLazyListState()
        FocusedScroll(listState = listState, focusedIndex = focusIndex)

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            itemsIndexed(cards) { index, card ->
                OptionItem(
                    label = defaultCardLabel(context, card),
                    value = summarize(context, card, dateFormat),
                    isFocused = focusIndex == index,
                    isSelected = card.path == selectedCardPath,
                    onClick = { onSelectCard(card.path) }
                )
            }
        }
    }
}

private fun defaultCardLabel(context: Context, card: MemcardInfo): String {
    if (!card.isDefault) return card.name
    return if (card.name.isNotBlank()) {
        context.getString(R.string.ui_memcard_picker_default_named, card.name)
    } else {
        context.getString(R.string.ui_memcard_picker_default)
    }
}

private fun summarize(context: Context, card: MemcardInfo, dateFormat: DateFormat): String {
    val games = if (card.gameFolderCount == 0) {
        context.getString(R.string.ui_memcard_picker_empty)
    } else {
        context.resources.getQuantityString(
            R.plurals.ui_memcard_picker_save_count,
            card.gameFolderCount,
            card.gameFolderCount
        )
    }
    return if (card.lastModified > 0) {
        context.getString(
            R.string.ui_memcard_picker_summary_dated,
            games,
            dateFormat.format(Date(card.lastModified))
        )
    } else {
        games
    }
}
