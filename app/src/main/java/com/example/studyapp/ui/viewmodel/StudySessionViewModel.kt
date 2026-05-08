package com.example.studyapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studyapp.data.model.StudySession
import com.example.studyapp.data.repository.StudySessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudySessionViewModel(private val repo: StudySessionRepository) : ViewModel() {

    val allSessions: StateFlow<List<StudySession>> = repo.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSessions: StateFlow<List<StudySession>> = repo.getRecentSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSession(subject: String, durationMillis: Long, note: String = "") {
        if (durationMillis < 1000) return // bỏ qua session < 1 giây
        viewModelScope.launch {
            repo.addSession(StudySession(subject = subject, durationMillis = durationMillis, note = note))
        }
    }

    fun deleteSession(session: StudySession) {
        viewModelScope.launch { repo.deleteSession(session) }
    }
}

class StudySessionViewModelFactory(private val repo: StudySessionRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return StudySessionViewModel(repo) as T
    }
}
