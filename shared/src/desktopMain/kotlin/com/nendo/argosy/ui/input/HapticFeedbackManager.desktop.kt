package com.nendo.argosy.ui.input

import com.nendo.argosy.data.preferences.ControlsPreferences
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays the shared haptic waveforms as rumble on the gamepad that last sent input, the desktop
 * stand-in for a handheld's body vibration. Each segment is one SDL rumble of the segment's
 * length on both motors; a pause stops the motors.
 */
@Singleton
actual class HapticFeedbackManager @Inject constructor(
    private val rumble: GamepadRumble
) {
    private val player = Executors.newSingleThreadScheduledExecutor { task ->
        Thread(task, "haptics").apply { isDaemon = true }
    }

    private var enabled = true

    @Volatile
    var strength: Float = ControlsPreferences.DEFAULT_HAPTIC_STRENGTH
        private set

    @Volatile
    private var activePattern: HapticPattern? = null

    @Volatile
    private var activeUntilMs = 0L

    private var scheduled: List<ScheduledFuture<*>> = emptyList()

    actual fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    actual fun setStrength(strength: Float) {
        this.strength = strength.coerceIn(0f, 1f)
    }

    actual fun vibrate(pattern: HapticPattern) {
        if (!enabled) return
        val waveform = HapticWaveforms.forPattern(pattern, strength) ?: return
        val now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime())
        synchronized(this) {
            if (!HapticWaveforms.canInterrupt(activePattern, activeUntilMs, pattern, now)) return
            activePattern = pattern
            activeUntilMs = now + waveform.durationMs
            scheduled.forEach { it.cancel(false) }
            var offsetMs = 0L
            scheduled = waveform.timingsMs.zip(waveform.amplitudes) { durationMs, amplitude ->
                val motor = amplitude * MOTOR_PER_AMPLITUDE
                player.schedule(
                    { rumble.rumble(motor, motor, durationMs.toInt()) },
                    offsetMs,
                    TimeUnit.MILLISECONDS
                ).also { offsetMs += durationMs }
            }
        }
    }

    private companion object {
        const val MOTOR_PER_AMPLITUDE = 257
    }
}
