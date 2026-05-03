package com.example.kmptodo.feature.todo.data.repository

import com.example.kmptodo.feature.todo.data.mapper.toDomain
import com.example.kmptodo.feature.todo.data.source.TodoRemoteDataSource
import com.example.kmptodo.feature.todo.domain.entity.TodoItem
import com.example.kmptodo.feature.todo.domain.repository.TodoRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Combines the remote data source with an in-memory cache.
 *
 * Why a cache? JSONPlaceholder is a mock API: POST/PUT/DELETE succeed
 * but don't persist server-side. The cache lets the UI display
 * locally-created/edited todos as if they were saved. In a real app,
 * this is also where you'd add a local database (SQLDelight/Room) and
 * implement an offline-first strategy.
 *
 * The [Mutex] guards concurrent reads/writes since multiple coroutines
 * can call into the repository at once.
 */
class TodoRepositoryImpl(
    private val remoteDataSource: TodoRemoteDataSource
) : TodoRepository {

    private val cache = mutableListOf<TodoItem>()
    private val mutex = Mutex()
    private var nextLocalId = 1_000_000L

    override suspend fun getTodos(): List<TodoItem> = mutex.withLock {
        if (cache.isEmpty()) {
            cache += remoteDataSource.getTodos().map { it.toDomain() }
        }
        cache.toList()
    }

    override suspend fun createTodo(title: String): TodoItem = mutex.withLock {
        // Hit the API for realism (returns id=201 always for JSONPlaceholder),
        // but use our own local id so multiple inserts don't collide.
        remoteDataSource.createTodo(title)
        val todo = TodoItem(
            id = nextLocalId++,
            title = title,
            completed = false,
            userId = 1
        )
        cache.add(0, todo)
        todo
    }

    override suspend fun updateTodo(
        id: Long,
        title: String,
        completed: Boolean
    ): TodoItem = mutex.withLock {
        // Only call the API for ids that came from the server (JSONPlaceholder
        // has 200 todos with ids 1..200; our local ids start at 1_000_000).
        if (id <= 200) {
            remoteDataSource.updateTodo(id, title, completed)
        }
        val updated = TodoItem(id = id, title = title, completed = completed, userId = 1)
        val index = cache.indexOfFirst { it.id == id }
        if (index >= 0) {
            cache[index] = updated
        }
        updated
    }

    override suspend fun deleteTodo(id: Long): Unit = mutex.withLock {
        if (id <= 200) {
            remoteDataSource.deleteTodo(id)
        }
        cache.removeAll { it.id == id }
    }
}
