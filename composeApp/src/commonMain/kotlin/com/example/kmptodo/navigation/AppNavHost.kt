package com.example.kmptodo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.kmptodo.feature.todo.presentation.ui.TodoDetailScreen
import com.example.kmptodo.feature.todo.presentation.ui.TodoListScreen

/**
 * Root navigation host. Hosts a [NavHostController] back stack and dispatches
 * each [NavKey] to its screen.
 *
 * Shape on purpose: sealed `NavKey` + `@Serializable` route arguments + one
 * `composable<KeyType>` entry per destination. This mirrors the nav3 API so
 * future migration is mechanical. See `navigation/NavKeys.kt` for the migration
 * note.
 *
 * Adding a new destination:
 *   1. Add a new entry to the `NavKey` sealed interface in `NavKeys.kt`.
 *   2. Add a `composable<NewKey> { ... }` block here.
 *   3. From the calling screen, push it via `navController.navigate(NewKey(...))`.
 */
// TODO: migrate to androidx.navigation3 once a KMP artifact ships.
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = TodoListKey,
    ) {
        composable<TodoListKey> {
            TodoListScreen(
                onItemClick = { todo -> navController.navigate(TodoDetailKey(todo)) },
            )
        }
        composable<TodoDetailKey> { entry ->
            val key = entry.toRoute<TodoDetailKey>()
            TodoDetailScreen(
                todo = key.todo,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
