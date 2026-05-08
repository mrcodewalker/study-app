package com.example.studyapp.music

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.studyapp.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ForegroundService phát nhạc từ assets/music/.
 * Chạy ngầm khi tắt màn hình.
 * UI giao tiếp qua companion StateFlow + Intent actions.
 */
class MusicService : Service() {

    companion object {
        const val CHANNEL_ID = "music_player_channel"
        const val NOTIF_ID   = 9002

        const val ACTION_PLAY   = "PLAY"
        const val ACTION_PAUSE  = "PAUSE"
        const val ACTION_RESUME = "RESUME"
        const val ACTION_STOP   = "STOP"
        const val ACTION_NEXT   = "NEXT"
        const val ACTION_PREV   = "PREV"
        const val ACTION_TOGGLE_AUTO_NEXT = "TOGGLE_AUTO_NEXT"
        const val ACTION_SET_AUTO_NEXT    = "SET_AUTO_NEXT"
        const val EXTRA_TRACK   = "TRACK"   // filename trong assets/music/
        const val EXTRA_AUTO_NEXT = "AUTO_NEXT"

        // ── Shared state ──────────────────────────────────────────────────
        private val _currentTrack = MutableStateFlow<String?>(null)
        private val _isPlaying    = MutableStateFlow(false)
        private val _playlist     = MutableStateFlow<List<String>>(emptyList())
        private val _isAutoNext   = MutableStateFlow(true) // Mặc định là bật

        val currentTrack: StateFlow<String?> = _currentTrack
        val isPlaying:    StateFlow<Boolean> = _isPlaying
        val playlist:     StateFlow<List<String>> = _playlist
        val isAutoNext:   StateFlow<Boolean> = _isAutoNext
    }

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Load danh sách nhạc
        _playlist.value = loadTracks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val track = intent.getStringExtra(EXTRA_TRACK) ?: return START_STICKY
                playTrack(track)
            }
            ACTION_PAUSE  -> pausePlayback()
            ACTION_RESUME -> resumePlayback()
            ACTION_STOP   -> {
                stopPlayback()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_NEXT -> skipTo(+1)
            ACTION_PREV -> skipTo(-1)
            ACTION_TOGGLE_AUTO_NEXT -> {
                _isAutoNext.value = !_isAutoNext.value
            }
            ACTION_SET_AUTO_NEXT -> {
                _isAutoNext.value = intent.getBooleanExtra(EXTRA_AUTO_NEXT, true)
            }
        }
        return START_STICKY
    }

    private fun playTrack(track: String) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            val afd = assets.openFd("music/$track")
            setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            isLooping = false
            setOnCompletionListener {
                if (_isAutoNext.value) {
                    skipTo(+1)
                } else {
                    _isPlaying.value = false
                    _currentTrack.value?.let { updateNotification(it, false) }
                }
            }
            prepare()
            start()
        }
        _currentTrack.value = track
        _isPlaying.value = true
        startForeground(NOTIF_ID, buildNotification(track, true))
    }

    private fun pausePlayback() {
        mediaPlayer?.pause()
        _isPlaying.value = false
        _currentTrack.value?.let { updateNotification(it, false) }
    }

    private fun resumePlayback() {
        mediaPlayer?.start()
        _isPlaying.value = true
        _currentTrack.value?.let { updateNotification(it, true) }
    }

    private fun stopPlayback() {
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        _currentTrack.value = null
    }

    private fun skipTo(delta: Int) {
        val list = _playlist.value
        if (list.isEmpty()) return
        val cur = _currentTrack.value
        val idx = list.indexOf(cur)
        val next = list[(idx + delta).mod(list.size)]
        playTrack(next)
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildNotification(track: String, playing: Boolean): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        fun svcIntent(action: String) = PendingIntent.getService(
            this, action.hashCode(),
            Intent(this, MusicService::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("KMAStudy — Nhạc")
            .setContentText(track.removeSuffix(".mp3"))
            .setContentIntent(tapIntent)
            .addAction(android.R.drawable.ic_media_previous, "Trước", svcIntent(ACTION_PREV))
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "Tạm dừng" else "Phát",
                svcIntent(if (playing) ACTION_PAUSE else ACTION_RESUME)
            )
            .addAction(android.R.drawable.ic_media_next, "Tiếp", svcIntent(ACTION_NEXT))
            .addAction(android.R.drawable.ic_delete, "Dừng", svcIntent(ACTION_STOP))
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2))
            .setOngoing(playing)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(track: String, playing: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(track, playing))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Trình phát nhạc",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Phát nhạc nền khi học" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun loadTracks(): List<String> = try {
        assets.list("music")?.filter { it.endsWith(".mp3") }?.sorted() ?: emptyList()
    } catch (e: Exception) { emptyList() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
