package com.example.kmptodo.feature.todo.domain.entity

import kotlinx.serialization.Serializable

/**
 * Domain entity for a Todo. The data layer's
 * [com.example.kmptodo.feature.todo.data.model.TodoDto] is converted to this
 * via the mapper.
 *
 * `@Serializable` here is a deliberate trade-off: the navigation layer passes
 * a full `TodoItem` as a type-safe route argument, which requires the entity
 * to be serializable. The principled alternative would be a separate
 * `TodoNavArg` mirror in the navigation layer; we keep the simpler shape on
 * purpose for this reference project. See ARCHITECTURE.md > Navigation.
 */
@Serializable
data class TodoItem(
    val id: Long,
    val title: String,
    val completed: Boolean,
    val userId: Long
)
