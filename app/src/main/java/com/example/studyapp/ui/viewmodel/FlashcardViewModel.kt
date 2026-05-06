package com.example.studyapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studyapp.data.model.Flashcard
import com.example.studyapp.data.model.FlashcardDeck
import com.example.studyapp.data.repository.FlashcardRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class BulkPreviewState(
    val validCards: List<Pair<String, String>> = emptyList(),
    val errorLines: List<String> = emptyList(),
    val isVisible: Boolean = false
)

class FlashcardViewModel(private val repository: FlashcardRepository) : ViewModel() {

    val allDecks: StateFlow<List<FlashcardDeck>> = repository.getAllDecks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDeckId = MutableStateFlow<Long?>(null)
    val selectedDeckId: StateFlow<Long?> = _selectedDeckId.asStateFlow()

    val cardsForSelectedDeck: StateFlow<List<Flashcard>> = _selectedDeckId
        .flatMapLatest { deckId ->
            if (deckId != null) repository.getCardsForDeck(deckId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _bulkPreview = MutableStateFlow(BulkPreviewState())
    val bulkPreview: StateFlow<BulkPreviewState> = _bulkPreview.asStateFlow()

    private val _selectedDeck = MutableStateFlow<FlashcardDeck?>(null)
    val selectedDeck: StateFlow<FlashcardDeck?> = _selectedDeck.asStateFlow()

    fun selectDeck(deckId: Long) {
        _selectedDeckId.value = deckId
        viewModelScope.launch {
            _selectedDeck.value = repository.getDeckById(deckId)
        }
    }

    fun clearSelection() {
        _selectedDeckId.value = null
        _selectedDeck.value = null
    }

    fun createDeck(name: String, description: String) {
        viewModelScope.launch {
            repository.createDeck(name, description)
        }
    }

    fun updateDeck(deck: FlashcardDeck) {
        viewModelScope.launch {
            repository.updateDeck(deck)
        }
    }

    fun deleteDeck(deck: FlashcardDeck) {
        viewModelScope.launch {
            repository.deleteDeck(deck)
        }
    }

    fun addSingleCard(deckId: Long, front: String, back: String) {
        viewModelScope.launch {
            repository.addCard(deckId, front, back)
        }
    }

    fun parseBulkInput(input: String) {
        val (valid, errors) = repository.parseBulkInput(input)
        _bulkPreview.value = BulkPreviewState(
            validCards = valid,
            errorLines = errors,
            isVisible = true
        )
    }

    fun confirmBulkInsert(deckId: Long) {
        viewModelScope.launch {
            val cards = _bulkPreview.value.validCards
            if (cards.isNotEmpty()) {
                repository.insertBulkCards(deckId, cards)
            }
            _bulkPreview.value = BulkPreviewState()
        }
    }

    fun cancelBulkInsert() {
        _bulkPreview.value = BulkPreviewState()
    }

    fun updateCard(card: Flashcard) {
        viewModelScope.launch {
            repository.updateCard(card)
        }
    }

    fun deleteCard(card: Flashcard) {
        viewModelScope.launch {
            repository.deleteCard(card)
        }
    }

    fun saveStudyProgress(deckId: Long, lastIndex: Int, studiedCount: Int) {
        viewModelScope.launch {
            repository.saveStudyProgress(deckId, lastIndex, studiedCount)
        }
    }
}

class FlashcardViewModelFactory(private val repository: FlashcardRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FlashcardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FlashcardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
