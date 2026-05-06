package com.example.studyapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.studyapp.data.dao.FlashcardDao
import com.example.studyapp.data.dao.FlashcardDeckDao
import com.example.studyapp.data.dao.NoteDao
import com.example.studyapp.data.dao.TodoDao
import com.example.studyapp.data.model.Flashcard
import com.example.studyapp.data.model.FlashcardDeck
import com.example.studyapp.data.model.Note
import com.example.studyapp.data.model.TodoItem

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE todo_items ADD COLUMN dueDate INTEGER")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE flashcard_decks ADD COLUMN lastStudiedIndex INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE flashcard_decks ADD COLUMN studiedCount INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [FlashcardDeck::class, Flashcard::class, Note::class, TodoItem::class],
    version = 3,
    exportSchema = false
)
abstract class KMAStudyDatabase : RoomDatabase() {
    abstract fun flashcardDeckDao(): FlashcardDeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun noteDao(): NoteDao
    abstract fun todoDao(): TodoDao

    companion object {
        @Volatile
        private var INSTANCE: KMAStudyDatabase? = null

        fun getDatabase(context: Context): KMAStudyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KMAStudyDatabase::class.java,
                    "kmastudy_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
