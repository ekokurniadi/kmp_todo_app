package com.example.kmptodo.feature.todo.presentation.effect

sealed class TodoEffect {
    data class ShowError(val message: String): TodoEffect()
    data class ShowSuccess(val message: String): TodoEffect()
}
