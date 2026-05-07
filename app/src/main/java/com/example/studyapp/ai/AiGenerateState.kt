package com.example.studyapp.ai

/**
 * UI state for the AI flashcard generation screen.
 */
sealed class AiGenerateState {
    /** Initial / idle state */
    object Idle : AiGenerateState()

    /** Loading the LLM model into memory */
    object LoadingModel : AiGenerateState()

    /** Model loaded, generation in progress. [streamedText] accumulates tokens. */
    data class Generating(val streamedText: String = "") : AiGenerateState()

    /** Generation done, parsed cards ready for review before inserting */
    data class Preview(
        val cards: List<Pair<String, String>>,
        val errorLines: List<String> = emptyList()
    ) : AiGenerateState()

    /** Cards successfully inserted into Room */
    data class Success(val insertedCount: Int) : AiGenerateState()

    /** Something went wrong */
    data class Error(val message: String, val isModelMissing: Boolean = false) : AiGenerateState()
}
