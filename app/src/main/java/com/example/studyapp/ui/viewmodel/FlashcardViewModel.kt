package com.example.studyapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.studyapp.ai.AiFlashcardParser
import com.example.studyapp.ai.AiGenerateState
import com.example.studyapp.ai.FlashcardPromptBuilder
import com.example.studyapp.ai.LlmInferenceManager
import com.example.studyapp.ai.ModelNotFoundException
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
        modelFileName: String = LlmInferenceManager.DEFAULT_MODEL_FILE
    ) {
        viewModelScope.launch {
            _aiState.value = AiGenerateState.LoadingModel
            try {
                LlmInferenceManager.initialize(context, modelFileName)
            } catch (e: ModelNotFoundException) {
                _aiState.value = AiGenerateState.Error(e.message ?: "Model not found", isModelMissing = true)
                return@launch
            } catch (e: Exception) {
                _aiState.value = AiGenerateState.Error("Failed to load model: ${e.message}")
                return@launch
            }

            val prompt = FlashcardPromptBuilder.build(topic, count, language)
            _aiState.value = AiGenerateState.Generating()

            try {
                val fullOutput = StringBuilder()
                LlmInferenceManager.generateStream(context, prompt).collect { token ->
                    fullOutput.append(token)
                    _aiState.value = AiGenerateState.Generating(fullOutput.toString())
                }
                val result = AiFlashcardParser.parse(fullOutput.toString())
                _aiState.value = AiGenerateState.Preview(result.cards, result.errorLines)
            } catch (e: Exception) {
                _aiState.value = AiGenerateState.Error("Generation failed: ${e.message}")
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
