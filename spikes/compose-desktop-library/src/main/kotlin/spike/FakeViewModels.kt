package spike

import com.nendo.argosy.ui.screens.library.LibraryViewModel
import com.nendo.argosy.ui.theme.ThemeViewModel
import kotlin.reflect.KClass

/** What Hilt would inject on Android: one instance per fake view model type. */
object FakeViewModels {
    private val instances = mutableMapOf<KClass<*>, Any>()

    init {
        register(ThemeViewModel())
        register(LibraryViewModel())
    }

    fun register(vm: Any) { instances[vm::class] = vm }

    @Suppress("UNCHECKED_CAST")
    fun <VM : Any> get(type: KClass<VM>): VM = instances[type] as VM? ?: error("no fake for ${type.simpleName}")
}
