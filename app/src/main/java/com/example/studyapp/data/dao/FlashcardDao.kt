package com.example.studyapp.data.dao

import androidx.room.*
import com.example.studyapp.data.model.FlashcardDeck
import com.example.studyapp.data.model.Flashcard
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDeckDao {
    @Query("""
        SELECT d.*, COUNT(c.id) as cardCount 
        FROM flashcard_decks d 
        LEFT JOIN flashcards c ON c.deckId = d.id 
        GROUP BY d.id 
        ORDER BY d.createdAt DESC
    """)
    fun getAllDecks(): Flow<List<FlashcardDeck>>

    @Query("SELECT * FROM flashcard_decks WHERE id = :deckId")
    suspend fun getDeckById(deckId: Long): FlashcardDeck?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeck(deck: FlashcardDeck): Long

    @Update
    suspend fun updateDeck(deck: FlashcardDeck)

    @Delete
    suspend fun deleteDeck(deck: FlashcardDeck)

    @Query("SELECT COUNT(*) FROM flashcards WHERE deckId = :deckId")
    suspend fun getCardCountForDeck(deckId: Long): Int

    @Query("UPDATE flashcard_decks SET lastStudiedIndex = :index, studiedCount = :count WHERE id = :deckId")
    suspend fun saveStudyProgress(deckId: Long, index: Int, count: Int)
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
}
