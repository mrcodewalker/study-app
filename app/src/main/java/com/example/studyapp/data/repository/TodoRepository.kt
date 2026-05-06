package com.example.studyapp.data.repository

import com.example.studyapp.data.dao.TodoDao
import com.example.studyapp.data.model.TodoItem
import kotlinx.coroutines.flow.Flow

class TodoRepository(private val todoDao: TodoDao) {
    fun getAllTodos(): Flow<List<TodoItem>> = todoDao.getAllTodos()
    fun getPendingCount(): Flow<Int> = todoDao.getPendingCount()

    suspend fun addTodo(title: String, priority: Int = 0, dueDate: Long? = null): Long {
        return todoDao.insertTodo(TodoItem(title = title, priority = priority, dueDate = dueDate))
    }

    suspend fun toggleComplete(todo: TodoItem) {
        todoDao.updateTodo(todo.copy(isCompleted = !todo.isCompleted))
    }

    suspend fun updateTodo(todo: TodoItem) = todoDao.updateTodo(todo)
    suspend fun deleteTodo(todo: TodoItem) = todoDao.deleteTodo(todo)
    suspend fun deleteCompletedTodos() = todoDao.deleteCompletedTodos()
}
