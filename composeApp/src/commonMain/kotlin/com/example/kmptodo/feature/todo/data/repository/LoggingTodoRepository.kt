package com.example.kmptodo.feature.todo.data.repository

import com.example.kmptodo.feature.todo.domain.entity.TodoItem
import com.example.kmptodo.feature.todo.domain.repository.TodoRepository
import kotlin.time.TimeSource

/**
 * GoF Decorator pattern applied to [TodoRepository].
 *
 * Wraps a real [TodoRepository] (the `delegate`) and adds cross-cutting logging
 * — entry, success/failure, and latency — without consumers knowing. Use cases
 * still depend on the [TodoRepository] interface and Koin transparently injects
 * this decorator instead of the raw [TodoRepositoryImpl].
 *
 * Wiring: see `feature/todo/di/TodoModule.kt`. The real impl is registered with
 * a Koin qualifier `named("impl")`; this decorator is the unqualified default
 * binding for [TodoRepository].
 *
 * Logging uses `println` for cross-platform simplicity. In a production app,
 * swap for a multiplatform logger such as Napier or kermit.
 */
class LoggingTodoRepository(
    private val delegate: TodoRepository
) : TodoRepository {

    override suspend fun getTodos(): List<TodoItem> =
        log("getTodos") { delegate.getTodos() }

    override suspend fun createTodo(title: String): TodoItem =
        log("createTodo(title=\"$title\")") { delegate.createTodo(title) }

    override suspend fun updateTodo(
        id: Long,
        title: String,
        completed: Boolean
    ): TodoItem = log("updateTodo(id=$id, completed=$completed)") {
        delegate.updateTodo(id, title, completed)
    }

    override suspend fun deleteTodo(id: Long): Unit =
        log("deleteTodo(id=$id)") { delegate.deleteTodo(id) }

    private inline fun <T> log(call: String, block: () -> T): T {
        println("$TAG $call -> entering")
        val mark = TimeSource.Monotonic.markNow()
        return try {
            val result = block()
            println("$TAG $call -> success in ${mark.elapsedNow()}")
            result
        } catch (t: Throwable) {
            println("$TAG $call -> failure in ${mark.elapsedNow()}: ${t::class.simpleName}: ${t.message}")
            throw t
        }
    }

    private companion object {
        const val TAG = "[TodoRepository]"
    }
}
