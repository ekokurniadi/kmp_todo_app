package com.example.kmptodo.core.exception

/**
 * Domain-level error type. Every layer below the UI should map raw exceptions
 * (IOException, HTTP errors, parsing errors, etc.) into one of these cases.
 *
 * The UI then renders a user-friendly message based on the [Failure] subtype
 * rather than inspecting raw exceptions.
 */
sealed class Failure {
    abstract val message: String

    data class Network(override val message: String = "Network connection failed") : Failure()
    data class Timeout(override val message: String = "Request timed out") : Failure()
    data class Server(val code: Int, override val message: String) : Failure()
    data class Validation(override val message: String) : Failure()
    data class Unknown(override val message: String = "An unexpected error occurred") : Failure()
}

/** Map any [Throwable] to a [Failure]. Extend this as you add new exception types. */
fun Throwable.toFailure(): Failure = when (this) {
    is io.ktor.client.plugins.HttpRequestTimeoutException ->
        Failure.Timeout()
    is io.ktor.client.plugins.ClientRequestException ->
        Failure.Server(response.status.value, message ?: "Client error")
    is io.ktor.client.plugins.ServerResponseException ->
        Failure.Server(response.status.value, message ?: "Server error")
    else ->
        Failure.Unknown(message ?: "Unknown error")
}
