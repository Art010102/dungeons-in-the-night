package com.dungeonsnight.game

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator

class AudioHub(private val ctx: Context) {

    private var music: MediaPlayer? = null
    private var volume = 0.8f
    private val tones = ToneGenerator(AudioManager.STREAM_MUSIC, 60)

    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        music?.setVolume(volume * 0.55f, volume * 0.55f)
    }

    fun startMusic() {
        stopMusic()
        try {
            music = MediaPlayer.create(ctx, R.raw.dungeon_theme)?.apply {
                isLooping = true
                setVolume(volume * 0.55f, volume * 0.55f)
                start()
            }
        } catch (_: Exception) { /* missing resource */ }
    }

    fun stopMusic() {
        try {
            music?.stop()
            music?.release()
        } catch (_: Exception) {}
        music = null
    }

    fun jump() { beep(ToneGenerator.TONE_CDMA_PIP, 70) }
    fun land() { beep(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 50) }
    fun swing() { beep(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 60) }
    fun hit() { beep(ToneGenerator.TONE_PROP_BEEP, 80) }
    fun hurt() { beep(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 120) }
    fun death() { beep(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200) }
    fun win() { beep(ToneGenerator.TONE_PROP_ACK, 180) }
    fun start() { beep(ToneGenerator.TONE_PROP_BEEP2, 90) }

    private fun beep(tone: Int, ms: Int) {
        if (volume <= 0.02f) return
        try { tones.startTone(tone, ms) } catch (_: Exception) {}
    }

    fun release() {
        stopMusic()
        try { tones.release() } catch (_: Exception) {}
    }
}
