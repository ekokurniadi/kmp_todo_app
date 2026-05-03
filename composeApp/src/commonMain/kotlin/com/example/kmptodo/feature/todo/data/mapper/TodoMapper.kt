package com.example.kmptodo.feature.todo.data.mapper

import com.example.kmptodo.feature.todo.data.model.TodoDto
import com.example.kmptodo.feature.todo.domain.entity.TodoItem

fun TodoDto.toDomain(): TodoItem = TodoItem(
    id = id,
    title = title,
    completed = completed,
    userId = userId
)

fun TodoItem.toDto(): TodoDto = TodoDto(
    id = id,
    title = title,
    completed = completed,
    userId = userId
)
