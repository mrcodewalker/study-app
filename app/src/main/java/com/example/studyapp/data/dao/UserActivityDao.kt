package com.example.studyapp.data.dao

import androidx.room.*
import com.example.studyapp.data.model.UserActivity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserActivityDao {
    @Query("SELECT * FROM user_activity WHERE date = :date")
    suspend fun getActivityForDay(date: Long): UserActivity?

    @Query("SELECT * FROM user_activity ORDER BY date DESC LIMIT 7")
    fun getRecentActivity(): Flow<List<UserActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateActivity(activity: UserActivity)

    @Query("UPDATE user_activity SET durationMillis = durationMillis + :additionalMillis, lastActiveTime = :now WHERE date = :date")
    suspend fun incrementDuration(date: Long, additionalMillis: Long, now: Long)

    @Query("UPDATE user_activity SET timerMillis = timerMillis + :additionalMillis WHERE date = :date")
    suspend fun incrementTimerDuration(date: Long, additionalMillis: Long)
}
