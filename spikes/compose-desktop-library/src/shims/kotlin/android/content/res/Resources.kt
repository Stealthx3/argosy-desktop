package android.content.res

import spike.res.ResourceTable

class Resources {
    fun getString(id: Int): String = ResourceTable.string(id)
    fun getString(id: Int, vararg formatArgs: Any?): String = ResourceTable.string(id, *formatArgs)
    fun getQuantityString(id: Int, quantity: Int): String = ResourceTable.quantity(id, quantity)
    fun getQuantityString(id: Int, quantity: Int, vararg formatArgs: Any?): String =
        ResourceTable.quantity(id, quantity, *formatArgs)
}

/** Reads the app's assets/ directory from the classpath (build copies app/src/main/assets in). */
class AssetManager {
    fun list(path: String): Array<String>? =
        AssetManager::class.java.getResourceAsStream("/assets/$path/.index")
            ?.bufferedReader()?.readLines()?.filter { it.isNotBlank() }?.toTypedArray()

    fun open(path: String): java.io.InputStream =
        AssetManager::class.java.getResourceAsStream("/assets/$path") ?: throw java.io.FileNotFoundException(path)
}

/** android.content.res.Configuration fields upstream reads; filled from the window size. */
class Configuration(
    @JvmField val screenWidthDp: Int,
    @JvmField val screenHeightDp: Int,
) {
    @JvmField val orientation: Int = if (screenWidthDp >= screenHeightDp) ORIENTATION_LANDSCAPE else ORIENTATION_PORTRAIT
    @JvmField val uiMode: Int = UI_MODE_NIGHT_YES
    @JvmField val smallestScreenWidthDp: Int = minOf(screenWidthDp, screenHeightDp)

    companion object {
        const val ORIENTATION_PORTRAIT = 1
        const val ORIENTATION_LANDSCAPE = 2
        const val UI_MODE_NIGHT_MASK = 0x30
        const val UI_MODE_NIGHT_YES = 0x20
    }
}
