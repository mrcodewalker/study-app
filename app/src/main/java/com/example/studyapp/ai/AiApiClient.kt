package com.example.studyapp.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * HTTP client that calls the local Python AI server.
 *
 * Emulator:  baseUrl = "http://10.0.2.2:8000"
 * Real device on same WiFi: baseUrl = "http://192.168.x.x:8000"
 */
object AiApiClient {

    // 10.0.2.2 = host machine from Android emulator
    // For real device on same WiFi, change to your PC's LAN IP e.g. "http://192.168.1.x:8000"
    var baseUrl: String = "http://10.0.2.2:8000"

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)  // 5 min for 50 long cards
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // Separate fast client just for status checks
    private val statusClient = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    data class ServerStatus(
        val isOnline: Boolean,
        val isReady: Boolean,
        val model: String,
        val message: String,
        val lanIp: String = ""   // parsed from server banner, empty if unavailable
    )

    data class GenerateResult(
        val cards: List<Pair<String, String>>,
        val errorLines: List<String>,
        val durationMs: Int
    )

    /** Check if server is reachable and model is loaded. */
    suspend fun checkStatus(): ServerStatus = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$baseUrl/status").get().build()
            val resp = statusClient.newCall(req).execute()
            val body = resp.body?.string() ?: "{}"
            val json = JSONObject(body)
            val status = json.optString("status", "error")
            ServerStatus(
                isOnline = true,
                isReady = status == "ready",
                model = json.optString("model", "unknown"),
                message = json.optString("message", ""),
                lanIp = json.optString("lan_ip", "")
            )
        } catch (e: Exception) {
            ServerStatus(
                isOnline = false,
                isReady = false,
                model = "",
                message = "Không kết nối được server: ${e.message}"
            )
        }
    }

    /** Generate flashcards via POST /generate */
    suspend fun generate(topic: String, count: Int, language: String, cardType: String = "term_def"): GenerateResult =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("topic", topic)
                put("count", count)
                put("language", language)
                put("card_type", cardType)
            }.toString().toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url("$baseUrl/generate")
                .post(body)
                .build()

            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) {
                val err = resp.body?.string() ?: "Unknown error"
                throw Exception("Server error ${resp.code}: $err")
            }

            val json = JSONObject(resp.body!!.string())
            val cardsArr = json.getJSONArray("cards")
            val cards = (0 until cardsArr.length()).map { i ->
                val card = cardsArr.getJSONObject(i)
                Pair(card.getString("front"), card.getString("back"))
            }
            val errArr = json.getJSONArray("error_lines")
            val errors = (0 until errArr.length()).map { errArr.getString(it) }

            GenerateResult(
                cards = cards,
                errorLines = errors,
                durationMs = json.optInt("duration_ms", 0)
            )
        }

    data class ChatResult(
        val sessionId: String,
        val reply: String,
        val durationMs: Int,
        val historyLength: Int
    )

    /** Send a chat message via POST /chat. Pass sessionId to continue a conversation. */
    suspend fun chat(
        message: String,
        sessionId: String,
        language: String = "Vietnamese"
    ): ChatResult = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("message", message)
            put("session_id", sessionId)
            put("language", language)
        }.toString().toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url("$baseUrl/chat")
            .post(body)
            .build()

        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) {
            val err = resp.body?.string() ?: "Unknown error"
            throw Exception("Server error ${resp.code}: $err")
        }

        val json = JSONObject(resp.body!!.string())
        ChatResult(
            sessionId = json.getString("session_id"),
            reply = json.getString("reply"),
            durationMs = json.optInt("duration_ms", 0),
            historyLength = json.optInt("history_length", 0)
        )
    }

    /** Delete a chat session via DELETE /chat/{sessionId} */
    suspend fun deleteChatSession(sessionId: String) = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$baseUrl/chat/$sessionId")
            .delete()
            .build()
        client.newCall(req).execute()
    }
}
