package com.example.kmptodo.feature.todo.presentation.state

import com.example.kmptodo.feature.todo.domain.entity.TodoItem

/**
 * UI state for the detail screen.
 *
 * `todo` is the persisted source of truth (last value confirmed by the
 * repository). `draftTitle` is the editable buffer so the user can type
 * without each keystroke racing to the network. `Save` flushes draftTitle
 * back into the repository.
 */
data class TodoDetailUiState(
    val todo: TodoItem,
    val draftTitle: String = todo.title,
    val isSaving: Boolean = false,
    val showDeleteConfirm: Boolean = false,
) {
    val hasUnsavedChanges: Boolean get() = draftTitle != todo.title
}
