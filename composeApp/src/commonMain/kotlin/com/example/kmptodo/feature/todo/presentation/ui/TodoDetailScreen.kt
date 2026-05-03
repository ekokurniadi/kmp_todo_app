package com.example.kmptodo.feature.todo.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kmptodo.feature.todo.domain.entity.TodoItem
import com.example.kmptodo.feature.todo.presentation.effect.TodoDetailEffect
import com.example.kmptodo.feature.todo.presentation.event.TodoDetailEvent
import com.example.kmptodo.feature.todo.presentation.viewmodel.TodoDetailViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Detail screen for a single [TodoItem]. View + inline-edit + delete.
 *
 * The [TodoDetailViewModel] is obtained from Koin with the route's [TodoItem]
 * passed as a runtime parameter — see `feature/todo/di/TodoModule.kt` for the
 * `viewModel { (todo) -> ... }` registration.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoDetailScreen(
    todo: TodoItem,
    onBack: () -> Unit,
    viewModel: TodoDetailViewModel = koinViewModel { parametersOf(todo) },
) {
    val state by viewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TodoDetailEffect.ShowError -> snackbarHost.showSnackbar(effect.message)
                is TodoDetailEffect.ShowSuccess -> snackbarHost.showSnackbar(effect.message)
                TodoDetailEffect.NavigateBack -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Todo #${state.todo.id}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(TodoDetailEvent.RequestDelete) },
                        enabled = !state.isSaving,
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete todo")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(
                value = state.draftTitle,
                onValueChange = { viewModel.onEvent(TodoDetailEvent.EditTitleChanged(it)) },
                label = { Text("Title") },
                singleLine = false,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Checkbox(
                    checked = state.todo.completed,
                    onCheckedChange = { viewModel.onEvent(TodoDetailEvent.ToggleCompleted) },
                    enabled = !state.isSaving,
                )
                Text(
                    text = if (state.todo.completed) "Completed" else "Not completed",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Text(
                text = "User #${state.todo.userId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { viewModel.onEvent(TodoDetailEvent.Save) },
                enabled = state.hasUnsavedChanges && !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.hasUnsavedChanges) "Save changes" else "No changes")
            }
        }
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(TodoDetailEvent.CancelDelete) },
            title = { Text("Delete this todo?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(TodoDetailEvent.ConfirmDelete) }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(TodoDetailEvent.CancelDelete) }) {
                    Text("Cancel")
                }
            }
        )
    }
}
