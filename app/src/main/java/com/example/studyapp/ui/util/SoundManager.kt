package com.example.studyapp.ui.util

import android.content.Context
import android.media.MediaPlayer
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Quản lý toàn bộ sound effect trong app.
 * Dùng SoundPool cho các sound ngắn (click, swipe, flip, success, error, confetti, cong*).
 */
object SoundManager {

    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<String, Int>()
    private var loaded = false

    // Danh sách file cong* để random
    private val congSounds = mutableListOf<String>()

    fun init(context: Context) {
        if (loaded) return
        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .build()

        val assets = context.assets.list("sound") ?: return
        assets.forEach { file ->
            val key = file.removeSuffix(".mp3")
            val id = soundPool!!.load(context.assets.openFd("sound/$file"), 1)
            soundIds[key] = id
            if (key.startsWith("cong") && key.removePrefix("cong").all { it.isDigit() }) {
                congSounds.add(key)
            }
        }
        loaded = true
    }

    fun play(key: String) {
        val id = soundIds[key] ?: return
        soundPool?.play(id, 1f, 1f, 1, 0, 1f)
    }

    /** Phát confetti rồi await ~800ms sau đó random 1 cong* */
    fun playConfettiThenCong(scope: CoroutineScope) {
        play("confetti")
        scope.launch(Dispatchers.Main) {
            delay(800)
            if (congSounds.isNotEmpty()) {
                play(congSounds.random())
            }
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
        congSounds.clear()
        loaded = false
    }
}
