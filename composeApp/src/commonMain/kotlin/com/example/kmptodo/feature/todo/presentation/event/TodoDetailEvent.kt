package com.example.kmptodo.feature.todo.presentation.event

sealed interface TodoDetailEvent {
    data class EditTitleChanged(val text: String) : TodoDetailEvent
    data object ToggleCompleted : TodoDetailEvent
    data object Save : TodoDetailEvent
    data object RequestDelete : TodoDetailEvent
    data object ConfirmDelete : TodoDetailEvent
    data object CancelDelete : TodoDetailEvent
}
