package io.github.gabrielgaraujo.highwaypatrol.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class TiltTest {
    private val g = 9.81f

    /** Gravity reading for a portrait phone tipped back [pitch] degrees and rolled [roll] degrees right. */
    private fun Tilt.hold(pitch: Float, roll: Float = 0f) {
        val p = Math.toRadians(pitch.toDouble())
        val r = Math.toRadians(roll.toDouble())
        val x = (-sin(r) * g).toFloat()
        val y = (cos(r) * cos(p) * g).toFloat()
        val z = (cos(r) * sin(p) * g).toFloat()
        onGravity(x, y, z)
    }

    @Test
    fun anglesMatchPhonePose() {
        assertEquals(0f, Tilt.pitchDegrees(0f, g, 0f), 0.01f)
        assertEquals(90f, Tilt.pitchDegrees(0f, 0f, g), 0.01f)
        assertEquals(30f, Tilt.rollDegrees(-g / 2f, g * 0.866f, 0f), 0.1f)
    }

    @Test
    fun holdingStillDoesNothing() {
        val tilt = Tilt()
        tilt.hold(40f)
        tilt.hold(42f) // small wobble within the dead zone
        assertEquals(0f, tilt.forward, 0f)
        assertEquals(0f, tilt.sideways, 0f)
    }

    @Test
    fun tippingAwayIsGasTowardsIsBrake() {
        val tilt = Tilt()
        val game = Game(1000f, Size(98f, 214f), listOf(Size(98f, 214f)))
        tilt.hold(40f) // calibrates neutral at 40 degrees

        tilt.hold(40f + Tilt.PITCH_FULL + 5f)
        tilt.applyTo(game)
        assertEquals(1f, game.throttle, 0.001f)
        assertEquals(0f, game.brake, 0f)

        tilt.hold(40f - 12f)
        tilt.applyTo(game)
        assertEquals(0f, game.throttle, 0f)
        assertTrue(game.brake > 0.3f && game.brake < 0.7f)
    }

    @Test
    fun rollingSteers() {
        val tilt = Tilt()
        tilt.hold(40f, roll = 30f)
        assertEquals(1f, tilt.sideways, 0.001f)
        tilt.hold(40f, roll = -10f)
        assertTrue(tilt.sideways < -0.2f && tilt.sideways > -0.5f)
    }

    @Test
    fun recalibrateUsesNewGrip() {
        val tilt = Tilt()
        tilt.hold(40f)
        tilt.calibrate()
        tilt.hold(70f)
        assertEquals(0f, tilt.forward, 0f)
    }

    @Test
    fun sensitivityNeedsLessTilt() {
        val tilt = Tilt(sensitivity = 2f)
        tilt.hold(40f)
        tilt.hold(40f + Tilt.PITCH_FULL / 2f)
        assertEquals(1f, tilt.forward, 0.001f)
    }
}
