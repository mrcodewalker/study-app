package com.example.studyapp.data.repository

import com.example.studyapp.data.dao.UserActivityDao
import com.example.studyapp.data.model.UserActivity
import kotlinx.coroutines.flow.Flow
import java.util.*

class UserActivityRepository(private val dao: UserActivityDao) {

    fun getRecentActivity(): Flow<List<UserActivity>> = dao.getRecentActivity()

    private fun getMidnight(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    suspend fun recordActivity(durationMillis: Long) {
        val midnight = getMidnight()
        val existing = dao.getActivityForDay(midnight)
        if (existing == null) {
            dao.insertOrUpdateActivity(UserActivity(date = midnight, durationMillis = durationMillis))
        } else {
            dao.incrementDuration(midnight, durationMillis, System.currentTimeMillis())
        }
    }

    /** Cập nhật aggregate timerMillis cho ngày hôm nay */
    suspend fun recordTimerSession(durationMillis: Long) {
        if (durationMillis <= 0) return
        val midnight = getMidnight()
        val existing = dao.getActivityForDay(midnight)
        if (existing == null) {
            dao.insertOrUpdateActivity(UserActivity(date = midnight, timerMillis = durationMillis))
        } else {
            dao.incrementTimerDuration(midnight, durationMillis)
        }
    }
}
