package spike

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.View
import androidx.compose.ui.unit.dp
import com.nendo.argosy.ui.screens.library.LibraryGameList
import com.nendo.argosy.ui.screens.library.LibraryViewModel
import com.nendo.argosy.ui.theme.ALauncherTheme
import java.io.File

private val desktopContext = Context()

/** What the Android framework provides around every composition, answered for a desktop window. */
@Composable
fun DesktopAndroidLocals(content: @Composable () -> Unit) {
    BoxWithConstraints {
        CompositionLocalProvider(
            LocalContext provides desktopContext,
            LocalConfiguration provides Configuration(maxWidth.value.toInt(), maxHeight.value.toInt()),
            LocalView provides View(),
            content = content,
        )
    }
}

/** Upstream's ALauncherTheme around upstream's LibraryGameList, fed by the fake view model. */
@Composable
fun LibraryPreview() {
    val vm = FakeViewModels.get(LibraryViewModel::class)
    val state by vm.uiState.collectAsState()
    DesktopAndroidLocals {
        ALauncherTheme {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                LibraryGameList(
                    uiState = state,
                    viewModel = vm,
                    sidebarWidth = 0.dp,
                    headerHeightPx = 0,
                    footerHeightPx = 0,
                    onGameSelect = { println("open game $it") },
                )
            }
        }
    }
}

fun seedLibrary() {
    val covers = File(System.getProperty("java.io.tmpdir"), "argosy-spike-covers")
    FakeViewModels.get(LibraryViewModel::class).seed(FakeLibrary.state(covers))
}
