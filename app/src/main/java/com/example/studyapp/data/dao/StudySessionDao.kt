package com.example.studyapp.data.dao

import androidx.room.*
import com.example.studyapp.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {
    @Insert
    suspend fun insert(session: StudySession): Long

    @Delete
    suspend fun delete(session: StudySession)

    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions WHERE startedAt >= :from AND startedAt < :to ORDER BY startedAt DESC")
    fun getSessionsInRange(from: Long, to: Long): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC LIMIT 30")
    fun getRecentSessions(): Flow<List<StudySession>>
}
