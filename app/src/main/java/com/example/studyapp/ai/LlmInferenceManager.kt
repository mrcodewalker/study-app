package com.example.studyapp.ai

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages the lifecycle of a MediaPipe LLM Inference session.
 *
 * Model placement (push via adb or Device File Explorer):
 *   /sdcard/Android/data/com.example.studyapp/files/models/<MODEL_FILE_NAME>
 *
 * Recommended model: gemma3-1b-it-int4.task (~500MB)
 */
object LlmInferenceManager {

    private const val TAG = "LlmInferenceManager"
    const val DEFAULT_MODEL_FILE = "gemma3-1b-it-int4.task"
    private const val MAX_TOKENS = 512
    private const val TEMPERATURE = 0.3f
    private const val TOP_K = 20

    @Volatile private var syncEngine: LlmInference? = null
    @Volatile private var loadedModelPath: String? = null

    fun getModelPath(context: Context, fileName: String = DEFAULT_MODEL_FILE): String {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        return File(modelsDir, fileName).absolutePath
    }

    fun isModelAvailable(context: Context, fileName: String = DEFAULT_MODEL_FILE): Boolean =
        File(getModelPath(context, fileName)).exists()

    suspend fun initialize(context: Context, modelFileName: String = DEFAULT_MODEL_FILE) {
        withContext(Dispatchers.IO) {
            val modelPath = getModelPath(context, modelFileName)
            if (syncEngine != null && loadedModelPath == modelPath) return@withContext
            if (!File(modelPath).exists()) {
                throw ModelNotFoundException("Model not found: $modelPath")
            }
            try {
                syncEngine?.close()
                syncEngine = null
                val options = LlmInferenceOptions.builder()
                    .setModelPath(modelPath)
                    .setMaxTokens(MAX_TOKENS)
                    .setTemperature(TEMPERATURE)
                    .setTopK(TOP_K)
                    .setRandomSeed(42)
                    .build()
                syncEngine = LlmInference.createFromOptions(context, options)
                loadedModelPath = modelPath
                Log.d(TAG, "LLM initialized: $modelFileName")
            } catch (e: Exception) {
                throw LlmInitException("Failed to load model: ${e.message}", e)
            }
        }
    }

    /**
     * Stream generation.
     * Per official docs: setResultListener must be set in options builder,
     * then call generateResponseAsync(prompt) with no callback argument.
     */
    fun generateStream(context: Context, prompt: String): Flow<String> = callbackFlow {
        val modelPath = loadedModelPath
            ?: throw LlmNotInitializedException("Call initialize() first.")

        var streamEngine: LlmInference? = null
        try {
            val options = LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(MAX_TOKENS)
                .setTemperature(TEMPERATURE)
                .setTopK(TOP_K)
                .setRandomSeed(42)
                .setResultListener { partialResult, done ->
                    if (partialResult != null) trySend(partialResult)
                    if (done) close()
                }
                .build()
            streamEngine = LlmInference.createFromOptions(context, options)
            streamEngine.generateResponseAsync(prompt)
        } catch (e: Exception) {
            close(e)
        }

        awaitClose { streamEngine?.close() }
    }.flowOn(Dispatchers.IO)

    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val engine = syncEngine
            ?: throw LlmNotInitializedException("Call initialize() first.")
        engine.generateResponse(prompt)
    }

    fun release() {
        syncEngine?.close()
        syncEngine = null
        loadedModelPath = null
    }
}

class ModelNotFoundException(message: String) : Exception(message)
class LlmInitException(message: String, cause: Throwable? = null) : Exception(message, cause)
class LlmNotInitializedException(message: String) : Exception(message)
