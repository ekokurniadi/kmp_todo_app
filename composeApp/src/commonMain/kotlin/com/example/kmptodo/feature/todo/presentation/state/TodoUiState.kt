package com.example.kmptodo.feature.todo.presentation.state

import com.example.kmptodo.core.exception.Failure
import com.example.kmptodo.feature.todo.domain.entity.TodoItem

/**
 * Single immutable state class observed by the UI.
 *
 * Rather than separate Loading/Success/Error sealed cases, we use one
 * data class so that errors and loading indicators can overlay existing
 * content (e.g. show a snackbar error while still displaying the cached
 * list). [error] is null when there's nothing to show.
 */
data class TodoUiState(
    val todos: List<TodoItem> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
)
