package com.example.studyapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.studyapp.MainActivity
import com.example.studyapp.R

object NotificationHelper {

    // ── Channels ──────────────────────────────────────────────────────────────
    const val CHANNEL_ID          = "kmastudy_reminders"
    const val CHANNEL_NAME        = "Nhắc nhở công việc"
    const val CHANNEL_DESC        = "Thông báo nhắc nhở trước khi đến hạn công việc"

    const val CHANNEL_PENDING_ID   = "kmastudy_pending"
    const val CHANNEL_PENDING_NAME = "Việc chưa hoàn thành"
    const val CHANNEL_PENDING_DESC = "Tóm tắt hàng ngày các việc còn tồn đọng"

    // ── Notification IDs ──────────────────────────────────────────────────────
    private const val NOTIF_ID_PENDING_SUMMARY = 8001

    // ── Channel creation ──────────────────────────────────────────────────────
    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Reminder channel (due-date alerts)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = CHANNEL_DESC
                    enableVibration(true)
                }
            )

            // Pending-tasks daily summary channel
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_PENDING_ID, CHANNEL_PENDING_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = CHANNEL_PENDING_DESC
                    enableVibration(false)
                }
            )
        }
    }

    // ── Due-date reminder ─────────────────────────────────────────────────────
    fun sendReminderNotification(
        context: Context,
        notifId: Int,
        title: String,
        message: String
    ) {
        createChannel(context)
        val pendingIntent = mainActivityIntent(context, notifId)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notifySafe(context, notifId, notification)
    }

    // ── Pending tasks summary (Inbox-style) ───────────────────────────────────
    /**
     * Pushes a rich summary notification listing all incomplete tasks.
     * Shows up to 5 task titles as inbox lines; remaining count in summary text.
     *
     * @param total           total pending task count
     * @param overdueCount    how many are already past due date
     * @param taskTitles      all pending task titles (full list)
     * @param hasHighPriority whether any task has priority == 2 (high)
     */
    fun sendPendingTasksSummary(
        context: Context,
        total: Int,
        overdueCount: Int,
        taskTitles: List<String>,
        hasHighPriority: Boolean
    ) {
        createChannel(context)
        if (total == 0) return

        val pendingIntent = mainActivityIntent(context, NOTIF_ID_PENDING_SUMMARY)

        // Build headline
        val overdueText = if (overdueCount > 0) " · $overdueCount quá hạn ⚠️" else ""
        val priorityText = if (hasHighPriority) " 🔴" else ""
        val headline = "$total việc chưa hoàn thành$overdueText$priorityText"

        // Build InboxStyle
        val style = NotificationCompat.InboxStyle()
            .setBigContentTitle(headline)
        val preview = taskTitles.take(5)
        preview.forEach { t ->
            val bullet = if (t.length > 42) t.take(42) + "…" else t
            style.addLine(bullet)
        }
        val remaining = total - preview.size
        if (remaining > 0) style.setSummaryText("+ $remaining việc khác")

        // Choose emoji for title based on urgency
        val titleEmoji = when {
            overdueCount > 0 -> "⚠️"
            hasHighPriority  -> "🔴"
            else             -> "📋"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_PENDING_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("$titleEmoji KMAStudy — Nhắc việc tồn đọng")
            .setContentText(headline)
            .setStyle(style)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup("pending_tasks_group")
            .build()

        notifySafe(context, NOTIF_ID_PENDING_SUMMARY, notification)
    }

    // ── Test notification ─────────────────────────────────────────────────────
    fun sendTestNotification(context: Context) {
        sendReminderNotification(
            context, 9999,
            "🔔 KMAStudy — Thử nghiệm thông báo",
            "Thông báo hoạt động bình thường! Bạn sẽ nhận được nhắc nhở 30 phút trước khi đến hạn."
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────
    private fun mainActivityIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notifySafe(context: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted
        }
    }
}

