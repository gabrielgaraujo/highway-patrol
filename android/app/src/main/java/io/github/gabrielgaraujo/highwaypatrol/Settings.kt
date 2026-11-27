package io.github.gabrielgaraujo.highwaypatrol

import android.content.Context

enum class Controls { TILT, TOUCH }

/** Player preferences, stored in SharedPreferences. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("highway_patrol", Context.MODE_PRIVATE)

    var controls: Controls
        get() = runCatching { Controls.valueOf(prefs.getString(KEY_CONTROLS, null)!!) }
            .getOrDefault(Controls.TILT)
        set(value) = prefs.edit().putString(KEY_CONTROLS, value.name).apply()

    /** Tilt sensitivity step, [MIN_SENSITIVITY]..[MAX_SENSITIVITY]; 5 is the default. */
    var tiltSensitivity: Int
        get() = prefs.getInt(KEY_SENSITIVITY, DEFAULT_SENSITIVITY).coerceIn(MIN_SENSITIVITY, MAX_SENSITIVITY)
        set(value) = prefs.edit().putInt(KEY_SENSITIVITY, value).apply()

    /** The sensitivity step as a multiplier for [io.github.gabrielgaraujo.highwaypatrol.game.Tilt]. */
    val tiltMultiplier get() = 0.5f + tiltSensitivity * 0.1f

    var bestMeters: Long
        get() = prefs.getLong(KEY_BEST, 0L)
        set(value) = prefs.edit().putLong(KEY_BEST, value).apply()

    companion object {
        const val MIN_SENSITIVITY = 1
        const val MAX_SENSITIVITY = 10
        const val DEFAULT_SENSITIVITY = 5
        private const val KEY_CONTROLS = "controls"
        private const val KEY_SENSITIVITY = "tilt_sensitivity"
        private const val KEY_BEST = "best_meters"
    }
}
