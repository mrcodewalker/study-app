package com.example.studyapp.data.repository

import com.example.studyapp.data.dao.NoteDao
import com.example.studyapp.data.model.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {
    fun getAllNotes(): Flow<List<Note>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun createNote(title: String, content: String, color: Int = 0): Long {
        return noteDao.insertNote(Note(title = title, content = content, color = color))
    }

    suspend fun updateNote(note: Note) = noteDao.updateNote(
        note.copy(updatedAt = System.currentTimeMillis())
    )

    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)
}
