package com.example.studyapp.ai

/**
 * Parses raw LLM output into (front, back) pairs.
 *
 * Handles common LLM quirks:
 *   - Numbered lines: "1. Term ~ Def" → strips the number
 *   - Markdown bold: "**Term** ~ Def" → strips **
 *   - Extra whitespace
 *   - Lines without separator (reported as errors)
 *   - Duplicate fronts (deduplicated)
 */
object AiFlashcardParser {

    private val STRIP_NUMBERING = Regex("""^\s*\d+[\.\)]\s*""")
    private val STRIP_MARKDOWN   = Regex("""\*{1,2}|_{1,2}|`""")

    data class ParseResult(
        val cards: List<Pair<String, String>>,
        val errorLines: List<String>
    )

    fun parse(rawOutput: String): ParseResult {
        val lines = rawOutput
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val cards = mutableListOf<Pair<String, String>>()
        val errors = mutableListOf<String>()
        val seenFronts = mutableSetOf<String>()

        for (line in lines) {
            // Skip lines that look like headers or model artifacts
            if (line.startsWith("<") || line.startsWith("#") || line.startsWith("---")) continue

            val cleaned = line
                .replace(STRIP_NUMBERING, "")
                .replace(STRIP_MARKDOWN, "")
                .trim()

            val separatorIdx = cleaned.indexOf("~")
            if (separatorIdx < 0) {
                if (cleaned.isNotBlank()) errors.add(line)
                continue
            }

            val front = cleaned.substring(0, separatorIdx).trim()
            val back  = cleaned.substring(separatorIdx + 1).trim()

            if (front.isBlank() || back.isBlank()) {
                errors.add(line)
                continue
            }

            // Deduplicate by front (case-insensitive)
            val frontKey = front.lowercase()
            if (frontKey in seenFronts) continue
            seenFronts.add(frontKey)

            cards.add(Pair(front, back))
        }

        return ParseResult(cards, errors)
    }
}
