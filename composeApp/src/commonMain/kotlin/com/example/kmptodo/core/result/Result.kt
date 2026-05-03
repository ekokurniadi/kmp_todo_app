package com.example.kmptodo.core.result

import com.example.kmptodo.core.exception.Failure

/**
 * A typed result wrapper used across the domain and presentation layers.
 *
 * Use cases return [AppResult] so callers can pattern-match on success vs.
 * failure without try/catch. This is the same pattern shown in the PDF
 * (section "Error Handling") — a custom sealed class instead of [kotlin.Result]
 * because we want to carry our own [Failure] type with structured error info.
 */
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val failure: Failure) : AppResult<Nothing>()
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onError(action: (Failure) -> Unit): AppResult<T> {
    if (this is AppResult.Error) action(failure)
    return this
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Error -> this
}
