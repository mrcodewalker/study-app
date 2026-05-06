package com.example.studyapp.data.dao

import androidx.room.*
import com.example.studyapp.data.model.TodoItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todo_items ORDER BY isCompleted ASC, priority DESC, createdAt DESC")
    fun getAllTodos(): Flow<List<TodoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoItem): Long

    @Update
    suspend fun updateTodo(todo: TodoItem)

    @Delete
    suspend fun deleteTodo(todo: TodoItem)

    @Query("DELETE FROM todo_items WHERE isCompleted = 1")
    suspend fun deleteCompletedTodos()

    @Query("SELECT COUNT(*) FROM todo_items WHERE isCompleted = 0")
    fun getPendingCount(): Flow<Int>
}
