package com.example.studyapp.ai

sealed class AiGenerateState {
    object Idle : AiGenerateState()
    object CheckingServer : AiGenerateState()
    object Generating : AiGenerateState()
    data class Preview(
        val cards: List<Pair<String, String>>,
        val errorLines: List<String> = emptyList(),
        val durationMs: Int = 0
    ) : AiGenerateState()
    data class Success(val insertedCount: Int) : AiGenerateState()
    data class Error(val message: String, val isServerOffline: Boolean = false) : AiGenerateState()
}
