package com.nendo.argosy.desktop

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.unit.Density
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
import com.nendo.argosy.platform.image.installDesktopImageLoader
import com.nendo.argosy.ui.ArgosyApp
import com.nendo.argosy.ui.theme.ALauncherTheme
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

fun main(args: Array<String>) {
    val options = ShellOptions.parse(args)
    val app = DesktopApplication()
    installDesktopImageLoader(app.component.rommConnectionManager(), app.component.userCertStore())
    val viewModels = app.component.viewModelFactory()
    val keyDispatcher = app.component.keyDispatcher()
    val gamepadDispatcher = app.component.gamepadDispatcher()
    gamepadDispatcher.start()
    app.component.hostSessionWatcher().start()
    val fontFamilyResolver = robotoFontFamilyResolver()
    val keyPipe = options.keyPipe?.let { KeyPipe(java.io.File(it), gamepadDispatcher) }?.also { it.start() }
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
            onPreviewKeyEvent = keyDispatcher::dispatch,
        ) {
            DisposableEffect(window) {
                keyPipe?.attach(window)
                val focusListener = object : WindowAdapter() {
                    override fun windowLostFocus(e: WindowEvent) {
                        keyDispatcher.releaseAll()
                        gamepadDispatcher.releaseAll()
                    }
                }
                window.addWindowFocusListener(focusListener)
                onDispose { window.removeWindowFocusListener(focusListener) }
            }
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(
                    LocalDensity provides tvDensity(constraints.maxHeight, LocalDensity.current.fontScale),
                    LocalFontFamilyResolver provides fontFamilyResolver,
                    LocalPlatformContext provides app,
                    LocalViewModelProviders provides viewModels,
                ) {
                    MultiResumeLifecycle {
                        ALauncherTheme {
                            Surface(modifier = Modifier.fillMaxSize()) {
                                ArgosyApp()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Android TV's density rule: the screen is 540dp tall at any resolution (720p is 1.33, 1080p is 2.0,
 * 4K is 4.0), so a 1920x1080 window lays out exactly as a 320 dpi 1080p Android device does.
 */
fun tvDensity(heightPx: Int, fontScale: Float): Density =
    Density(density = (heightPx / TV_HEIGHT_DP).coerceAtLeast(1f), fontScale = fontScale)

private const val TV_HEIGHT_DP = 540f

data class ShellOptions(val windowed: Boolean, val width: Int, val height: Int, val keyPipe: String?) {
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
                keyPipe = args.firstOrNull { it.startsWith("--key-pipe=") }?.removePrefix("--key-pipe="),
            )
        }
    }
}
