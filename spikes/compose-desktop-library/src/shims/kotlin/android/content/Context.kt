package android.content

import android.content.res.AssetManager
import android.content.res.Resources
import java.io.File
import spike.res.ResourceTable

/** The slice of android.content.Context upstream's UI code reads: strings, resources, assets, files. */
open class Context {
    open val resources: Resources = Resources()
    open val assets: AssetManager = AssetManager()
    open val filesDir: File = File(System.getProperty("user.home"), ".local/share/argosy-desktop-spike")
    open val applicationContext: Context get() = this

    fun getString(resId: Int): String = ResourceTable.string(resId)
    fun getString(resId: Int, vararg formatArgs: Any?): String = ResourceTable.string(resId, *formatArgs)
}
