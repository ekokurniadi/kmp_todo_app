package com.example.kmptodo.feature.todo.presentation.event

/**
 * User intents flowing from UI to ViewModel. Modeled as a sealed class so
 * the ViewModel's `when` block over the events is exhaustive — adding a new
 * event causes a compile error in any handler that hasn't been updated.
 *
 * This is the same MVI-style pattern the PDF describes in section
 * "State Management: BLoC vs Kotlin Approaches".
 */
sealed interface TodoEvent {
    data object Refresh : TodoEvent
    data class Create(val title: String) : TodoEvent
    data class ToggleCompleted(val id: Long, val currentTitle: String, val newCompleted: Boolean) : TodoEvent
    data class Delete(val id: Long) : TodoEvent
}
