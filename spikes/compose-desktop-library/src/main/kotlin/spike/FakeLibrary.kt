package spike

import com.nendo.argosy.data.model.GameSource
import com.nendo.argosy.data.social.FriendActivity
import com.nendo.argosy.domain.model.CompletionProgress
import com.nendo.argosy.ui.common.GameListDetails
import com.nendo.argosy.ui.screens.library.LibraryGameUi
import com.nendo.argosy.ui.screens.library.LibraryGridItem
import com.nendo.argosy.ui.screens.library.LibraryUiState
import java.io.File

/** Fake library: homebrew titles, generated placeholder covers (not real box art), upstream's own state types. */
object FakeLibrary {
    private data class Row(
        val title: String, val slug: String, val platform: String, val year: Int, val genre: String,
        val rating: Float?, val playMinutes: Int, val achievements: Pair<Int, Int>, val downloaded: Boolean,
        val hue: Int,
    )

    private val rows = listOf(
        Row("Battle Kid: Fortress of Peril", "nes", "Nintendo Entertainment System", 2010, "Platform", 78f, 312, 12 to 40, true, 200),
        Row("Böxxle", "gb", "Game Boy", 2009, "Puzzle", 71f, 45, 0 to 0, true, 95),
        Row("Kira Kira Star Night DX", "nes", "Nintendo Entertainment System", 2019, "Action", 74f, 0, 0 to 0, false, 280),
        Row("Micro Mages", "nes", "Nintendo Entertainment System", 2019, "Platform", 88f, 640, 22 to 30, true, 10),
        Row("Tanglewood", "genesis", "Sega Genesis", 2018, "Puzzle", 82f, 95, 3 to 25, true, 130),
        Row("Twin Dragons", "nes", "Nintendo Entertainment System", 2017, "Platform", 70f, 0, 0 to 0, false, 330),
        Row("Xeno Crisis", "genesis", "Sega Genesis", 2019, "Shooter", 85f, 1210, 31 to 31, true, 0),
        Row("Yo-Yo Shuriken", "gb", "Game Boy", 2021, "Action", 76f, 20, 1 to 18, true, 50),
    )

    fun state(coverDir: File): LibraryUiState {
        val games = rows.mapIndexed { i, r ->
            LibraryGameUi(
                id = i + 1L,
                title = r.title,
                sortTitle = r.title.lowercase(),
                platformId = r.slug.hashCode().toLong(),
                platformSlug = r.slug,
                platformDisplayName = r.platform,
                coverPath = cover(coverDir, i + 1L, r.title, r.hue).absolutePath,
                source = if (r.downloaded) GameSource.ROMM_SYNCED else GameSource.ROMM_REMOTE,
                isFavorite = i % 3 == 0,
                isDownloaded = r.downloaded,
                isRommGame = true,
                isAndroidApp = false,
                emulatorName = null,
                listDetails = GameListDetails(
                    releaseYear = r.year,
                    genre = r.genre,
                    rating = r.rating,
                    userRating = if (i % 2 == 0) 4 else 0,
                    achievementCount = r.achievements.second,
                    earnedAchievementCount = r.achievements.first,
                    completion = CompletionProgress.resolve(0, r.playMinutes, 6 * 3600),
                    playTimeMinutes = r.playMinutes,
                    timeToBeatMainSec = 6 * 3600,
                    igdbId = if (i == 3) 1L else null,
                ),
            )
        }
        val grid = buildList {
            games.groupBy { it.sortTitle.first().uppercaseChar() }.forEach { (letter, inSection) ->
                add(LibraryGridItem.Header(letter.toString()))
                inSection.forEach { add(LibraryGridItem.Game(it, games.indexOf(it))) }
            }
        }
        return LibraryUiState(
            games = games,
            gridItems = grid,
            focusedIndex = 3,
            friendsActivity = mapOf(1 to listOf(FriendActivity("f1", "Rin", "#E8A33D", null, playingNow = true))),
        )
    }

    /** A flat, clearly-fake cover: two-tone gradient with the title, so the image path is exercised. */
    private fun cover(dir: File, id: Long, title: String, hue: Int): File {
        val file = File(dir, "cover-$id.png")
        if (file.isFile) return file
        dir.mkdirs()
        val img = java.awt.image.BufferedImage(264, 352, java.awt.image.BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
        g.paint = java.awt.GradientPaint(
            0f, 0f, java.awt.Color.getHSBColor(hue / 360f, 0.55f, 0.75f),
            0f, 352f, java.awt.Color.getHSBColor(hue / 360f, 0.7f, 0.3f),
        )
        g.fillRect(0, 0, 264, 352)
        g.color = java.awt.Color.WHITE
        g.font = java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, 26)
        title.split(' ').chunked(2).forEachIndexed { line, words ->
            g.drawString(words.joinToString(" "), 16, 220 + line * 32)
        }
        g.dispose()
        javax.imageio.ImageIO.write(img, "png", file)
        return file
    }
}
