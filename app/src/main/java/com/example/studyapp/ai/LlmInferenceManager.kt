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
 * Download from: https://huggingface.co/litert-community/Gemma3-1B-IT
 */
object LlmInferenceManager {
    const val DEFAULT_MODEL_FILE = "gemma3-1B-it-int4.task"
    const val DEFAULT_MODEL_FILE = "gemma-2b-it-cpu-int4.bin"

    const val DEFAULT_MODEL_FILE = "gemma3-1b-it-int4.task"

    private const val MAX_TOKENS = 512
    private const val TEMPERATURE = 0.3f
    private const val TOP_K = 20

ming needs listener in options)
    @Volatile private var syncEngine: LlmInference? = null
    @Volatile private var loadedModelPath: String? = null

    fun getModelPath(context: Context, fileName: String = DEFAULT_MODEL_FILE): String {
        val modelsDir = File(context.getExternalFilesDir(null), "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        return File(modelsDir, fileName).absolutePath
    }

    fun isModelAvailable(context: Context, fileName: String = DEFAULT_MODEL_FILE): Boolean =
        File(getModelPath(context, fileName)).exists()

    /**
     * Initialize the sync engine. Call from a background coroutine.
     */
    susinitialize(context: Context, modelFileName: String = DEFAULT_MODEL_FILE) {
        withContext(Dispatchers.IO) {
            val modelPath = getModelPath(context, modelFileName)

            if (syncEngine != null && loadedModelPath == modelPath) {
                Log.d(TAG, "LLM already initialized")
                return@withContext
            }

            if (!File(modelPath).exists()) {
                throw ModelNotFoundException(
                    "Model not found: $modelPath\n" +
            "Upload via Device File Explorer to:\n" +
                    "sdcard/Android/data/com.example.studyapp/files/models/"
                )
            }

            Log.d(TAG, "Initializing sync LLM from $modelPath")
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
                Log.d(TAG, "LLM initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize LLM", e)
                throw LlmInitException("Failed to load model: ${e.message}", e)
            }
        }
    }

    /**
     * Stream generation using a fresh LlmInference instance with setResultListener in options.
     * The official API requires the listener to be set at build time, not passed to generateResponseAsync.
     */
    fun generateStream(context: Context, prompt: String): Flow<String> = callbackFlow {
        val modelPath = loadedModelPath
            ?: throw LlmNotInitializedException("LLM not initialized. Call initialize() first.")

        Log.d(TAG, "Starting streaming generation")

        var streamEngine: LlmInferencell
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

    /**
     * Synchronous generation — simpler fallback if streaming has issues.
     */
    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val engine = syncEngine
            ?: throw Llitialize() first.")
        Log.d(TAG, "Generating response (sync)...")
        val result = engine.generateResponse(prompt)
        Log.d(TAG, "Generation complete, length=${result.length}")
        result
    }

    fun release() {
        syncEngine?.close()
        syncEngine = null
        loadedModelPath = null
        Log.d(TAG, "LLM released")
    }
}

class ModelNotFoundException(message: String) : Exception(message)
class LlmInitException(message: String, caus, cause)
class LlmNotInitializedException(message: String) : Exception(message)
