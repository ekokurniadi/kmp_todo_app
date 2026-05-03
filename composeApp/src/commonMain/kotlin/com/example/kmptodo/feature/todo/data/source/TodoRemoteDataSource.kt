package com.example.kmptodo.feature.todo.data.source

import com.example.kmptodo.feature.todo.data.model.TodoDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

private const val BASE_URL = "https://jsonplaceholder.typicode.com"

/**
 * Thin wrapper around Ktor for the Todo endpoints. Returns DTOs only —
 * mapping to domain entities happens in [com.example.kmptodo.feature.todo.data.repository.TodoRepositoryImpl].
 *
 * NOTE on JSONPlaceholder: this is a fake API. POST/PUT/DELETE return
 * realistic responses (with ids and 200/201 status codes) but nothing
 * is actually persisted server-side. The repository keeps an in-memory
 * cache so the UI behaves as if the writes succeeded.
 */
class TodoRemoteDataSource(private val httpClient: HttpClient) {

    suspend fun getTodos(): List<TodoDto> =
        httpClient.get("$BASE_URL/todos").body()

    suspend fun createTodo(title: String): TodoDto =
        httpClient.post("$BASE_URL/todos") {
            setBody(TodoDto(title = title, completed = false, userId = 1))
        }.body()

    suspend fun updateTodo(id: Long, title: String, completed: Boolean): TodoDto =
        httpClient.put("$BASE_URL/todos/$id") {
            setBody(TodoDto(id = id, title = title, completed = completed, userId = 1))
        }.body()

    suspend fun deleteTodo(id: Long) {
        httpClient.delete("$BASE_URL/todos/$id")
    }
}
