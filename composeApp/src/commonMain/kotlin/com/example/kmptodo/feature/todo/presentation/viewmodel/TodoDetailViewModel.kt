package com.example.kmptodo.feature.todo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kmptodo.core.result.AppResult
import com.example.kmptodo.feature.todo.domain.entity.TodoItem
import com.example.kmptodo.feature.todo.domain.usecase.DeleteTodoUseCase
import com.example.kmptodo.feature.todo.domain.usecase.UpdateTodoUseCase
import com.example.kmptodo.feature.todo.presentation.effect.TodoDetailEffect
import com.example.kmptodo.feature.todo.presentation.event.TodoDetailEvent
import com.example.kmptodo.feature.todo.presentation.state.TodoDetailUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the detail screen's [TodoDetailUiState].
 *
 * `initialTodo` is supplied by the navigation layer via Koin parametric
 * injection — see `feature/todo/di/TodoModule.kt`. The ViewModel never
 * touches the back stack itself; instead it emits a [TodoDetailEffect.NavigateBack]
 * effect when the screen should pop, and the composable wires that to the
 * actual NavController.
 */
class TodoDetailViewModel(
    initialTodo: TodoItem,
    private val updateTodo: UpdateTodoUseCase,
    private val deleteTodo: DeleteTodoUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(TodoDetailUiState(todo = initialTodo))
    val state: StateFlow<TodoDetailUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<TodoDetailEffect>()
    val effect = _effect

    fun onEvent(event: TodoDetailEvent) {
        when (event) {
            is TodoDetailEvent.EditTitleChanged -> _state.update { it.copy(draftTitle = event.text) }
            TodoDetailEvent.ToggleCompleted -> toggleCompleted()
            TodoDetailEvent.Save -> save()
            TodoDetailEvent.RequestDelete -> _state.update { it.copy(showDeleteConfirm = true) }
            TodoDetailEvent.CancelDelete -> _state.update { it.copy(showDeleteConfirm = false) }
            TodoDetailEvent.ConfirmDelete -> confirmDelete()
        }
    }

    private fun toggleCompleted() {
        val current = _state.value.todo
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            when (val result = updateTodo(current.id, current.title, !current.completed)) {
                is AppResult.Success -> _state.update {
                    it.copy(todo = result.data, isSaving = false)
                }
                is AppResult.Error -> {
                    _state.update { it.copy(isSaving = false) }
                    _effect.emit(TodoDetailEffect.ShowError(result.failure.message))
                }
            }
        }
    }

    private fun save() {
        val ui = _state.value
        if (!ui.hasUnsavedChanges) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            when (val result = updateTodo(ui.todo.id, ui.draftTitle, ui.todo.completed)) {
                is AppResult.Success -> {
                    _state.update {
                        it.copy(
                            todo = result.data,
                            draftTitle = result.data.title,
                            isSaving = false
                        )
                    }
                    _effect.emit(TodoDetailEffect.ShowSuccess("Saved"))
                }
                is AppResult.Error -> {
                    _state.update { it.copy(isSaving = false) }
                    _effect.emit(TodoDetailEffect.ShowError(result.failure.message))
                }
            }
        }
    }

    private fun confirmDelete() {
        val id = _state.value.todo.id
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, showDeleteConfirm = false) }
            when (val result = deleteTodo(id)) {
                is AppResult.Success -> {
                    _effect.emit(TodoDetailEffect.ShowSuccess("Deleted"))
                    _effect.emit(TodoDetailEffect.NavigateBack)
                }
                is AppResult.Error -> {
                    _state.update { it.copy(isSaving = false) }
                    _effect.emit(TodoDetailEffect.ShowError(result.failure.message))
                }
            }
        }
    }
}
