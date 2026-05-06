package com.example.studyapp.notification

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

class TodoReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "Công việc sắp đến hạn"
        val todoId = inputData.getLong(KEY_TODO_ID, 0L)

        NotificationHelper.sendReminderNotification(
            context = context,
            notifId = todoId.toInt(),
            title = "⏰ KMAStudy — Nhắc nhở",
            message = "\"$title\" sẽ đến hạn trong 30 phút!"
        )
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "todo_title"
        const val KEY_TODO_ID = "todo_id"
        const val WORK_TAG = "todo_reminder"

        /**
         * Schedule a reminder 30 minutes before dueDate.
         * If dueDate is less than 30 min away, fire immediately.
         */
        fun schedule(context: Context, todoId: Long, title: String, dueDate: Long) {
            val now = System.currentTimeMillis()
            val fireAt = dueDate - 30 * 60 * 1000L  // 30 min before
            val delay = (fireAt - now).coerceAtLeast(0L)

            val data = workDataOf(
                KEY_TITLE to title,
                KEY_TODO_ID to todoId
            )

            val request = OneTimeWorkRequestBuilder<TodoReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .addTag("$WORK_TAG:$todoId")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "$WORK_TAG:$todoId",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }

        fun cancel(context: Context, todoId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork("$WORK_TAG:$todoId")
        }
    }
}
