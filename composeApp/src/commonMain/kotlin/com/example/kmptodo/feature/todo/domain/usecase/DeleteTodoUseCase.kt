package com.example.kmptodo.feature.todo.domain.usecase

import com.example.kmptodo.core.exception.toFailure
import com.example.kmptodo.core.result.AppResult
import com.example.kmptodo.feature.todo.domain.repository.TodoRepository

class DeleteTodoUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(id: Long): AppResult<Unit> = try {
        repository.deleteTodo(id)
        AppResult.Success(Unit)
    } catch (e: Exception) {
        AppResult.Error(e.toFailure())
    }
}
