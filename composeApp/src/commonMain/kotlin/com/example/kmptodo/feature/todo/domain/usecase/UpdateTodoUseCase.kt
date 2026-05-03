package com.example.kmptodo.feature.todo.domain.usecase

import com.example.kmptodo.core.exception.toFailure
import com.example.kmptodo.core.result.AppResult
import com.example.kmptodo.feature.todo.domain.entity.TodoItem
import com.example.kmptodo.feature.todo.domain.repository.TodoRepository

class UpdateTodoUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(
        id: Long,
        title: String,
        completed: Boolean
    ): AppResult<TodoItem> = try {
        AppResult.Success(repository.updateTodo(id, title, completed))
    } catch (e: Exception) {
        AppResult.Error(e.toFailure())
    }
}
