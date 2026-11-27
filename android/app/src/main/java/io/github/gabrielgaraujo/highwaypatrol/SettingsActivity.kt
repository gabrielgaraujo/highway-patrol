package io.github.gabrielgaraujo.highwaypatrol

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView

class SettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val settings = Settings(this)

        val sensors = getSystemService(SensorManager::class.java)
        val hasTilt = sensors?.getDefaultSensor(Sensor.TYPE_GRAVITY) != null ||
            sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

        val controls = findViewById<RadioGroup>(R.id.controls)
        val sensitivityLabel = findViewById<TextView>(R.id.sensitivity_label)
        val sensitivity = findViewById<SeekBar>(R.id.sensitivity)

        fun showTiltOptions(enabled: Boolean) {
            sensitivity.isEnabled = enabled
            sensitivityLabel.isEnabled = enabled
            sensitivity.alpha = if (enabled) 1f else 0.4f
        }

        if (!hasTilt) {
            findViewById<View>(R.id.controls_tilt).isEnabled = false
            findViewById<View>(R.id.no_tilt_sensor).visibility = View.VISIBLE
        }
        val tiltSelected = hasTilt && settings.controls == Controls.TILT
        controls.check(if (tiltSelected) R.id.controls_tilt else R.id.controls_touch)
        showTiltOptions(tiltSelected)
        controls.setOnCheckedChangeListener { _, id ->
            val choice = if (id == R.id.controls_tilt) Controls.TILT else Controls.TOUCH
            settings.controls = choice
            showTiltOptions(choice == Controls.TILT)
        }

        // SeekBar min is API 26+, so offset from 0 instead.
        sensitivity.max = Settings.MAX_SENSITIVITY - Settings.MIN_SENSITIVITY
        sensitivity.progress = settings.tiltSensitivity - Settings.MIN_SENSITIVITY
        sensitivityLabel.text = getString(R.string.tilt_sensitivity_value, settings.tiltSensitivity)
        sensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                val value = progress + Settings.MIN_SENSITIVITY
                settings.tiltSensitivity = value
                sensitivityLabel.text = getString(R.string.tilt_sensitivity_value, value)
            }

            override fun onStartTrackingTouch(bar: SeekBar) = Unit
            override fun onStopTrackingTouch(bar: SeekBar) = Unit
        })

        findViewById<Button>(R.id.done).setOnClickListener { finish() }
    }
}
