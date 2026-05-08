package com.example.studyapp.notification

import android.content.Context
import androidx.work.*
import com.example.studyapp.data.db.KMAStudyDatabase
import java.util.concurrent.TimeUnit

/**
 * Periodic worker: fires once a day to push a summary notification
 * for all incomplete (pending) todo items.
 * Also exposes a [scheduleImmediate] helper to trigger it right away.
 */
class PendingTasksNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val db = KMAStudyDatabase.getDatabase(context)
        // Query synchronously — Worker runs on background thread
        val pending = db.todoDao().getAllTodosSync().filter { !it.isCompleted }

        if (pending.isEmpty()) return Result.success()

        val highPriority = pending.filter { it.priority == 2 }
        val overdue = pending.filter {
            it.dueDate != null && it.dueDate < System.currentTimeMillis()
        }

        NotificationHelper.sendPendingTasksSummary(
            context = context,
            total = pending.size,
            overdueCount = overdue.size,
            taskTitles = pending.map { it.title },
            hasHighPriority = highPriority.isNotEmpty()
        )
        return Result.success()
    }

    companion object {
        private const val WORK_NAME_DAILY = "pending_tasks_daily"
        private const val WORK_NAME_IMMEDIATE = "pending_tasks_now"

        /** Schedule a daily periodic reminder at ~08:00 each morning */
        fun scheduleDaily(context: Context) {
            val request = PeriodicWorkRequestBuilder<PendingTasksNotificationWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(calculateDelayToMorning(), TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .addTag(WORK_NAME_DAILY)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_DAILY,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Fire immediately — for "push now" button in TodoScreen */
        fun scheduleImmediate(context: Context) {
            val request = OneTimeWorkRequestBuilder<PendingTasksNotificationWorker>()
                .addTag(WORK_NAME_IMMEDIATE)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        /** Calculate delay until next 08:00 AM */
        private fun calculateDelayToMorning(): Long {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 8)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(java.util.Calendar.DAY_OF_YEAR, 1)
                }
            }
            return (cal.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        }
    }
}
