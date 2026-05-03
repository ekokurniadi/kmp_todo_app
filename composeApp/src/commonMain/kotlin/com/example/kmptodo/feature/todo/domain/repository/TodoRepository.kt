package com.example.kmptodo.feature.todo.domain.repository

import com.example.kmptodo.feature.todo.domain.entity.TodoItem

/**
 * Repository contract owned by the domain layer. The data layer provides
 * the implementation. Use cases and ViewModels depend on this interface,
 * never on the concrete class — that's the Dependency Inversion principle
 * the PDF discusses in section "Clean Architecture".
 */
interface TodoRepository {
    suspend fun getTodos(): List<TodoItem>
    suspend fun createTodo(title: String): TodoItem
    suspend fun updateTodo(id: Long, title: String, completed: Boolean): TodoItem
    suspend fun deleteTodo(id: Long)
}
