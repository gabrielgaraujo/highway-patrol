package io.github.gabrielgaraujo.highwaypatrol

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.github.gabrielgaraujo.highwaypatrol.game.Size

/** Bitmaps from the shared top-level `assets/` folder (also used by the pygame version). */
class Sprites(assets: AssetManager) {
    private fun load(assets: AssetManager, path: String): Bitmap =
        assets.open(path).use { BitmapFactory.decodeStream(it) }
            ?: error("Could not decode asset $path")

    val road: Bitmap = load(assets, "sprites/asphalt.jpg")

    val policeIdle: Bitmap = load(assets, "sprites/cars/animated/police/idle.png")
    val policeSiren: List<Bitmap> = (1..3).map { load(assets, "sprites/cars/animated/police/$it.png") }

    /** Same cars the original picks obstacles from. */
    val traffic: List<Bitmap> = listOf("audi", "car", "black_viper", "mini_truck")
        .map { load(assets, "sprites/cars/$it.png") }

    // Sprites are drawn at their pixel size in world units, as in the 500px-wide original.
    val playerSize get() = Size(policeIdle.width.toFloat(), policeIdle.height.toFloat())
    val trafficSizes get() = traffic.map { Size(it.width.toFloat(), it.height.toFloat()) }
}
