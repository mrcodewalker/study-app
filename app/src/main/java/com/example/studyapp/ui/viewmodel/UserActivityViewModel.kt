package com.example.studyapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studyapp.data.model.UserActivity
import com.example.studyapp.data.repository.UserActivityRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserActivityViewModel(private val repository: UserActivityRepository) : ViewModel() {
    val recentActivity: StateFlow<List<UserActivity>> = repository.getRecentActivity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun recordActivity(durationMillis: Long) {
        viewModelScope.launch {
            repository.recordActivity(durationMillis)
        }
    }
}

class UserActivityViewModelFactory(private val repository: UserActivityRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserActivityViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return UserActivityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
