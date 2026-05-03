package com.example.kmptodo.feature.todo.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.kmptodo.feature.todo.domain.entity.TodoItem
import com.example.kmptodo.feature.todo.presentation.effect.TodoEffect
import com.example.kmptodo.feature.todo.presentation.event.TodoEvent
import com.example.kmptodo.feature.todo.presentation.viewmodel.TodoViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * The list screen. Pulls the [TodoViewModel] from Koin (scoped to the current
 * NavBackStackEntry's ViewModelStore), collects its state, and renders.
 *
 * `onItemClick` is supplied by [com.example.kmptodo.navigation.AppNavHost] —
 * it pushes a `TodoDetailKey(todo)` onto the back stack.
 *
 * Refresh-on-resume: when the list becomes RESUMED again (after the user pops
 * back from the detail screen) we fire a [TodoEvent.Refresh]. This re-reads
 * from the repository's in-memory cache, picking up edits/deletes the detail
 * screen made. A skipped first-resume guard avoids double-refresh on initial
 * load (the ViewModel's `init` block already kicks off the first fetch).
 *
 * This couples the list screen to the navigation library's lifecycle. A purer
 * alternative is exposing a `Flow<List<TodoItem>>` from the repository and
 * collecting it in the ViewModel — kept out of scope here for clarity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    onItemClick: (TodoItem) -> Unit = {},
    viewModel: TodoViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackBarHost = remember { SnackbarHostState() }
    var showAddDialog: Boolean by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TodoEffect.ShowError -> snackBarHost.showSnackbar(effect.message)
                is TodoEffect.ShowSuccess -> snackBarHost.showSnackbar(effect.message)
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        var firstResume = true
        lifecycleOwner.lifecycle.currentStateFlow.collect { lifecycleState ->
            if (lifecycleState == Lifecycle.State.RESUMED) {
                if (firstResume) firstResume = false
                else viewModel.onEvent(TodoEvent.Refresh)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todos") },
                actions = {
                    IconButton(onClick = { viewModel.onEvent(TodoEvent.Refresh) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add todo")
            }
        },
        snackbarHost = { SnackbarHost(snackBarHost) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading && state.todos.isEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxSize().padding(16.dp).align(Alignment.TopCenter))
            }

            if (!state.isLoading && state.todos.isEmpty()) {
                EmptyView(
                    onAddClick = { showAddDialog = true },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (state.todos.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.todos, key = { it.id }) { todo ->
                        TodoItemCard(
                            todo = todo,
                            onClick = { onItemClick(todo) },
                            onToggle = { newCompleted ->
                                viewModel.onEvent(
                                    TodoEvent.ToggleCompleted(todo.id, todo.title, newCompleted)
                                )
                            },
                            onDelete = { viewModel.onEvent(TodoEvent.Delete(todo.id)) }
                        )
                    }
                }
            }

            if (state.isLoading && state.todos.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxSize().align(Alignment.TopCenter))
            }
        }
    }

    if (showAddDialog) {
        AddTodoDialog(
            isCreating = state.isCreating,
            onDismiss = { showAddDialog = false },
            onConfirm = { title ->
                viewModel.onEvent(TodoEvent.Create(title))
                showAddDialog = false
            }
        )
    }
}
