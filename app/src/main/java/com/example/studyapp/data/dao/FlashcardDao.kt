package com.example.studyapp.data.dao

import androidx.room.*
import com.example.studyapp.data.model.FlashcardDeck
import com.example.studyapp.data.model.Flashcard
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDeckDao {
    @Query("""
        SELECT d.id, d.name, d.description, d.createdAt, d.lastStudiedIndex, d.lastStudiedAt,
               COUNT(c.id) as cardCount,
               COALESCE(SUM(CASE WHEN c.isLearned = 1 THEN 1 ELSE 0 END), 0) as studiedCount
        FROM flashcard_decks d 
        LEFT JOIN flashcards c ON c.deckId = d.id 
        GROUP BY d.id 
        ORDER BY d.createdAt DESC
    """)
    fun getAllDecks(): Flow<List<FlashcardDeck>>

    @Query("""
        SELECT d.id, d.name, d.description, d.createdAt, d.lastStudiedIndex, d.lastStudiedAt,
               COUNT(c.id) as cardCount,
               COALESCE(SUM(CASE WHEN c.isLearned = 1 THEN 1 ELSE 0 END), 0) as studiedCount
        FROM flashcard_decks d 
        LEFT JOIN flashcards c ON c.deckId = d.id 
        WHERE d.id = :deckId
        GROUP BY d.id
    """)
    suspend fun getDeckById(deckId: Long): FlashcardDeck?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: FlashcardDeck): Long

    @Update
    suspend fun updateDeck(deck: FlashcardDeck)

    @Delete
    suspend fun deleteDeck(deck: FlashcardDeck)

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId")
    suspend fun getCardCountForDeck(deckId: Long): Int

    @Query("UPDATE flashcard_decks SET lastStudiedIndex = :index, studiedCount = :count, lastStudiedAt = :lastStudiedAt WHERE id = :deckId")
    suspend fun saveStudyProgress(deckId: Long, index: Int, count: Int, lastStudiedAt: Long)
}

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY createdAt ASC")
    fun getCardsForDeck(deckId: Long): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE deckId = :deckId ORDER BY createdAt ASC")
    suspend fun getCardsForDeckSync(deckId: Long): List<Flashcard>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Flashcard): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<Flashcard>)

    @Update
    suspend fun updateCard(card: Flashcard)

    @Delete
    suspend fun deleteCard(card: Flashcard)

    @Query("DELETE FROM flashcards WHERE deckId = :deckId")
    suspend fun deleteAllCardsInDeck(deckId: Long)

    @Query("UPDATE flashcards SET isLearned = :isLearned WHERE id = :cardId")
    suspend fun updateCardMastery(cardId: Long, isLearned: Boolean)
}
