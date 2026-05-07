package com.example.studyapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studyapp.ai.AiApiClient
import com.example.studyapp.ai.AiGenerateState
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

    val selectedDeck: StateFlow<FlashcardDeck?> = combine(
        _selectedDeckId,
        allDecks
    ) { deckId, decks ->
        if (deckId != null) decks.firstOrNull { it.id == deckId } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectDeck(deckId: Long) {
        _selectedDeckId.value = deckId
    }

    fun clearSelection() {
        _selectedDeckId.value = null
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
            // selectedDeck tự động cập nhật qua combine với allDecks
        }
    }

    fun updateCardMastery(cardId: Long, isLearned: Boolean) {
        viewModelScope.launch {
            repository.updateCardMastery(cardId, isLearned)
        }
    }

    // ── AI Generation ─────────────────────────────────────────────────────────

    private val _aiState = MutableStateFlow<AiGenerateState>(AiGenerateState.Idle)
    val aiState: StateFlow<AiGenerateState> = _aiState.asStateFlow()

    /**
     * Generate flashcards using the local LLM.
     * Streams tokens into [AiGenerateState.Generating], then transitions to [AiGenerateState.Preview].
     */
    fun generateFlashcards(
        context: Context,
        topic: String,
        count: Int,
        language: String,
        cardType: String = "term_def",
        modelFileName: String = ""
    ) {
        val handler = kotlinx.coroutines.CoroutineExceptionHandler { _, t ->
            _aiState.value = AiGenerateState.Error("Lỗi: ${t.message}")
        }
        viewModelScope.launch(handler) {
            _aiState.value = AiGenerateState.CheckingServer
            val status = AiApiClient.checkStatus()
            if (!status.isOnline) {
                _aiState.value = AiGenerateState.Error(
                    "Không kết nối được server AI.\n\nChạy server trên máy tính:\n  cd ai_server\n  python server.py",
                    isServerOffline = true
                )
                return@launch
            }
            if (!status.isReady) {
                _aiState.value = AiGenerateState.Error(
                    "Server online nhưng chưa load model.\n${status.message}",
                    isServerOffline = false
                )
                return@launch
            }

            _aiState.value = AiGenerateState.Generating
            try {
                val result = AiApiClient.generate(topic, count, language, cardType)
                _aiState.value = AiGenerateState.Preview(
                    cards = result.cards,
                    errorLines = result.errorLines,
                    durationMs = result.durationMs
                )
            } catch (t: Throwable) {
                _aiState.value = AiGenerateState.Error("Generate thất bại: ${t.message}")
            }
        }
    }

    /** Insert AI-generated cards into the deck and transition to [AiGenerateState.Success]. */
    fun confirmAiInsert(deckId: Long) {
        val preview = _aiState.value as? AiGenerateState.Preview ?: return
        viewModelScope.launch {
            repository.insertBulkCards(deckId, preview.cards)
            _aiState.value = AiGenerateState.Success(preview.cards.size)
        }
    }

    /** Reset AI state back to Idle. */
    fun resetAiState() {
        _aiState.value = AiGenerateState.Idle
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
