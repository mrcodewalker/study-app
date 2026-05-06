package com.example.studyapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studyapp.data.model.TodoItem
import com.example.studyapp.data.repository.TodoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TodoViewModel(private val repository: TodoRepository) : ViewModel() {

    val allTodos: StateFlow<List<TodoItem>> = repository.getAllTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingCount: StateFlow<Int> = repository.getPendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun addTodo(title: String, priority: Int = 0, dueDate: Long? = null) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.addTodo(title.trim(), priority, dueDate) }
    }

    fun toggleComplete(todo: TodoItem) {
        viewModelScope.launch { repository.toggleComplete(todo) }
    }

    fun updateTodo(todo: TodoItem) {
        viewModelScope.launch { repository.updateTodo(todo) }
    }

    fun deleteTodo(todo: TodoItem) {
        viewModelScope.launch { repository.deleteTodo(todo) }
    }

    fun clearCompleted() {
        viewModelScope.launch { repository.deleteCompletedTodos() }
    }
}

class TodoViewModelFactory(private val repository: TodoRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TodoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TodoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
