package spike

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() {
    seedLibrary()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Argosy Desktop — Library spike",
            state = rememberWindowState(width = 1280.dp, height = 800.dp),
        ) { LibraryPreview() }
    }
}
