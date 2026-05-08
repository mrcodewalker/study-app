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
import com.example.studyapp.data.dao.StudySessionDao
import com.example.studyapp.data.dao.TodoDao
import com.example.studyapp.data.model.Flashcard
import com.example.studyapp.data.model.FlashcardDeck
import com.example.studyapp.data.model.Note
import com.example.studyapp.data.model.StudySession
import com.example.studyapp.data.model.TodoItem
import com.example.studyapp.data.model.UserActivity
import com.example.studyapp.data.dao.UserActivityDao

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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE flashcard_decks ADD COLUMN lastStudiedAt INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE flashcards ADD COLUMN isLearned INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS `user_activity` (`date` INTEGER NOT NULL, `durationMillis` INTEGER NOT NULL, `lastActiveTime` INTEGER NOT NULL, PRIMARY KEY(`date`))")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE notes ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE notes ADD COLUMN imageUris TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE notes ADD COLUMN links TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE user_activity ADD COLUMN timerMillis INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `study_sessions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `subject` TEXT NOT NULL DEFAULT '',
                `durationMillis` INTEGER NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `note` TEXT NOT NULL DEFAULT ''
            )
        """)
    }
}

@Database(
    entities = [FlashcardDeck::class, Flashcard::class, Note::class, TodoItem::class, UserActivity::class, StudySession::class],
    version = 9,
    exportSchema = false
)
abstract class KMAStudyDatabase : RoomDatabase() {
    abstract fun flashcardDeckDao(): FlashcardDeckDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun noteDao(): NoteDao
    abstract fun todoDao(): TodoDao
    abstract fun userActivityDao(): UserActivityDao
    abstract fun studySessionDao(): StudySessionDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
