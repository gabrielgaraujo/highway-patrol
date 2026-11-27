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

    /** Advance the game while clearing traffic, so a crash can't end the run. */
    private fun Game.driveSafely(seconds: Float, step: Float = 1f / 60f) {
        var t = 0f
        while (t < seconds) {
            traffic.clear()
            update(step)
            t += step
        }
    }

    @Test
    fun waitsForStartBeforeMoving() {
        val game = newGame()
        game.update(5f)
        assertEquals(State.READY, game.state)
        assertEquals(0f, game.speed, 0f)
        assertEquals(0.0, game.odometer, 0.0)
        assertTrue(game.traffic.isEmpty())

        game.start()
        assertEquals(State.PLAYING, game.state)
        assertEquals(Game.START_SPEED, game.speed, 0f)
    }

    @Test
    fun speedsUpOnItsOwnAndCapsAtMax() {
        val game = newGame()
        game.start()
        game.driveSafely(10f)
        assertEquals(Game.START_SPEED + Game.ACCELERATION * 10f, game.speed, 0.1f)

        game.driveSafely(120f)
        assertEquals(Game.MAX_SPEED, game.speed, 0f)
        assertEquals(140f, Game.MAX_SPEED, 0f)
    }

    @Test
    fun odometerCountsMeters() {
        val game = newGame()
        game.start()
        val before = game.odometer
        game.driveSafely(1f, step = 1f) // about 60.5 km/h for 1 s
        assertEquals(game.speed / 3.6, game.odometer - before, 0.01)
    }

    @Test
    fun steeringOnlyWhileDrivingAndStaysOnScreen() {
        val game = newGame()
        val start = game.player.x
        game.steerBy(100f)
        assertEquals(start, game.player.x, 0f)

        game.start()
        game.steerBy(-10_000f)
        assertEquals(0f, game.player.x, 0f)
        game.steerBy(10_000f)
        assertEquals(Game.WORLD_WIDTH - car.w, game.player.x, 0f)
    }

    @Test
    fun hittingTrafficCrashesAndRestartWaitsForStart() {
        val game = newGame()
        var crashes = 0
        game.onCrash = { crashes++ }
        game.start()
        game.traffic.add(TrafficCar(0, Box(game.player.x, game.player.y - 230f, car.w, car.h), 0f))
        repeat(60) { if (game.state == State.PLAYING) game.update(1f / 60f) }

        assertEquals(State.CRASHED, game.state)
        assertEquals(1, crashes)
        assertFalse(game.canRestart)
        game.update(Game.RESTART_DELAY_SECONDS)
        assertTrue(game.canRestart)

        game.reset()
        assertEquals(State.READY, game.state)
        assertEquals(0f, game.speed, 0f)
        assertTrue(game.traffic.isEmpty())
    }

    @Test
    fun trafficSpawnsWhileDrivingAndIsCleanedUp() {
        val game = newGame()
        game.start()
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
    fun sirenAnimatesOnlyWhenOn() {
        val game = newGame()
        game.update(Game.SIREN_FRAME_SECONDS * 1.5f)
        assertEquals(1, game.sirenFrame)
        game.toggleSiren()
        game.update(1f)
        assertEquals(1, game.sirenFrame)
    }
}
