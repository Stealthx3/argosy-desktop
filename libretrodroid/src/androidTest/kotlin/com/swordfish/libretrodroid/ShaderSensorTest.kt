package com.swordfish.libretrodroid

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShaderSensorTest {
    @Test
    fun shaderChainsTrackSensorUsageAndRedrawAcrossReplacements() {
        System.loadLibrary("libretrodroid")
        for (hardware in listOf(false, true)) {
            assertNull("hardware=$hardware", verifyShaderChain(hardware))
        }
    }

    @Test
    fun calibrationHandlesMountingFlatStartupAndResume() {
        val orientations = listOf(
            SensorVector(0f, 8f, 6f), SensorVector(-8f, 0f, 6f),
            SensorVector(0f, -8f, 6f), SensorVector(8f, 0f, 6f)
        )
        val gyroscopes = listOf(
            SensorVector(1f, 2f, 3f), SensorVector(2f, -1f, 3f),
            SensorVector(-1f, -2f, 3f), SensorVector(-2f, 1f, 3f)
        )
        for ((rotation, gravity) in orientations.withIndex()) {
            val calibration = ShaderSensorCalibration()
            repeat(60) { calibration.accelerometerChanged(0f, 0f, 9.8f) }
            repeat(60) { calibration.accelerometerChanged(gravity.x, gravity.y, gravity.z) }
            val state = calibration.gyroscopeChanged(1f, 2f, 3f)
            assertEquals("mounting=$rotation gyro", gyroscopes[rotation], state.gyroscope)
            assertEquals(0f, state.accelerometer.x, 0f)
            assertEquals(8f, state.accelerometer.y, 0f)
            assertEquals(6f, state.accelerometer.z, 0f)
            assertEquals(SensorVector(0f, 8f, 6f), state.accelerometerRest)
            assertEquals(state.accelerometerRest, calibration.accelerometerChanged(1f, 2f, 3f).accelerometerRest)
            calibration.resume()
            assertEquals(SensorVector(), calibration.gyroscopeChanged(0f, 0f, 0f).accelerometerRest)
            repeat(30) { calibration.accelerometerChanged(gravity.x * 0.75f, gravity.y * 0.75f, 7f) }
            assertEquals(SensorVector(0f, 6f, 7f), calibration.gyroscopeChanged(0f, 0f, 0f).accelerometerRest)
        }
    }

    private external fun verifyShaderChain(hardware: Boolean): String?
}
