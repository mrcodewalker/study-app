package com.example.studyapp.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.io.File

/**
 * Stub — on-device LLM via MediaPipe is not used in this build.
 * The app uses AiApiClient (HTTP) to talk to the Python AI server instead.
 */
object LlmInferenceManager {
    private const val TAG = "LlmInferenceManager"
    const val DEFAULT_MODEL_FILE = "gemma3-1b-it-int4.task"

    fun getModelPath(context: Context, fileName: String = DEFAULT_MODEL_FILE): String {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        return File(modelsDir, fileName).absolutePath
    }

    fun isModelAvailable(context: Context, fileName: String = DEFAULT_MODEL_FILE): Boolean =
        File(getModelPath(context, fileName)).exists()

    suspend fun initialize(context: Context, modelFileName: String = DEFAULT_MODEL_FILE) {
        Log.w(TAG, "On-device LLM not available in this build. Using HTTP server instead.")
    }

    fun generateStream(context: Context, prompt: String): Flow<String> = emptyFlow()

    suspend fun generate(prompt: String): String {
        throw UnsupportedOperationException("On-device LLM not available. Use AiApiClient instead.")
    }

    fun release() {
        Log.d(TAG, "LlmInferenceManager.release() — no-op stub")
    }
}

class ModelNotFoundException(message: String) : Exception(message)
class LlmInitException(message: String, cause: Throwable) : Exception(message, cause)
class LlmNotInitializedException(message: String) : Exception(message)
