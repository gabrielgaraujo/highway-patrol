package io.github.gabrielgaraujo.highwaypatrol

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer

/**
 * Looping background music (assets/sounds/music.ogg, rendered by tools/make_music.py).
 * Whether it's on is remembered between launches.
 */
class Music(context: Context) {
    private val prefs = context.getSharedPreferences("highway_patrol", Context.MODE_PRIVATE)

    // If the track can't be loaded the game simply plays without music.
    private val player: MediaPlayer? = runCatching {
        context.assets.openFd("sounds/music.ogg").use { fd ->
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                isLooping = true
                prepare()
            }
        }
    }.getOrNull()

    private var resumed = false
    private var ducked = false

    var enabled: Boolean = prefs.getBoolean(KEY_ENABLED, true)
        set(value) {
            field = value
            prefs.edit().putBoolean(KEY_ENABLED, value).apply()
            update()
        }

    init {
        applyVolume()
    }

    /** Quieter while the crash screen is up. */
    fun duck(on: Boolean) {
        ducked = on
        applyVolume()
    }

    fun resume() {
        resumed = true
        update()
    }

    fun pause() {
        resumed = false
        update()
    }

    fun release() {
        player?.release()
    }

    private fun update() {
        val player = player ?: return
        if (resumed && enabled) {
            if (!player.isPlaying) player.start()
        } else if (player.isPlaying) {
            player.pause()
        }
    }

    private fun applyVolume() {
        val volume = if (ducked) DUCKED_VOLUME else VOLUME
        player?.setVolume(volume, volume)
    }

    private companion object {
        const val KEY_ENABLED = "music_enabled"
        const val VOLUME = 0.55f
        const val DUCKED_VOLUME = 0.2f
    }
}
