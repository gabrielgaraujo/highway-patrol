package io.github.gabrielgaraujo.highwaypatrol

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.github.gabrielgaraujo.highwaypatrol.game.Tilt

/** Feeds gravity readings into a [Tilt]. Prefers the gravity sensor; falls back to a smoothed accelerometer. */
class TiltSensor(context: Context, val tilt: Tilt) : SensorEventListener {
    private val manager = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        ?: manager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val smooth = sensor?.type == Sensor.TYPE_ACCELEROMETER
    private val gravity = FloatArray(3)
    private var hasReading = false

    val available get() = sensor != null

    fun start() {
        val sensor = sensor ?: return
        hasReading = false
        manager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    }

    fun stop() {
        manager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val v = event.values
        if (!smooth || !hasReading) {
            gravity[0] = v[0]; gravity[1] = v[1]; gravity[2] = v[2]
        } else {
            // Low-pass filter strips out bumps and hand shake from the raw accelerometer.
            for (i in 0..2) gravity[i] += SMOOTHING * (v[i] - gravity[i])
        }
        hasReading = true
        tilt.onGravity(gravity[0], gravity[1], gravity[2])
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val SMOOTHING = 0.2f
    }
}
