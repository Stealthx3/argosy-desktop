package com.nendo.argosy.desktop

import com.nendo.argosy.ui.util.LocalHoverHighlight
import com.nendo.argosy.ui.util.LocalPointerInWindow
import com.nendo.argosy.ui.util.LocalDisplayProfile
import java.awt.AWTEvent
import javax.swing.SwingUtilities
import java.awt.event.AWTEventListener
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import com.nendo.argosy.ui.util.rememberHoverHighlight
import kotlin.system.exitProcess
import com.nendo.argosy.platform.AppShutdown
import com.nendo.argosy.platform.FactoryResetWipe
import com.nendo.argosy.ui.input.DesktopGamepadDispatcher
import com.nendo.argosy.platform.mpv.MpvProbe
import kotlinx.coroutines.flow.drop
import java.awt.image.BufferedImage
import java.awt.event.MouseMotionAdapter
import java.awt.event.MouseEvent
import java.awt.Toolkit
import java.awt.Point
import java.awt.Cursor
import java.awt.Color as AwtColor
import com.nendo.argosy.platform.DisplayProfileDetection
import com.nendo.argosy.data.preferences.DisplayProfile
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.nendo.argosy.di.DesktopApplication
import com.nendo.argosy.di.LocalViewModelProviders
import com.nendo.argosy.di.viewModelFactory
import com.nendo.argosy.platform.DesktopWindowMode
import com.nendo.argosy.platform.LocalPlatformContext
import com.nendo.argosy.platform.image.installDesktopImageLoader
import com.nendo.argosy.ui.ArgosyApp
import com.nendo.argosy.ui.theme.ALauncherTheme
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    args.firstOrNull { it.startsWith(PROBE_VIDEO_FLAG) }?.let {
        println(MpvProbe.run(it.removePrefix(PROBE_VIDEO_FLAG)))
        return
    }
    FactoryResetWipe.runIfPending()
    val options = ShellOptions.parse(args)
    val app = DesktopApplication()
    com.nendo.argosy.data.emulator.LinuxEmulatorDatabase.current
    installDesktopImageLoader(app.component.rommConnectionManager(), app.component.userCertStore())
    val viewModels = app.component.viewModelFactory()
    val keyDispatcher = app.component.keyDispatcher()
    val gamepadDispatcher = app.component.gamepadDispatcher()
    gamepadDispatcher.start()
    Runtime.getRuntime().addShutdownHook(Thread({ shutDown(gamepadDispatcher) }, "argosy-shutdown"))
    app.component.hostSessionWatcher().start()
    app.component.backgroundJobs().start()
    val fontFamilyResolver = robotoFontFamilyResolver()
    val keyPipe = options.keyPipe?.let { KeyPipe(java.io.File(it), gamepadDispatcher) }?.also { it.start() }
    val gamescope = DesktopWindowMode.isGamescope(System.getenv())
    val startFullScreen = runBlocking { app.component.displayPreferences().preferences.first().startFullScreen }
    val fullScreen = DesktopWindowMode.startsFullScreen(startFullScreen, options.fullScreenOverride, gamescope)
    val detectedProfile = DisplayProfileDetection.detect()
    val blankCursor = Toolkit.getDefaultToolkit()
        .createCustomCursor(BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), Point(0, 0), "blank")
    application(exitProcessOnExit = false) {
        val shutdownRequested by AppShutdown.requested.collectAsState()
        LaunchedEffect(shutdownRequested) { if (shutdownRequested) exitApplication() }
        val state = rememberWindowState(
            placement = if (fullScreen) WindowPlacement.Fullscreen else WindowPlacement.Floating,
            position = WindowPosition(Alignment.Center),
            size = DpSize(options.width.dp, options.height.dp),
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = "Argosy",
            state = state,
            undecorated = gamescope,
            onPreviewKeyEvent = { event ->
                if (event.key == Key.F11) {
                    if (event.type == KeyEventType.KeyDown && !gamescope) state.toggleFullScreen()
                    true
                } else {
                    keyDispatcher.dispatch(event)
                }
            },
        ) {
            val display by app.component.displayPreferences().preferences.collectAsState(initial = null)
            val profile = DisplayProfile.effective(display?.displayProfile, detectedProfile, gamescope)
            val safeArea = DisplayProfile.effectiveSafeArea(display?.safeAreaPercent, profile)
            LaunchedEffect(window) {
                window.background = AwtColor.BLACK
                window.contentPane.background = AwtColor.BLACK
                window.rootPane.background = AwtColor.BLACK
            }
            LaunchedEffect(window, profile) {
                window.cursor = Cursor.getDefaultCursor()
                if (profile.hidesIdleCursor) gamepadDispatcher.padActivity.drop(1).collect { window.cursor = blankCursor }
            }
            DisposableEffect(window) {
                val mouseListener = object : MouseMotionAdapter() {
                    override fun mouseMoved(e: MouseEvent) {
                        if (window.cursor == blankCursor) window.cursor = Cursor.getDefaultCursor()
                    }
                }
                window.contentPane.addMouseMotionListener(mouseListener)
                onDispose { window.contentPane.removeMouseMotionListener(mouseListener) }
            }
            val pointerInWindow = remember { mutableStateOf(true) }
            DisposableEffect(window) {
                val pointerListener = AWTEventListener { event ->
                    if (event is MouseEvent && (event.component == window || SwingUtilities.getWindowAncestor(event.component) == window)) {
                        when (event.id) {
                            MouseEvent.MOUSE_EXITED -> pointerInWindow.value = false
                            MouseEvent.MOUSE_ENTERED, MouseEvent.MOUSE_MOVED -> pointerInWindow.value = true
                        }
                    }
                }
                Toolkit.getDefaultToolkit().addAWTEventListener(
                    pointerListener,
                    AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
                )
                onDispose { Toolkit.getDefaultToolkit().removeAWTEventListener(pointerListener) }
            }
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
            BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                val marginX = maxWidth * (safeArea / 100f)
                val marginY = maxHeight * (safeArea / 100f)
                BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(horizontal = marginX, vertical = marginY)) {
                    CompositionLocalProvider(
                        LocalDensity provides tvDensity(constraints.maxHeight, LocalDensity.current.fontScale),
                        LocalFontFamilyResolver provides fontFamilyResolver,
                        LocalPlatformContext provides app,
                        LocalViewModelProviders provides viewModels,
                    ) {
                        MultiResumeLifecycle {
                            ALauncherTheme {
                                CompositionLocalProvider(
                                    LocalPointerInWindow provides pointerInWindow,
                                    LocalDisplayProfile provides profile,
                                    LocalHoverHighlight provides
                                        if (profile == DisplayProfile.DESKTOP) rememberHoverHighlight() else null,
                                ) {
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
    }
    keyDispatcher.releaseAll()
    if (AppShutdown.factoryResetRequested.value) FactoryResetWipe.schedule()
    exitProcess(shellExitCode())
}

private fun shutDown(gamepadDispatcher: DesktopGamepadDispatcher) {
    Thread({
        Thread.sleep(SHUTDOWN_GRACE_MS)
        Runtime.getRuntime().halt(shellExitCode())
    }, "argosy-shutdown-watchdog").apply { isDaemon = true }.start()
    gamepadDispatcher.stop()
}

private fun shellExitCode(): Int =
    if (AppShutdown.factoryResetRequested.value) FactoryResetWipe.RESTART_EXIT_CODE else 0

private const val SHUTDOWN_GRACE_MS = 3_000L

/**
 * Android TV's density rule: the screen is 540dp tall at any resolution (720p is 1.33, 1080p is 2.0,
 * 4K is 4.0), so a 1920x1080 window lays out exactly as a 320 dpi 1080p Android device does.
 */
fun tvDensity(heightPx: Int, fontScale: Float): Density =
    Density(density = (heightPx / TV_HEIGHT_DP).coerceAtLeast(1f), fontScale = fontScale)

private const val TV_HEIGHT_DP = 540f

private const val PROBE_VIDEO_FLAG = "--probe-video="

private fun WindowState.toggleFullScreen() {
    placement = if (placement == WindowPlacement.Fullscreen) WindowPlacement.Floating else WindowPlacement.Fullscreen
}

data class ShellOptions(val fullScreenOverride: Boolean?, val width: Int, val height: Int, val keyPipe: String?) {
    companion object {
        fun parse(args: Array<String>): ShellOptions {
            val size = args.firstOrNull { it.startsWith("--size=") }
                ?.removePrefix("--size=")
                ?.split('x')
                ?.mapNotNull { it.toIntOrNull() }
                ?.takeIf { it.size == 2 }
            return ShellOptions(
                fullScreenOverride = DesktopWindowMode.overrideFrom(args),
                width = size?.get(0) ?: DEFAULT_WIDTH,
                height = size?.get(1) ?: DEFAULT_HEIGHT,
                keyPipe = args.firstOrNull { it.startsWith("--key-pipe=") }?.removePrefix("--key-pipe="),
            )
        }

        private const val DEFAULT_WIDTH = 1280
        private const val DEFAULT_HEIGHT = 720
    }
}
