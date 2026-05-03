package com.example.kmptodo.core.di

import com.example.kmptodo.core.network.createHttpClient
import io.ktor.client.HttpClient
import org.koin.core.annotation.Module
import org.koin.core.annotation.Single

/**
 * Cross-cutting infrastructure dependencies. Right now this is just the
 * [io.ktor.client.HttpClient]. Add singletons here for things like
 * Settings/DataStore wrappers, clocks, loggers, etc.
 *
 * `@Module` tells the Koin compiler (KSP) to generate a Koin `Module` that
 * collects every `@Single`/`@Factory`/`@KoinViewModel` declaration on this
 * class. The generated extension is consumed in `di/Initializer.kt` via
 * `org.koin.ksp.generated.module`.
 */
@Module
class CoreModule {

    @Single
    fun httpClient(): HttpClient = createHttpClient()
}
