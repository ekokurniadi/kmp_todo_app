package com.example.kmptodo.feature.todo.domain.usecase

import com.example.kmptodo.core.exception.toFailure
import com.example.kmptodo.core.result.AppResult
import com.example.kmptodo.feature.todo.domain.entity.TodoItem
import com.example.kmptodo.feature.todo.domain.repository.TodoRepository

class GetTodosUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(): AppResult<List<TodoItem>> = try {
        AppResult.Success(repository.getTodos())
    } catch (e: Exception) {
        AppResult.Error(e.toFailure())
    }
}
