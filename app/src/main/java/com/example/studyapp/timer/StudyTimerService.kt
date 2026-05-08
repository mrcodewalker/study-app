package com.example.studyapp.timer

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.studyapp.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ForegroundService giữ đồng hồ bấm giờ chạy khi màn hình tắt.
 * Giao tiếp với UI qua companion object StateFlow (in-process).
 */
class StudyTimerService : Service() {

    companion object {
        const val CHANNEL_ID       = "study_timer_channel"
        const val CHANNEL_ALARM_ID = "study_timer_alarm_channel"
        const val NOTIF_ID         = 9001
        const val NOTIF_ALARM_ID   = 9003

        const val ACTION_START   = "START"
        const val ACTION_PAUSE   = "PAUSE"
        const val ACTION_RESUME  = "RESUME"
        const val ACTION_STOP    = "STOP"
        const val EXTRA_TARGET_MS = "TARGET_MS"   // 0 = stopwatch, >0 = countdown

        // ── Shared state (in-process) ─────────────────────────────────────────
        private val _elapsedMs   = MutableStateFlow(0L)
        private val _isRunning   = MutableStateFlow(false)
        private val _isCountdown = MutableStateFlow(false)
        private val _targetMs    = MutableStateFlow(0L)
        private val _finished    = MutableStateFlow(false)

        val elapsedMs:   StateFlow<Long>    = _elapsedMs
        val isRunning:   StateFlow<Boolean> = _isRunning
        val isCountdown: StateFlow<Boolean> = _isCountdown
        val targetMs:    StateFlow<Long>    = _targetMs
        val finished:    StateFlow<Boolean> = _finished

        fun resetFinished() { _finished.value = false }
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null
    private var startTimeMs = 0L
    private var accumulatedMs = 0L
    private var targetDurationMs = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                targetDurationMs = intent.getLongExtra(EXTRA_TARGET_MS, 0L)
                accumulatedMs = 0L
                _elapsedMs.value = 0L
                _targetMs.value = targetDurationMs
                _isCountdown.value = targetDurationMs > 0L
                _finished.value = false
                startForeground(NOTIF_ID, buildNotification("Đang học..."))
                startTicking()
            }
            ACTION_PAUSE -> pauseTicking()
            ACTION_RESUME -> resumeTicking()
            ACTION_STOP -> {
                stopTicking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startTicking() {
        startTimeMs = SystemClock.elapsedRealtime()
        _isRunning.value = true
        timerJob = scope.launch {
            while (isActive) {
                delay(500)
                val now = SystemClock.elapsedRealtime()
                val total = accumulatedMs + (now - startTimeMs)
                _elapsedMs.value = total

                // Countdown: kiểm tra hết giờ
                if (targetDurationMs > 0L && total >= targetDurationMs) {
                    _elapsedMs.value = targetDurationMs
                    _isRunning.value = false
                    _finished.value = true
                    triggerAlarm()
                    updateNotification("⏰ Hết giờ!")
                    break
                }

                updateNotification(formatTime(total))
            }
        }
    }

    private fun pauseTicking() {
        accumulatedMs += SystemClock.elapsedRealtime() - startTimeMs
        timerJob?.cancel()
        _isRunning.value = false
        updateNotification("⏸ Tạm dừng — ${formatTime(accumulatedMs)}")
    }

    private fun resumeTicking() {
        startTimeMs = SystemClock.elapsedRealtime()
        _isRunning.value = true
        startTicking()
    }

    private fun stopTicking() {
        timerJob?.cancel()
        _isRunning.value = false
        _elapsedMs.value = 0L
        accumulatedMs = 0L
    }

    private fun triggerAlarm() {
        // 1. Rung máy — pattern dài hơn để dễ nhận ra
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 600, 200, 600, 200, 600, 400, 1200)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }

        // 2. Phát chuông báo qua RingtoneManager (chạy ngay cả khi tắt màn hình)
        try {
            val alarmUri = android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_ALARM
            ) ?: android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_NOTIFICATION
            )
            val ringtone = android.media.RingtoneManager.getRingtone(this, alarmUri)
            ringtone?.play()
            // Tự tắt sau 4 giây
            scope.launch {
                delay(4000)
                ringtone?.stop()
            }
        } catch (_: Exception) {}

        // 3. Notification riêng cho hết giờ — có âm thanh + đèn
        sendFinishedNotification()
    }

    private fun sendFinishedNotification() {
        val tapIntent = PendingIntent.getActivity(
            this, 10,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmUri = android.media.RingtoneManager.getDefaultUri(
            android.media.RingtoneManager.TYPE_NOTIFICATION
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_ALARM_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⏰ Hết giờ học!")
            .setContentText("Bạn đã học được ${formatTime(targetDurationMs)}. Nghỉ ngơi một chút nhé!")
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setSound(alarmUri)
            .setLights(0xFF6750A4.toInt(), 500, 500)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ALARM_ID, notif)
    }

    private fun buildNotification(text: String): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, StudyTimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("KMAStudy — Đồng hồ học")
            .setContentText(text)
            .setContentIntent(tapIntent)
            .addAction(android.R.drawable.ic_delete, "Dừng", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            // Channel thường — silent, ongoing
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Đồng hồ học tập", NotificationManager.IMPORTANCE_LOW)
                    .apply { description = "Hiển thị thời gian học đang chạy" }
            )
            // Channel alarm — có âm thanh + đèn
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ALARM_ID, "Báo hết giờ học", NotificationManager.IMPORTANCE_HIGH)
                    .apply {
                        description = "Thông báo khi đếm ngược kết thúc"
                        enableLights(true)
                        lightColor = 0xFF6750A4.toInt()
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 600, 200, 600, 200, 600)
                    }
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
