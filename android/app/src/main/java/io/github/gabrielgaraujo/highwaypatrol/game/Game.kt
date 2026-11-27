package io.github.gabrielgaraujo.highwaypatrol.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Width and height of a sprite, in world units. */
data class Size(val w: Float, val h: Float)

/** Axis-aligned box in world units (y grows downwards, like the screen). */
class Box(var x: Float, var y: Float, val w: Float, val h: Float) {
    val right get() = x + w
    val bottom get() = y + h
    val centerX get() = x + w / 2f

    fun contains(px: Float, py: Float, slack: Float = 0f) =
        px >= x - slack && px <= right + slack && py >= y - slack && py <= bottom + slack

    /** True if the boxes overlap once each is shrunk by [inset] on every side. */
    fun intersects(o: Box, inset: Float = 0f) =
        x + inset < o.right - inset && o.x + inset < right - inset &&
            y + inset < o.bottom - inset && o.y + inset < bottom - inset
}

class TrafficCar(val sprite: Int, val box: Box, var speed: Float)

/** READY: waiting for the first tap. PLAYING: driving. CRASHED: run over. */
enum class State { READY, PLAYING, CRASHED }

/**
 * The whole game simulation, independent of Android so it can be unit tested.
 *
 * The world is [WORLD_WIDTH] units wide (the original pygame window width) and
 * [worldHeight] units tall, which depends on the phone's aspect ratio.
 * Speeds are in km/h; [update] takes elapsed seconds.
 *
 * The car drives itself: a run starts at [START_SPEED] and speeds up steadily
 * until it reaches [MAX_SPEED]. The player only steers.
 */
class Game(
    val worldHeight: Float,
    private val playerSize: Size,
    private val trafficSizes: List<Size>,
    /** Space kept free at the bottom of the screen (system navigation area). */
    private val bottomReserve: Float = 0f,
    private val random: Random = Random.Default,
) {
    companion object {
        const val WORLD_WIDTH = 500f
        const val START_SPEED = 60f
        const val MAX_SPEED = 140f
        /** km/h gained per second; reaches [MAX_SPEED] about 90 seconds into a run. */
        const val ACCELERATION = 0.9f
        /** Screen scroll, in world units per second, for each km/h of speed. */
        const val UNITS_PER_KMH = 9f
        const val SIREN_FRAME_SECONDS = 5f / 60f
        const val SIREN_FRAMES = 3
        const val COLLISION_INSET = 6f
        const val RESTART_DELAY_SECONDS = 0.8f
        const val TRAFFIC_MIN_SPEED = 30f
        const val TRAFFIC_MAX_SPEED = 100f
    }

    var state = State.READY
        private set
    val player = Box(0f, 0f, playerSize.w, playerSize.h)
    val traffic = mutableListOf<TrafficCar>()

    var speed = 0f
        private set
    /** Distance driven in meters. */
    var odometer = 0.0
        private set
    /** Total road scrolled, in world units; the renderer wraps it to the texture height. */
    var roadScroll = 0f
        private set

    var sirenOn = true
        private set
    var sirenFrame = 0
        private set
    private var sirenTimer = 0f

    private var distanceToNextSpawn = 0f
    private var crashedFor = 0f

    /** Called once when the player hits another car. */
    var onCrash: (() -> Unit)? = null

    init {
        reset()
    }

    /** Back to the starting line, waiting for [start]. */
    fun reset() {
        state = State.READY
        player.x = (WORLD_WIDTH - player.w) / 2f
        player.y = worldHeight - bottomReserve - player.h - 60f
        traffic.clear()
        speed = 0f
        odometer = 0.0
        roadScroll = 0f
        sirenFrame = 0
        sirenTimer = 0f
        distanceToNextSpawn = 200f
        crashedFor = 0f
    }

    /** Start driving. Does nothing unless the game is [State.READY]. */
    fun start() {
        if (state != State.READY) return
        state = State.PLAYING
        speed = START_SPEED
    }

    val canRestart get() = state == State.CRASHED && crashedFor >= RESTART_DELAY_SECONDS

    fun toggleSiren() {
        sirenOn = !sirenOn
    }

    /** Move the car sideways by a finger drag of [dx] world units. */
    fun steerBy(dx: Float) {
        if (state != State.PLAYING) return
        player.x = (player.x + dx).coerceIn(0f, WORLD_WIDTH - player.w)
    }

    fun update(dt: Float) {
        if (state == State.CRASHED) {
            crashedFor += dt
            return
        }

        if (sirenOn) {
            sirenTimer += dt
            while (sirenTimer >= SIREN_FRAME_SECONDS) {
                sirenTimer -= SIREN_FRAME_SECONDS
                sirenFrame = (sirenFrame + 1) % SIREN_FRAMES
            }
        }

        if (state == State.READY) return

        speed = min(speed + ACCELERATION * dt, MAX_SPEED)

        val scrolled = speed * UNITS_PER_KMH * dt
        // Wrapped at a multiple of the road tile (WORLD_WIDTH) to keep float precision.
        roadScroll = (roadScroll + scrolled) % (WORLD_WIDTH * 100f)
        odometer += speed / 3.6 * dt

        moveTraffic(dt)
        spawnTraffic(scrolled)

        if (traffic.any { player.intersects(it.box, COLLISION_INSET) }) {
            state = State.CRASHED
            speed = 0f
            onCrash?.invoke()
        }
    }

    private fun moveTraffic(dt: Float) {
        // A car that catches up with another in its lane slows down to match it.
        for (car in traffic) {
            for (other in traffic) {
                if (other === car) continue
                val sameLane = car.box.x < other.box.right && other.box.x < car.box.right
                val justAhead = other.box.y < car.box.y && car.box.y - other.box.bottom < 60f
                if (sameLane && justAhead) car.speed = min(car.speed, other.speed)
            }
        }
        for (car in traffic) {
            car.box.y += (speed - car.speed) * UNITS_PER_KMH * dt
        }
        // Drop cars that fell behind the player or pulled far ahead.
        traffic.removeAll { it.box.y > worldHeight + 50f || it.box.bottom < -worldHeight }
    }

    private fun spawnTraffic(scrolled: Float) {
        if (speed <= 0f) return
        distanceToNextSpawn -= scrolled
        if (distanceToNextSpawn > 0f) return

        val index = random.nextInt(trafficSizes.size)
        val size = trafficSizes[index]
        // Keep traffic between the yellow edge lines of the road texture.
        val x = 40f + random.nextFloat() * (WORLD_WIDTH - 80f - size.w)
        val box = Box(x, -size.h - 20f, size.w, size.h)
        if (traffic.any { it.box.intersects(Box(box.x, box.y - 80f, box.w, box.h + 160f)) }) {
            distanceToNextSpawn = 50f // spot is taken; try again shortly
            return
        }
        val carSpeed = TRAFFIC_MIN_SPEED + random.nextFloat() * (TRAFFIC_MAX_SPEED - TRAFFIC_MIN_SPEED)
        traffic.add(TrafficCar(index, box, carSpeed))

        // Gaps shrink as the player drives further, down to 55% after 20 km.
        val difficulty = max(0.55f, 1f - (odometer / 20_000.0).toFloat())
        distanceToNextSpawn = (300f + random.nextFloat() * 350f) * difficulty
    }
}
