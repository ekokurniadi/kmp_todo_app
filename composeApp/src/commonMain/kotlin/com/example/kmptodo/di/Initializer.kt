package com.example.kmptodo.di

import com.example.kmptodo.core.di.CoreModule
import com.example.kmptodo.feature.todo.di.TodoModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module

/**
 * One entry point for Koin initialization, called from each platform's
 * launcher (Android Application, iOS MainViewController, Desktop main).
 * The `appDeclaration` lambda lets the Android side inject `androidContext`.
 *
 * `CoreModule().module` is the KSP-generated extension property (in package
 * `org.koin.ksp.generated`) that materializes the `@Module`-annotated class
 * into a real Koin `Module`.
 */
fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}) {
    startKoin {
        appDeclaration()
        modules(
            CoreModule().module,
            TodoModule().module,
        )
    }
}
