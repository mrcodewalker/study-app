package com.example.studyapp.data.repository

import com.example.studyapp.data.dao.StudySessionDao
import com.example.studyapp.data.model.StudySession
import kotlinx.coroutines.flow.Flow

class StudySessionRepository(private val dao: StudySessionDao) {
    fun getAllSessions(): Flow<List<StudySession>> = dao.getAllSessions()
    fun getRecentSessions(): Flow<List<StudySession>> = dao.getRecentSessions()

    suspend fun addSession(session: StudySession) = dao.insert(session)
    suspend fun deleteSession(session: StudySession) = dao.delete(session)
}
