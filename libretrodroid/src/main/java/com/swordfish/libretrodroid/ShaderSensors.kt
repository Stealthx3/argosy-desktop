package com.swordfish.libretrodroid

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.swordfish.libretrodroid.LibretroDroid.SHADER_SENSOR_ACCELEROMETER
import com.swordfish.libretrodroid.LibretroDroid.SHADER_SENSOR_GYROSCOPE

internal class ShaderSensors(context: Context) : SensorEventListener {
    private val manager = context.applicationContext.getSystemService(SensorManager::class.java)
    private val calibration = ShaderSensorCalibration()
    private var sensorUsage = 0

    @Volatile
    var state = ShaderSensorState()
        private set

    fun setRequiredSensors(usage: Int) {
        if (usage == sensorUsage) return
        manager?.unregisterListener(this)
        sensorUsage = usage
        state = ShaderSensorState()
        if (usage != 0) {
            calibration.resume()
            if (usage and SHADER_SENSOR_ACCELEROMETER != 0) {
                manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                    manager.registerListener(this, it, SAMPLE_PERIOD_US)
                }
            }
            if (usage and SHADER_SENSOR_GYROSCOPE != 0) {
                manager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
                    manager.registerListener(this, it, SAMPLE_PERIOD_US)
                }
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (sensorUsage == 0) return
        val values = event.values
        state = when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> calibration.accelerometerChanged(values[0], values[1], values[2])
            Sensor.TYPE_GYROSCOPE -> {
                if (sensorUsage and SHADER_SENSOR_GYROSCOPE == 0) return
                calibration.gyroscopeChanged(values[0], values[1], values[2])
            }
            else -> return
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val SAMPLE_PERIOD_US = 16_667
    }
}
