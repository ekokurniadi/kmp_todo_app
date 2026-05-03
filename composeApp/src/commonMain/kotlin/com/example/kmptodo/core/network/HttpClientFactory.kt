package com.example.kmptodo.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Each platform supplies its own Ktor engine. Android uses OkHttp, iOS uses
 * Darwin (NSURLSession), Desktop JVM uses OkHttp. The [createPlatformEngine]
 * function is declared `expect` here and `actual` in each platform source set.
 */
expect fun createPlatformEngine(): HttpClientEngine

/**
 * Builds the shared [HttpClient] used by the data layer. Configured with:
 * - JSON (kotlinx.serialization) content negotiation
 * - Request/response logging
 * - 15-second timeout
 * - JSON content type by default
 *
 * The client is registered as a singleton in [com.example.kmptodo.core.di.coreModule].
 */
fun createHttpClient(): HttpClient = HttpClient(createPlatformEngine()) {
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
            }
        )
    }

    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                println("HTTP: $message")
            }
        }
        level = LogLevel.INFO
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 15_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 15_000
    }

    defaultRequest {
        contentType(ContentType.Application.Json)
    }
}
