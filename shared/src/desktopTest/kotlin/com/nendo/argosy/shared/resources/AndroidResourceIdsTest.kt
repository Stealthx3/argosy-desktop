package com.nendo.argosy.shared.resources

import com.nendo.argosy.res.ArgosyR as R
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getDrawableResourceBytes
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getSystemResourceEnvironment
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidResourceIdsTest {

    private val original: Locale = Locale.getDefault()

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(original)
    }

    private fun string(id: Int, locale: Locale): String = runBlocking {
        Locale.setDefault(locale)
        getString(AndroidResourceIds.string(id))
    }

    private fun plural(id: Int, count: Int, locale: Locale): String = runBlocking {
        Locale.setDefault(locale)
        getPluralString(AndroidResourceIds.plurals(id), count)
    }

    @Test
    fun stringIdsResolveToTheAndroidTextPerLocale() {
        assertEquals("Choose a video on this device", string(R.string.home_grid_local_video_row_title, Locale.ENGLISH))
        assertEquals("Ein Video auf diesem Gerät auswählen", string(R.string.home_grid_local_video_row_title, Locale.GERMAN))
        assertEquals("选择此设备上的视频", string(R.string.home_grid_local_video_row_title, Locale.SIMPLIFIED_CHINESE))
    }

    @Test
    fun formatPlaceholdersSurviveForAndroidStyleFormatting() {
        val raw = string(R.string.common_homegrid_playtime_hours_minutes, Locale.ENGLISH)
        assertEquals("%1\$dh %2\$dm", raw)
        assertEquals("3h 5m", String.format(raw, 3, 5))
    }

    @Test
    fun pluralIdsPickTheLocaleQuantity() {
        assertEquals("%d game", plural(R.plurals.firstrun_romm_success_game_count, 1, Locale.ENGLISH))
        assertEquals("%d games", plural(R.plurals.firstrun_romm_success_game_count, 4, Locale.ENGLISH))
        assertEquals("%d Spiele", plural(R.plurals.firstrun_romm_success_game_count, 4, Locale.GERMAN))
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun drawableIdsLoadTheVectorXml() = runBlocking {
        val bytes = getDrawableResourceBytes(getSystemResourceEnvironment(), AndroidResourceIds.drawable(R.drawable.ic_check))
        assertTrue(bytes.decodeToString().contains("<vector"))
    }
}
