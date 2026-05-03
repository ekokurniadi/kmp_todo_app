package com.example.kmptodo.feature.todo.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kmptodo.core.result.AppResult
import com.example.kmptodo.feature.todo.domain.usecase.CreateTodoUseCase
import com.example.kmptodo.feature.todo.domain.usecase.DeleteTodoUseCase
import com.example.kmptodo.feature.todo.domain.usecase.GetTodosUseCase
import com.example.kmptodo.feature.todo.domain.usecase.UpdateTodoUseCase
import com.example.kmptodo.feature.todo.presentation.effect.TodoEffect
import com.example.kmptodo.feature.todo.presentation.event.TodoEvent
import com.example.kmptodo.feature.todo.presentation.state.TodoUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the [TodoUiState] and translates [TodoEvent]s into use case calls.
 *
 * Extends [androidx.lifecycle.ViewModel] from the JetBrains Compose
 * Multiplatform lifecycle library — same class as Android, but works
 * on iOS and Desktop too. This unlocks Koin's `viewModelOf` DSL and
 * `koinViewModel<TodoViewModel>()` in Compose, which handle scope
 * cleanup automatically.
 */
class TodoViewModel(
    private val getTodos: GetTodosUseCase,
    private val createTodo: CreateTodoUseCase,
    private val updateTodo: UpdateTodoUseCase,
    private val deleteTodo: DeleteTodoUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TodoUiState())
    val state: StateFlow<TodoUiState> = _state.asStateFlow()
    private val _effect = MutableSharedFlow<TodoEffect>()
    val effect = _effect

    init {
        onEvent(TodoEvent.Refresh)
    }

    fun onEvent(event: TodoEvent) {
        when (event) {
            TodoEvent.Refresh -> refresh()
            is TodoEvent.Create -> create(event.title)
            is TodoEvent.ToggleCompleted -> toggle(event.id, event.currentTitle, event.newCompleted)
            is TodoEvent.Delete -> delete(event.id)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = getTodos()) {
                is AppResult.Success -> _state.update {
                    it.copy(todos = result.data, isLoading = false)
                }
                is AppResult.Error -> {
                    _effect.emit(
                        TodoEffect.ShowError(result.failure.message)
                    )
                }
            }
        }
    }

    private fun create(title: String) {
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true) }
            when (val result = createTodo(title)) {
                is AppResult.Success -> _state.update {

                    _effect.emit(
                        TodoEffect.ShowSuccess("$title created")
                    )

                    it.copy(
                        todos = listOf(result.data) + it.todos,
                        isCreating = false
                    )


                }
                is AppResult.Error -> {
                    _effect.emit(
                        TodoEffect.ShowError(result.failure.message)
                    )
                }
            }
        }
    }

    private fun toggle(id: Long, currentTitle: String, newCompleted: Boolean) {
        viewModelScope.launch {
            // Optimistic update — flip the UI immediately, then reconcile.
            _state.update { ui ->
                ui.copy(
                    todos = ui.todos.map {
                        if (it.id == id) it.copy(completed = newCompleted) else it
                    }
                )
            }
            when (val result = updateTodo(id, currentTitle, newCompleted)) {
                is AppResult.Success -> _state.update { ui ->
                    ui.copy(
                        todos = ui.todos.map {
                            if (it.id == id) result.data else it
                        }
                    )
                }
                is AppResult.Error -> {
                    _effect.emit(
                        TodoEffect.ShowError(result.failure.message)
                    )
                }
            }
        }
    }

    private fun delete(id: Long) {
        viewModelScope.launch {
            val previous = _state.value.todos
            _state.update { it.copy(todos = it.todos.filterNot { todo -> todo.id == id }) }
            when (val result = deleteTodo(id)) {
                is AppResult.Success -> Unit
                is AppResult.Error -> {
                    _effect.emit(
                        TodoEffect.ShowError(result.failure.message)
                    )
                }
            }
        }
    }
}
