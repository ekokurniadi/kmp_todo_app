# KMP Todo App

A Kotlin Multiplatform + Compose Multiplatform sample that runs the **same UI code** on Android, iOS, and Desktop. Built as a learning reference following the Clean Architecture patterns from the *Kotlin Multiplatform Developer Guideline* PDF and the [official JetBrains tutorial](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-create-first-app.html).

## What's inside

- **One feature, two screens** — Todo CRUD against [JSONPlaceholder](https://jsonplaceholder.typicode.com/) with a list screen and a detail screen (view + inline edit + delete), wired through Clean Architecture (domain / data / presentation layers).
- **Three platforms** — Android, iOS, JVM Desktop.
- **Shared UI** — every Composable lives in `commonMain`. Each platform launcher is ~10 lines of code.
- **Type-safe navigation** — sealed `NavKey` + `@Serializable` route arguments, shaped in the Navigation 3 style on top of the JetBrains `navigation-compose` KMP fork.
- **Decorator pattern in DI** — a `LoggingTodoRepository` transparently wraps the real `TodoRepositoryImpl` via Koin qualifiers; consumers see only the `TodoRepository` interface.
- **Annotation-based DI** — modules are declared with `@Module` classes and `@Single` / `@Factory` / `@KoinViewModel` / `@Named` annotations; KSP generates the Koin module at build time. No manual `module { }` DSL.

## Tech stack

| Concern              | Library                                              |
|----------------------|------------------------------------------------------|
| UI                   | Compose Multiplatform 1.7                            |
| Navigation           | `org.jetbrains.androidx.navigation:navigation-compose` (Navigation-3-style API: sealed `NavKey` + `@Serializable` routes) |
| State                | `kotlinx.coroutines` `StateFlow` + `androidx.lifecycle.ViewModel` (KMP fork) |
| DI                   | Koin 4.0.4 + Koin Annotations 2.0.1 (`@Module`, `@Single`, `@Factory`, `@KoinViewModel`, `@Named`) generated via KSP. `TodoRepository` wired via the GoF Decorator pattern |
| Networking           | Ktor 2.3 (OkHttp on Android/Desktop, Darwin on iOS)  |
| Serialization        | `kotlinx.serialization`                              |
| Build                | Gradle 8.7, Kotlin 2.0.21, AGP 8.6                   |

## Documentation

- [GETTING_STARTED.md](GETTING_STARTED.md) — how to run on each platform.
- [ARCHITECTURE.md](ARCHITECTURE.md) — file-by-file walkthrough of every layer and *why* it's structured that way.

## Project layout

```
kmp_todo_app/
├── composeApp/                     # The single KMP module — everything lives here
│   ├── src/
│   │   ├── commonMain/             # Shared UI + business logic + DI wiring
│   │   │   └── kotlin/com/example/kmptodo/
│   │   │       ├── App.kt          # Root composable — hosts AppNavHost
│   │   │       ├── core/           # Cross-cutting: HTTP client, AppResult, Failure
│   │   │       ├── di/             # initKoin + module aggregation
│   │   │       ├── feature/todo/   # The Todo feature (domain / data / presentation)
│   │   │       └── navigation/     # NavKey sealed interface + AppNavHost
│   │   ├── androidMain/            # MainActivity, Application, OkHttp engine
│   │   ├── iosMain/                # MainViewController, Darwin engine
│   │   └── jvmMain/                # Desktop main(), OkHttp engine
│   └── build.gradle.kts
├── iosApp/                         # SwiftUI shell that hosts the Compose UI
│   ├── iosApp/
│   │   ├── iOSApp.swift
│   │   └── ContentView.swift       # 4-line UIViewControllerRepresentable
│   └── iosApp.xcodeproj
├── gradle/libs.versions.toml       # Single source of truth for versions
└── settings.gradle.kts
```

This matches the structure JetBrains' [KMP Wizard](https://kmp.jetbrains.com/) generates.
