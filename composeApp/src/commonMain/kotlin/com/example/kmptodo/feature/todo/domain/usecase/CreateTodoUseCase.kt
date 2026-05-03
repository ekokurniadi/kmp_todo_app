package com.example.kmptodo.feature.todo.domain.usecase

import com.example.kmptodo.core.exception.Failure
import com.example.kmptodo.core.exception.toFailure
import com.example.kmptodo.core.result.AppResult
import com.example.kmptodo.feature.todo.domain.entity.TodoItem
import com.example.kmptodo.feature.todo.domain.repository.TodoRepository

class CreateTodoUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(title: String): AppResult<TodoItem> {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            return AppResult.Error(Failure.Validation("Title cannot be empty"))
        }
        return try {
            AppResult.Success(repository.createTodo(trimmed))
        } catch (e: Exception) {
            AppResult.Error(e.toFailure())
        }
    }
}
