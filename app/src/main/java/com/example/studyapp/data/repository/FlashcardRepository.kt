package com.example.studyapp.data.repository

import com.example.studyapp.data.dao.FlashcardDao
import com.example.studyapp.data.dao.FlashcardDeckDao
import com.example.studyapp.data.model.Flashcard
import com.example.studyapp.data.model.FlashcardDeck
import kotlinx.coroutines.flow.Flow

class FlashcardRepository(
    private val deckDao: FlashcardDeckDao,
    private val cardDao: FlashcardDao
) {
    fun getAllDecks(): Flow<List<FlashcardDeck>> = deckDao.getAllDecks()

    fun getCardsForDeck(deckId: Long): Flow<List<Flashcard>> = cardDao.getCardsForDeck(deckId)

    suspend fun createDeck(name: String, description: String): Long {
        return deckDao.insertDeck(FlashcardDeck(name = name, description = description))
    }

    suspend fun updateDeck(deck: FlashcardDeck) = deckDao.updateDeck(deck)

    suspend fun deleteDeck(deck: FlashcardDeck) = deckDao.deleteDeck(deck)

    suspend fun addCard(deckId: Long, front: String, back: String): Long {
        return cardDao.insertCard(Flashcard(deckId = deckId, front = front, back = back))
    }

    suspend fun insertBulkCards(deckId: Long, cards: List<Pair<String, String>>) {
        val flashcards = cards.map { (front, back) ->
            Flashcard(deckId = deckId, front = front.trim(), back = back.trim())
        }
        cardDao.insertCards(flashcards)
    }

    suspend fun updateCard(card: Flashcard) = cardDao.updateCard(card)

    suspend fun deleteCard(card: Flashcard) = cardDao.deleteCard(card)

    suspend fun getDeckById(deckId: Long): FlashcardDeck? = deckDao.getDeckById(deckId)

    suspend fun getCardCountForDeck(deckId: Long): Int = deckDao.getCardCountForDeck(deckId)

    suspend fun saveStudyProgress(deckId: Long, lastIndex: Int, studiedCount: Int) {
        deckDao.saveStudyProgress(deckId, lastIndex, studiedCount, System.currentTimeMillis())
    }

    suspend fun updateCardMastery(cardId: Long, isLearned: Boolean) {
        cardDao.updateCardMastery(cardId, isLearned)
    }

    /**
     * Parse bulk input text separated by '~'
     * Format: "Front1~Back1\nFront2~Back2" or "Front1~Back1, Front2~Back2"
     * Returns list of (front, back) pairs and list of error lines
     */
    fun parseBulkInput(input: String): Pair<List<Pair<String, String>>, List<String>> {
        val lines = input.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val validCards = mutableListOf<Pair<String, String>>()
        val errorLines = mutableListOf<String>()

        for (line in lines) {
            val parts = line.split("~")
            if (parts.size >= 2) {
                val front = parts[0].trim()
                val back = parts.drop(1).joinToString("~").trim()
                if (front.isNotBlank() && back.isNotBlank()) {
                    validCards.add(Pair(front, back))
                } else {
                    errorLines.add(line)
                }
            } else {
                errorLines.add(line)
            }
        }
        return Pair(validCards, errorLines)
    }
}
