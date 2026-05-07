package com.example.studyapp.ai

/**
 * Builds structured prompts for flashcard generation.
 *
 * Output contract (enforced via prompt):
 *   - One flashcard per line
 *   - Format: "Front ~ Back"
 *   - No numbering, no markdown, no extra explanation
 *   - Front: short term/question
 *   - Back: concise definition/answer
 */
object FlashcardPromptBuilder {

    /**
     * Build a prompt for the given topic, count, and language.
     *
     * @param topic     Subject to generate flashcards about
     * @param count     Number of flashcards to generate (1–50)
     * @param language  Output language, e.g. "Vietnamese", "English"
     */
    fun build(topic: String, count: Int, language: String): String {
        val clampedCount = count.coerceIn(1, 50)
        return """<start_of_turn>user
Generate exactly $clampedCount flashcards about "$topic".

Rules:
- Output language: $language
- Each line must follow EXACTLY this format: Term ~ Definition
- One flashcard per line, no blank lines between cards
- Do NOT number the lines
- Do NOT use markdown (no **, no *, no #)
- Do NOT add any explanation, header, or footer
- Front (Term): short, specific keyword or question
- Back (Definition): clear, concise answer (1–2 sentences max)
- No duplicate fronts
- Start immediately with the first flashcard

Example format:
Photosynthesis ~ Process by which plants convert sunlight into glucose using CO2 and water
Mitochondria ~ Organelle that produces ATP energy for the cell
DNA ~ Double-helix molecule that carries genetic information

Now generate $clampedCount flashcards about "$topic":
<end_of_turn>
<start_of_turn>model
"""
    }
}
