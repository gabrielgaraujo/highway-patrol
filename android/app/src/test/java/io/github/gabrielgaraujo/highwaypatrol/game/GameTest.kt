package io.github.gabrielgaraujo.highwaypatrol.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameTest {
    private val car = Size(98f, 214f)

    private fun newGame(seed: Int = 1) =
        Game(worldHeight = 1000f, playerSize = car, trafficSizes = listOf(car), random = Random(seed))

    private fun Game.run(seconds: Float, step: Float = 1f / 60f) {
        var t = 0f
        while (t < seconds) {
            update(step); t += step
        }
    }

    @Test
    fun gasAcceleratesUpToMaxSpeed() {
        val game = newGame()
        game.throttle = 1f
        game.update(1f)
        assertEquals(Game.ACCELERATION, game.speed, 0.01f)
        repeat(30) {
            game.traffic.clear() // keep traffic from ending the run
            game.update(1f)
        }
        assertEquals(Game.MAX_SPEED, game.speed, 0f)
    }

    @Test
    fun brakingNeverGoesBelowZero() {
        val game = newGame()
        game.throttle = 1f
        game.update(1f)
        game.throttle = 0f
        game.brake = 1f
        game.update(5f)
        assertEquals(0f, game.speed, 0f)
    }

    @Test
    fun odometerCountsMeters() {
        val game = newGame()
        game.throttle = 1f
        game.update(1f) // 12 km/h
        game.throttle = 0f
        val before = game.odometer
        game.update(3.6f) // 12 km/h for 3.6 s = 12 m
        assertEquals(12.0, game.odometer - before, 0.01)
    }

    @Test
    fun cannotSteerWhenStoppedAndStaysOnScreen() {
        val game = newGame()
        val start = game.player.x
        game.steerBy(100f)
        assertEquals(start, game.player.x, 0f)

        game.throttle = 1f
        game.update(5f) // 60 km/h, full sensitivity
        game.traffic.clear()
        game.steerBy(-10_000f)
        assertEquals(0f, game.player.x, 0f)
        game.steerBy(10_000f)
        assertEquals(Game.WORLD_WIDTH - car.w, game.player.x, 0f)
    }

    @Test
    fun hittingTrafficCrashesAndRestartResets() {
        val game = newGame()
        var crashes = 0
        game.onCrash = { crashes++ }
        game.throttle = 1f
        game.update(1f)
        game.traffic.clear()
        game.traffic.add(TrafficCar(0, Box(game.player.x, game.player.y - 230f, car.w, car.h), 0f))
        game.run(1f)

        assertEquals(State.CRASHED, game.state)
        assertEquals(1, crashes)
        assertFalse(game.canRestart)
        game.update(Game.RESTART_DELAY_SECONDS)
        assertTrue(game.canRestart)

        game.reset()
        assertEquals(State.PLAYING, game.state)
        assertEquals(0f, game.speed, 0f)
        assertTrue(game.traffic.isEmpty())
    }

    @Test
    fun trafficSpawnsWhileDrivingAndIsCleanedUp() {
        val game = newGame()
        game.throttle = 1f
        game.update(10f) // jump to 120 km/h
        var maxCars = 0
        repeat(600) {
            // Keep the player out of the way so we only observe traffic.
            game.traffic.removeAll { it.box.intersects(game.player, -40f) }
            game.update(1f / 60f)
            maxCars = maxOf(maxCars, game.traffic.size)
        }
        assertEquals(State.PLAYING, game.state)
        assertTrue("traffic should spawn", maxCars > 0)
        assertTrue("old traffic should be removed", maxCars < 20)
    }

    @Test
    fun partialInputsScaleAccelerationAndSteering() {
        val game = newGame()
        game.throttle = 0.5f
        game.update(1f)
        assertEquals(Game.ACCELERATION / 2f, game.speed, 0.01f)

        game.throttle = 1f
        game.update(4f) // 54 km/h: full steering sensitivity
        game.traffic.clear()
        val x = game.player.x
        game.throttle = 0f
        game.steer = 0.25f
        game.update(0.1f)
        assertEquals(x + Game.TILT_STEER_SPEED * 0.25f * 0.1f, game.player.x, 0.01f)
    }

    @Test
    fun sirenAnimatesOnlyWhenOn() {
        val game = newGame()
        game.update(Game.SIREN_FRAME_SECONDS * 1.5f)
        assertEquals(1, game.sirenFrame)
        game.toggleSiren()
        game.update(1f)
        assertEquals(1, game.sirenFrame)
    }
}
