package io.github.gabrielgaraujo.highwaypatrol

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.os.Build
import android.view.WindowInsets
import io.github.gabrielgaraujo.highwaypatrol.game.Game
import io.github.gabrielgaraujo.highwaypatrol.game.State
import kotlin.math.ceil

/**
 * Draws the game and turns touches into controls.
 *
 * Everything is drawn in world units (500 wide, like the original window) and
 * scaled to the screen width, so the game looks the same on every phone.
 *
 * Controls: hold GAS / BRAKE at the bottom, drag anywhere else to steer,
 * tap the police car to toggle the siren, tap after a crash to drive again.
 */
@SuppressLint("ViewConstructor")
class GameView(context: Context, private val sprites: Sprites, private val crashSound: CrashSound) :
    View(context), Choreographer.FrameCallback {

    private var game: Game? = null
    private var scale = 1f
    private var topInset = 0f // world units
    private var bottomInset = 0f

    private val prefs = context.getSharedPreferences("highway_patrol", Context.MODE_PRIVATE)
    private var bestMeters = prefs.getLong(KEY_BEST, 0L)

    private var running = false
    private var lastFrameNanos = 0L

    private var steeringPointer = MotionEvent.INVALID_POINTER_ID
    private var steeringLastX = 0f

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 40f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }
    private val pedalPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pedalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val overlayPaint = Paint().apply { color = Color.argb(170, 0, 0, 0) }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 80, 80)
        textSize = 64f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 34f
        textAlign = Paint.Align.CENTER
    }

    private val brakeRect = RectF()
    private val gasRect = RectF()
    private val dst = RectF()

    init {
        setOnApplyWindowInsetsListener { _, insets ->
            readInsets(insets)
            insets
        }
    }

    /** Keep the HUD and pedals clear of the camera cutout and (hidden) system bars, in world units. */
    private fun readInsets(insets: WindowInsets?) {
        if (insets == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bars = insets.getInsetsIgnoringVisibility(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            topInset = bars.top / scale
            bottomInset = bars.bottom / scale
        } else {
            @Suppress("DEPRECATION")
            topInset = insets.stableInsetTop / scale
            @Suppress("DEPRECATION")
            bottomInset = insets.stableInsetBottom / scale
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (w == 0 || h == 0) return
        scale = w / Game.WORLD_WIDTH
        readInsets(rootWindowInsets)
        game = Game(
            worldHeight = h / scale,
            playerSize = sprites.playerSize,
            trafficSizes = sprites.trafficSizes,
            bottomReserve = PEDAL_HEIGHT + PEDAL_MARGIN * 2 + bottomInset,
        ).apply { onCrash = ::onCrash }
    }

    private fun onCrash() {
        crashSound.play()
        val meters = game?.odometer?.toLong() ?: 0L
        if (meters > bestMeters) {
            bestMeters = meters
            prefs.edit().putLong(KEY_BEST, meters).apply()
        }
    }

    fun resume() {
        if (running) return
        running = true
        lastFrameNanos = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun pause() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
        game?.apply { throttle = false; brake = false }
        steeringPointer = MotionEvent.INVALID_POINTER_ID
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        if (lastFrameNanos != 0L) {
            // Cap the step so a hiccup doesn't teleport traffic through the player.
            val dt = ((frameTimeNanos - lastFrameNanos) / 1e9f).coerceAtMost(1f / 20f)
            game?.update(dt)
        }
        lastFrameNanos = frameTimeNanos
        invalidate()
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDraw(canvas: Canvas) {
        val game = game ?: return
        canvas.save()
        canvas.scale(scale, scale)

        drawRoad(canvas, game)
        for (car in game.traffic) {
            drawBitmap(canvas, sprites.traffic[car.sprite], car.box.x, car.box.y)
        }
        val police = if (game.sirenOn) sprites.policeSiren[game.sirenFrame] else sprites.policeIdle
        drawBitmap(canvas, police, game.player.x, game.player.y)

        drawHud(canvas, game)
        drawPedals(canvas, game)
        if (game.state == State.CRASHED) drawCrashed(canvas, game)

        canvas.restore()
    }

    private fun drawBitmap(canvas: Canvas, bitmap: android.graphics.Bitmap, x: Float, y: Float) {
        dst.set(x, y, x + bitmap.width, y + bitmap.height)
        canvas.drawBitmap(bitmap, null, dst, bitmapPaint)
    }

    private fun drawRoad(canvas: Canvas, game: Game) {
        // The texture is square, stretched to the world width, and tiled vertically.
        val tile = Game.WORLD_WIDTH
        var y = game.roadScroll % tile - tile
        while (y < game.worldHeight) {
            dst.set(0f, y, Game.WORLD_WIDTH, y + tile + 1f) // +1 hides seams
            canvas.drawBitmap(sprites.road, null, dst, bitmapPaint)
            y += tile
        }
    }

    private fun drawHud(canvas: Canvas, game: Game) {
        val baseline = topInset + 60f
        hudPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("${ceil(game.speed).toInt()} km/h", 20f, baseline, hudPaint)
        hudPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("${ceil(game.odometer).toLong()} m", Game.WORLD_WIDTH - 20f, baseline, hudPaint)
    }

    private fun layoutPedals(game: Game) {
        val bottom = game.worldHeight - bottomInset - PEDAL_MARGIN
        val top = bottom - PEDAL_HEIGHT
        brakeRect.set(PEDAL_MARGIN, top, PEDAL_MARGIN + PEDAL_WIDTH, bottom)
        gasRect.set(Game.WORLD_WIDTH - PEDAL_MARGIN - PEDAL_WIDTH, top, Game.WORLD_WIDTH - PEDAL_MARGIN, bottom)
    }

    private fun drawPedals(canvas: Canvas, game: Game) {
        layoutPedals(game)
        drawPedal(canvas, brakeRect, "BRAKE", Color.rgb(200, 40, 40), game.brake)
        drawPedal(canvas, gasRect, "GAS", Color.rgb(40, 170, 70), game.throttle)
    }

    private fun drawPedal(canvas: Canvas, rect: RectF, label: String, color: Int, pressed: Boolean) {
        pedalPaint.color = color
        pedalPaint.alpha = if (pressed) 230 else 120
        canvas.drawRoundRect(rect, 24f, 24f, pedalPaint)
        val textY = rect.centerY() - (pedalLabelPaint.ascent() + pedalLabelPaint.descent()) / 2f
        canvas.drawText(label, rect.centerX(), textY, pedalLabelPaint)
    }

    private fun drawCrashed(canvas: Canvas, game: Game) {
        canvas.drawRect(0f, 0f, Game.WORLD_WIDTH, game.worldHeight, overlayPaint)
        val cx = Game.WORLD_WIDTH / 2f
        val cy = game.worldHeight / 2f
        canvas.drawText("CRASHED!", cx, cy - 80f, titlePaint)
        canvas.drawText("Distance: ${game.odometer.toLong()} m", cx, cy, bodyPaint)
        canvas.drawText("Best: $bestMeters m", cx, cy + 50f, bodyPaint)
        if (game.canRestart) canvas.drawText("Tap to drive again", cx, cy + 140f, bodyPaint)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val game = game ?: return false
        layoutPedals(game)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (game.state == State.CRASHED) {
                    if (game.canRestart) {
                        game.reset()
                        steeringPointer = MotionEvent.INVALID_POINTER_ID
                    }
                    return true
                }
                val i = event.actionIndex
                val x = event.getX(i) / scale
                val y = event.getY(i) / scale
                if (!brakeRect.contains(x, y) && !gasRect.contains(x, y)) {
                    if (game.player.contains(x, y, slack = 20f)) game.toggleSiren()
                    steeringPointer = event.getPointerId(i)
                    steeringLastX = x
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val i = event.findPointerIndex(steeringPointer)
                if (i >= 0) {
                    val x = event.getX(i) / scale
                    game.steerBy((x - steeringLastX) * STEER_GAIN)
                    steeringLastX = x
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                if (event.getPointerId(event.actionIndex) == steeringPointer) {
                    steeringPointer = MotionEvent.INVALID_POINTER_ID
                }
            }
            MotionEvent.ACTION_CANCEL -> steeringPointer = MotionEvent.INVALID_POINTER_ID
        }

        // Pedals are pressed while any finger (other than one being lifted) is on them.
        var gas = false
        var brake = false
        if (event.actionMasked != MotionEvent.ACTION_CANCEL && game.state == State.PLAYING) {
            val lifting = event.actionMasked == MotionEvent.ACTION_UP ||
                event.actionMasked == MotionEvent.ACTION_POINTER_UP
            for (i in 0 until event.pointerCount) {
                if (lifting && i == event.actionIndex) continue
                val x = event.getX(i) / scale
                val y = event.getY(i) / scale
                if (gasRect.contains(x, y)) gas = true
                if (brakeRect.contains(x, y)) brake = true
            }
        }
        game.throttle = gas
        game.brake = brake
        return true
    }

    companion object {
        private const val KEY_BEST = "best_meters"
        private const val PEDAL_WIDTH = 190f
        private const val PEDAL_HEIGHT = 120f
        private const val PEDAL_MARGIN = 20f
        private const val STEER_GAIN = 1.5f
    }
}
