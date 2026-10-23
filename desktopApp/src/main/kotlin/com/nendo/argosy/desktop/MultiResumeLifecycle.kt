package com.nendo.argosy.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nendo.argosy.platform.HostGameProcess

/**
 * Compose Desktop resumes a window only while it holds keyboard focus; a visible window without
 * focus is STARTED. Android 10+ keeps every visible foreground activity RESUMED (multi-resume), and
 * upstream code depends on that: modals register their input handlers on ON_RESUME, and gamepad
 * input reaches the window without focus. This maps the window's STARTED to RESUMED. While a game
 * runs on the host the window is held at CREATED, as Android stops the launcher behind an emulator.
 */
@Composable
fun MultiResumeLifecycle(content: @Composable () -> Unit) {
    val parent = LocalLifecycleOwner.current
    val owner = remember(parent) { MultiResumeLifecycleOwner(parent.lifecycle) }
    DisposableEffect(owner) { onDispose { owner.dispose() } }
    LaunchedEffect(owner) { HostGameProcess.running.collect { owner.setGameRunning(it != null) } }
    CompositionLocalProvider(LocalLifecycleOwner provides owner, content = content)
}

private class MultiResumeLifecycleOwner(private val parent: Lifecycle) : LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    private val observer = LifecycleEventObserver { _, _ -> sync() }
    private var gameRunning = false

    init {
        parent.addObserver(observer)
    }

    private fun sync() {
        val state = parent.currentState
        if (state == Lifecycle.State.DESTROYED && registry.currentState == Lifecycle.State.INITIALIZED) return
        val mapped = if (state == Lifecycle.State.STARTED) Lifecycle.State.RESUMED else state
        registry.currentState = if (gameRunning && mapped.isAtLeast(Lifecycle.State.CREATED)) Lifecycle.State.CREATED else mapped
    }

    fun setGameRunning(running: Boolean) {
        gameRunning = running
        sync()
    }

    fun dispose() {
        parent.removeObserver(observer)
        if (registry.currentState != Lifecycle.State.INITIALIZED) registry.currentState = Lifecycle.State.DESTROYED
    }
}
