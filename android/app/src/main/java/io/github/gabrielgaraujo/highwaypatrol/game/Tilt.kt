package io.github.gabrielgaraujo.highwaypatrol.game

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * Turns the phone's gravity vector (from the accelerometer, in Android's sensor
 * axes for a portrait phone: x to the right, y to the top, z out of the screen)
 * into driving controls.
 *
 * - Tilt the top of the phone away from you: gas. Towards you: brake.
 * - Tilt left / right: steer.
 *
 * Forward/back is measured relative to the angle the phone was held at when
 * [calibrate] was called (start of every run), so any comfortable grip works.
 */
class Tilt(
    /** 1 is the default; 2 needs half the tilt for the same effect. */
    var sensitivity: Float = 1f,
) {
    companion object {
        const val PITCH_DEAD_ZONE = 4f
        const val PITCH_FULL = 20f
        const val ROLL_DEAD_ZONE = 3f
        const val ROLL_FULL = 25f

        fun pitchDegrees(x: Float, y: Float, z: Float): Float {
            // 0 when upright, 90 when flat on its back; grows as the top tips away.
            return Math.toDegrees(atan2(z, y).toDouble()).toFloat()
        }

        fun rollDegrees(x: Float, y: Float, z: Float): Float {
            // Positive when the right edge dips. The sensor reports the "up" direction,
            // so x goes negative as the right side goes down.
            return Math.toDegrees(atan2(-x, sqrt(y * y + z * z)).toDouble()).toFloat()
        }

        /** Maps an angle to -1..1 with a dead zone around 0. */
        fun response(degrees: Float, deadZone: Float, full: Float): Float {
            val magnitude = abs(degrees) - deadZone
            if (magnitude <= 0f) return 0f
            return sign(degrees) * (magnitude / (full - deadZone)).coerceAtMost(1f)
        }
    }

    private var neutralPitch: Float? = null
    private var pitch = 0f
    private var roll = 0f

    /** Feed one gravity reading. The first one after [calibrate] sets the neutral angle. */
    fun onGravity(x: Float, y: Float, z: Float) {
        pitch = pitchDegrees(x, y, z)
        roll = rollDegrees(x, y, z)
        if (neutralPitch == null) neutralPitch = pitch
    }

    /** Use the next reading as the "hands steady" angle. */
    fun calibrate() {
        neutralPitch = null
    }

    val calibrated get() = neutralPitch != null

    /** -1 (full brake) .. 1 (full gas). */
    val forward: Float
        get() {
            val neutral = neutralPitch ?: return 0f
            return response(pitch - neutral, PITCH_DEAD_ZONE, deadZoned(PITCH_FULL, PITCH_DEAD_ZONE))
        }

    /** -1 (full left) .. 1 (full right). */
    val sideways: Float
        get() = response(roll, ROLL_DEAD_ZONE, deadZoned(ROLL_FULL, ROLL_DEAD_ZONE))

    // Higher sensitivity reaches full effect with less tilt, but never inside the dead zone.
    private fun deadZoned(full: Float, deadZone: Float) =
        (full / sensitivity).coerceAtLeast(deadZone + 1f)

    /** Apply the tilt to the game's controls. */
    fun applyTo(game: Game) {
        val f = forward
        game.throttle = if (f > 0f) f else 0f
        game.brake = if (f < 0f) -f else 0f
        game.steer = sideways
    }
}
