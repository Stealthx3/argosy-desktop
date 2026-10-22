package com.nendo.argosy.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.nendo.argosy.di.DesktopApplication
import com.nendo.argosy.di.LocalViewModelProviders
import com.nendo.argosy.di.viewModelFactory
import com.nendo.argosy.platform.LocalPlatformContext
import com.nendo.argosy.ui.ArgosyApp
import com.nendo.argosy.ui.theme.ALauncherTheme

fun main(args: Array<String>) {
    val options = ShellOptions.parse(args)
    val app = DesktopApplication()
    val viewModels = app.component.viewModelFactory()
    application {
        val state = rememberWindowState(
            placement = if (options.windowed) WindowPlacement.Floating else WindowPlacement.Fullscreen,
            size = DpSize(options.width.dp, options.height.dp),
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = "Argosy",
            state = state,
            undecorated = !options.windowed,
        ) {
            CompositionLocalProvider(
                LocalPlatformContext provides app,
                LocalViewModelProviders provides viewModels,
            ) {
                ALauncherTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        ArgosyApp()
                    }
                }
            }
        }
    }
}

data class ShellOptions(val windowed: Boolean, val width: Int, val height: Int) {
    companion object {
        fun parse(args: Array<String>): ShellOptions {
            val size = args.firstOrNull { it.startsWith("--size=") }
                ?.removePrefix("--size=")
                ?.split('x')
                ?.mapNotNull { it.toIntOrNull() }
                ?.takeIf { it.size == 2 }
            return ShellOptions(
                windowed = "--windowed" in args,
                width = size?.get(0) ?: 1920,
                height = size?.get(1) ?: 1080,
            )
        }
    }
}
