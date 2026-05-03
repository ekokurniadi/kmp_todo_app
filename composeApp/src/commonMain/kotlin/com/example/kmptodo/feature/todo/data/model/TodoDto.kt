package com.example.kmptodo.feature.todo.data.model

import kotlinx.serialization.Serializable

/**
 * Wire format used by the JSONPlaceholder API.
 * https://jsonplaceholder.typicode.com/todos
 *
 * DTOs are kept separate from the domain [com.example.kmptodo.feature.todo.domain.entity.TodoItem]
 * so the API can change without rippling into the domain/UI layers.
 */
@Serializable
data class TodoDto(
    val id: Long = 0,
    val title: String,
    val completed: Boolean = false,
    val userId: Long = 1
)
