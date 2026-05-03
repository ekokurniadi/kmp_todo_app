package com.example.kmptodo.navigation

import com.example.kmptodo.feature.todo.domain.entity.TodoItem
import kotlinx.serialization.Serializable

/**
 * Type-safe routes, modeled in the Navigation 3 style: a sealed `NavKey`
 * hierarchy with `@Serializable` data objects/classes that the navigation
 * library encodes into the back stack.
 *
 * We're currently on the JetBrains KMP fork
 * `org.jetbrains.androidx.navigation:navigation-compose` — Navigation 2.x
 * shaped to match the nav3 API surface. When `androidx.navigation3` ships
 * a KMP artifact, swapping to it should be near-mechanical: the `NavKey`
 * sealed interface and `@Serializable` route shape carry over directly.
 */
sealed interface NavKey

@Serializable
data object TodoListKey : NavKey

@Serializable
data class TodoDetailKey(val todo: TodoItem) : NavKey
