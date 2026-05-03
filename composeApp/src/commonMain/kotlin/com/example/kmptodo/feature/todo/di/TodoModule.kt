package com.example.kmptodo.feature.todo.di

import com.example.kmptodo.feature.todo.data.repository.LoggingTodoRepository
import com.example.kmptodo.feature.todo.data.repository.TodoRepositoryImpl
import com.example.kmptodo.feature.todo.data.source.TodoRemoteDataSource
import com.example.kmptodo.feature.todo.domain.entity.TodoItem
import com.example.kmptodo.feature.todo.domain.repository.TodoRepository
import com.example.kmptodo.feature.todo.domain.usecase.CreateTodoUseCase
import com.example.kmptodo.feature.todo.domain.usecase.DeleteTodoUseCase
import com.example.kmptodo.feature.todo.domain.usecase.GetTodosUseCase
import com.example.kmptodo.feature.todo.domain.usecase.UpdateTodoUseCase
import com.example.kmptodo.feature.todo.presentation.viewmodel.TodoDetailViewModel
import com.example.kmptodo.feature.todo.presentation.viewmodel.TodoViewModel
import io.ktor.client.HttpClient
import org.koin.android.annotation.KoinViewModel
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * Todo feature module declared with Koin Annotations (`@Module` plus the
 * definition annotations `@Single` / `@Factory` / `@KoinViewModel`). KSP scans
 * this class at build time and generates a `TodoModule.module` extension we
 * consume from `di/Initializer.kt` via `org.koin.ksp.generated.module`.
 *
 * GoF Decorator wiring:
 *
 * - The real impl is registered behind the typed qualifier [RealRepo]
 *   (a `@Named`-meta-annotated annotation class).
 * - The unqualified `TodoRepository` binding is the [LoggingTodoRepository]
 *   decorator wrapping the real impl. Use cases ask for `TodoRepository` with
 *   no qualifier and transparently get the decorator.
 *
 * Note `@KoinViewModel` lives in `org.koin.android.annotation` even when used
 * from `commonMain` — that's the intended import for Koin Annotations 2.x.
 */
@Module
class TodoModule {

    @Single
    fun remoteDataSource(httpClient: HttpClient) = TodoRemoteDataSource(httpClient)

    // Real impl, named so the decorator can pull it without colliding with
    // the unqualified default binding.
    @Single
    @RealRepo
    fun todoRepositoryImpl(remote: TodoRemoteDataSource): TodoRepository =
        TodoRepositoryImpl(remote)

    // Default `TodoRepository` binding is the decorator. The function returns
    // the concrete `LoggingTodoRepository`; Koin auto-binds it to all
    // implemented interfaces, so consumers of `TodoRepository` resolve here.
    @Single
    fun loggingTodoRepository(@RealRepo delegate: TodoRepository): TodoRepository =
        LoggingTodoRepository(delegate)

    @Factory fun getTodos(repo: TodoRepository) = GetTodosUseCase(repo)
    @Factory fun createTodo(repo: TodoRepository) = CreateTodoUseCase(repo)
    @Factory fun updateTodo(repo: TodoRepository) = UpdateTodoUseCase(repo)
    @Factory fun deleteTodo(repo: TodoRepository) = DeleteTodoUseCase(repo)

    @KoinViewModel
    fun todoViewModel(
        getTodos: GetTodosUseCase,
        createTodo: CreateTodoUseCase,
        updateTodo: UpdateTodoUseCase,
        deleteTodo: DeleteTodoUseCase,
    ) = TodoViewModel(getTodos, createTodo, updateTodo, deleteTodo)

    // Parametric ViewModel: the navigation layer hands us a TodoItem at
    // resolution time via `koinViewModel { parametersOf(todo) }` from the
    // composable.
    @KoinViewModel
    fun todoDetailViewModel(
        @InjectedParam initialTodo: TodoItem,
        updateTodo: UpdateTodoUseCase,
        deleteTodo: DeleteTodoUseCase,
    ) = TodoDetailViewModel(initialTodo, updateTodo, deleteTodo)
}

/** Typed Koin qualifier for the real `TodoRepositoryImpl`. */
@Named
annotation class RealRepo
