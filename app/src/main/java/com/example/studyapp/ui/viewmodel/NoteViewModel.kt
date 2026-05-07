package com.example.studyapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studyapp.data.model.Note
import com.example.studyapp.data.repository.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class NoteSortOrder { UPDATED, CREATED, TITLE }

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(NoteSortOrder.UPDATED)
    val sortOrder: StateFlow<NoteSortOrder> = _sortOrder.asStateFlow()

    private val _filterTag = MutableStateFlow<String?>(null)
    val filterTag: StateFlow<String?> = _filterTag.asStateFlow()

    val allNotes: StateFlow<List<Note>> = combine(
        _searchQuery.debounce(300),
        _sortOrder,
        _filterTag
    ) { query, sort, tag -> Triple(query, sort, tag) }
        .flatMapLatest { (query, sort, tag) ->
            val base = if (query.isBlank()) repository.getAllNotes()
                       else repository.searchNotes(query)
            base.map { notes ->
                var filtered = if (tag != null)
                    notes.filter { it.tags.split(",").map(String::trim).contains(tag) }
                else notes
                val sorted = when (sort) {
                    NoteSortOrder.UPDATED -> filtered.sortedByDescending { it.updatedAt }
                    NoteSortOrder.CREATED -> filtered.sortedByDescending { it.createdAt }
                    NoteSortOrder.TITLE   -> filtered.sortedBy { it.title.lowercase() }
                }
                // Pinned notes always on top
                sorted.sortedByDescending { it.isPinned }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Collect all unique tags across notes */
    val allTags: StateFlow<List<String>> = allNotes.map { notes ->
        notes.flatMap { it.tags.split(",").map(String::trim).filter(String::isNotBlank) }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSortOrder(order: NoteSortOrder) { _sortOrder.value = order }
    fun setFilterTag(tag: String?) { _filterTag.value = tag }

    fun createNote(
        title: String, content: String, color: Int = 0,
        tags: String = "", imageUris: String = "", links: String = ""
    ) {
        viewModelScope.launch {
            repository.createNote(title, content, color, tags, imageUris, links)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch { repository.updateNote(note) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { repository.updateNote(note.copy(isPinned = !note.isPinned)) }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { repository.deleteNote(note) }
    }
}

class NoteViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
