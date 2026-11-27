package io.github.gabrielgaraujo.highwaypatrol

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class CrashSound(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundId = context.assets.openFd("sounds/crash.ogg").use { pool.load(it, 1) }

    fun play() {
        pool.play(soundId, 1f, 1f, 1, 0, 1f)
    }

    fun release() = pool.release()
}
