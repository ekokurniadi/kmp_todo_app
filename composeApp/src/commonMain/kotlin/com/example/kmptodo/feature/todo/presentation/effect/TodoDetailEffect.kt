package com.example.kmptodo.feature.todo.presentation.effect

sealed class TodoDetailEffect {
    data class ShowError(val message: String) : TodoDetailEffect()
    data class ShowSuccess(val message: String) : TodoDetailEffect()
    data object NavigateBack : TodoDetailEffect()
}
