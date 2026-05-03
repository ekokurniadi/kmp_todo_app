# Architecture Walkthrough

This document goes file-by-file so you understand *why* each piece exists. Read it top-to-bottom on your first pass — later you can jump straight to a section.

> Coming from Flutter? The PDF in `/Users/ekokurniadi/Documents/kmp/Kotlin Multiplatform Developer Guideline.pdf` has a "Dart vs. Kotlin" cheat sheet for every concept here.

---

## 1. The big picture

```
                ┌─────────────────────────────────┐
                │              App()              │   commonMain
                │       MaterialTheme + Surface   │
                └──────────────┬──────────────────┘
                               │  hosts
                ┌──────────────▼──────────────────┐
                │          AppNavHost             │   commonMain (navigation)
                │   NavController + NavKey routes │
                └──────┬──────────────────┬───────┘
                       │ TodoListKey      │ TodoDetailKey(todo)
            ┌──────────▼─────────┐   ┌────▼─────────────────┐
            │   TodoListScreen   │   │   TodoDetailScreen   │   commonMain
            │   TodoItemCard     │   │   (view + edit + del)│
            └──────────┬─────────┘   └─────────┬────────────┘
                       │                       │
            ┌──────────▼─────────┐   ┌─────────▼────────────┐
            │   TodoViewModel    │   │  TodoDetailViewModel │   commonMain
            │ (StateFlow<UiState>)│   │ (parametric: TodoItem)│
            └──────────┬─────────┘   └─────────┬────────────┘
                       └───────────┬───────────┘
                                   │  use case calls
                ┌──────────────────▼──────────────┐
                │           Use Cases             │   commonMain
                │  GetTodos / Create / Update / Delete
                └──────────────┬──────────────────┘
                               │  repository interface
                ┌──────────────▼──────────────────┐
                │   Repository (interface)        │   commonMain (domain)
                └──────────────┬──────────────────┘
                               │  Koin injects the decorator chain
                ┌──────────────▼──────────────────┐
                │  LoggingTodoRepository          │   ← decorator (default binding)
                │  delegates to ─────────────────┐│
                └────────────────────────────────┼┘
                                                 │
                ┌────────────────────────────────▼┐
                │   TodoRepositoryImpl + cache    │   ← named("impl") in Koin
                │   TodoRemoteDataSource          │
                │   TodoMapper (DTO ↔ Entity)     │
                └──────────────┬──────────────────┘
                               │  HttpClient
                ┌──────────────▼──────────────────┐
                │  Ktor + platform engine         │   commonMain + expect/actual
                │  OkHttp on Android/Desktop      │   androidMain / jvmMain
                │  Darwin on iOS                  │   iosMain
                └─────────────────────────────────┘
```

Every arrow points at an *interface or contract*, never at a concrete class — that's the Dependency Inversion Principle. It's why the ViewModel can be unit-tested by passing fake use cases, and the use cases can be tested by passing a fake `TodoRepository`. It's also why dropping in a logging decorator at the DI layer required zero changes to any consumer.

---

## 2. Project layout

```
composeApp/src/
├── commonMain/kotlin/com/example/kmptodo/
│   ├── App.kt                                  ← root @Composable, hosts AppNavHost
│   ├── core/                                   ← cross-cutting infrastructure
│   │   ├── result/Result.kt                    ← AppResult<T> sealed class
│   │   ├── exception/Failure.kt                ← Failure types + Throwable.toFailure()
│   │   ├── network/HttpClientFactory.kt        ← expect createPlatformEngine()
│   │   └── di/CoreModule.kt                    ← Koin module: HttpClient
│   ├── navigation/                             ← Navigation-3-style nav layer
│   │   ├── NavKeys.kt                          ← sealed NavKey + @Serializable routes
│   │   └── AppNavHost.kt                       ← NavController + composable<KeyType> entries
│   ├── feature/todo/                           ← the only feature
│   │   ├── domain/
│   │   │   ├── entity/TodoItem.kt              ← @Serializable domain model (route arg)
│   │   │   ├── repository/TodoRepository.kt    ← interface
│   │   │   └── usecase/                        ← four single-purpose use cases
│   │   ├── data/
│   │   │   ├── model/TodoDto.kt                ← @Serializable wire model
│   │   │   ├── source/TodoRemoteDataSource.kt  ← Ktor calls
│   │   │   ├── mapper/TodoMapper.kt            ← extension functions
│   │   │   ├── repository/TodoRepositoryImpl.kt    ← cache + mapper + remote
│   │   │   └── repository/LoggingTodoRepository.kt ← GoF decorator over TodoRepository
│   │   ├── presentation/
│   │   │   ├── event/TodoEvent.kt              ← user intents (list)
│   │   │   ├── event/TodoDetailEvent.kt        ← user intents (detail)
│   │   │   ├── state/TodoUiState.kt            ← single immutable state class (list)
│   │   │   ├── state/TodoDetailUiState.kt      ← detail state with draftTitle buffer
│   │   │   ├── effect/TodoEffect.kt            ← snackbar/error effects (list)
│   │   │   ├── effect/TodoDetailEffect.kt      ← snackbar + NavigateBack (detail)
│   │   │   ├── viewmodel/TodoViewModel.kt      ← orchestrates list events
│   │   │   ├── viewmodel/TodoDetailViewModel.kt← orchestrates detail events
│   │   │   └── ui/                             ← Composables
│   │   │       ├── TodoListScreen.kt
│   │   │       ├── TodoDetailScreen.kt
│   │   │       ├── TodoItemCard.kt
│   │   │       ├── AddTodoDialog.kt
│   │   │       └── EmptyView.kt
│   │   └── di/TodoModule.kt                    ← Koin: usecases, repo + decorator, viewmodels
│   └── di/Initializer.kt                       ← initKoin()
├── androidMain/                                ← Android entry + OkHttp engine
│   ├── AndroidManifest.xml
│   └── kotlin/com/example/kmptodo/
│       ├── KmpTodoApplication.kt               ← calls initKoin
│       ├── MainActivity.kt                     ← setContent { App() }
│       └── core/network/HttpClientEngine.android.kt
├── iosMain/                                    ← iOS entry + Darwin engine
│   └── kotlin/com/example/kmptodo/
│       ├── MainViewController.kt               ← exposed to Swift
│       └── core/network/HttpClientEngine.ios.kt
└── jvmMain/                                    ← Desktop entry + OkHttp engine
    └── kotlin/com/example/kmptodo/
        ├── desktop/Main.kt                     ← Compose Desktop application{}
        └── core/network/HttpClientEngine.desktop.kt
```

---

## 3. Layer-by-layer walkthrough

### 3.1 Core (`commonMain/.../core`)

#### `core/result/Result.kt`

```kotlin
sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val failure: Failure) : AppResult<Nothing>()
}
```

Every use case returns `AppResult<T>`. The UI layer pattern-matches on the result instead of catching exceptions. We use a custom sealed class instead of Kotlin's built-in `Result<T>` because we want to carry our own structured `Failure` type rather than a raw `Throwable`.

The file also has helper extensions (`onSuccess`, `onError`, `map`) so callers can chain transformations.

#### `core/exception/Failure.kt`

```kotlin
sealed class Failure {
    abstract val message: String
    data class Network(...)
    data class Timeout(...)
    data class Server(val code: Int, ...)
    data class Validation(...)
    data class Unknown(...)
}

fun Throwable.toFailure(): Failure = when (this) { ... }
```

The UI never sees a raw Ktor exception. Wherever we'd `catch (e: Exception)` in a use case, we call `e.toFailure()` to translate it into one of the structured cases. This means error handling in the ViewModel/UI is a `when` on a sealed class — exhaustive and easy to reason about.

#### `core/network/HttpClientFactory.kt`

```kotlin
expect fun createPlatformEngine(): HttpClientEngine

fun createHttpClient(): HttpClient = HttpClient(createPlatformEngine()) {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    install(Logging) { ... }
    install(HttpTimeout) { requestTimeoutMillis = 15_000 }
}
```

The `expect` function declares "every platform must provide an `HttpClientEngine`". The actual implementations live in:

- `androidMain/.../HttpClientEngine.android.kt` → `OkHttp.create()`
- `iosMain/.../HttpClientEngine.ios.kt` → `Darwin.create()` (uses NSURLSession)
- `jvmMain/.../HttpClientEngine.desktop.kt` → `OkHttp.create()`

This is the pattern from the PDF's "Platform-Specific Code" section: shared logic in `commonMain`, platform-specific implementations via `expect/actual`.

#### `core/di/CoreModule.kt`

```kotlin
val coreModule = module {
    single { createHttpClient() }
}
```

Tiny module that registers the `HttpClient` as a singleton. Combined with `todoModule` inside `Initializer.kt`.

---

### 3.2 Domain (`commonMain/.../feature/todo/domain`)

#### `domain/entity/TodoItem.kt`

```kotlin
@Serializable
data class TodoItem(
    val id: Long,
    val title: String,
    val completed: Boolean,
    val userId: Long
)
```

`@Serializable` here is a deliberate trade-off. The navigation layer passes a full `TodoItem` as a type-safe route argument (see §3.7), and `kotlinx.serialization` needs the entity to be serializable to encode it into the back stack. The principled alternative is a separate `TodoNavArg(id, title, completed, userId)` mirror in the navigation package — that keeps the domain entity framework-free at the cost of one more class. We picked the simpler shape on purpose for this reference project; copy the principled variant if your team treats abstraction-leak as load-bearing.

The DTO in the data layer (`TodoDto`) is still a separate `@Serializable` type because its field shape is dictated by the JSONPlaceholder API, not the domain. The mapper converts between the two.

#### `domain/repository/TodoRepository.kt`

```kotlin
interface TodoRepository {
    suspend fun getTodos(): List<TodoItem>
    suspend fun createTodo(title: String): TodoItem
    suspend fun updateTodo(id: Long, title: String, completed: Boolean): TodoItem
    suspend fun deleteTodo(id: Long)
}
```

Just a contract. The data layer provides the implementation. Use cases depend on this interface, never the concrete class — that's the Dependency Inversion Principle the PDF describes in section "Clean Architecture & Design Patterns".

#### `domain/usecase/*`

Four single-purpose classes: `GetTodosUseCase`, `CreateTodoUseCase`, `UpdateTodoUseCase`, `DeleteTodoUseCase`.

Each one has the same shape:

```kotlin
class GetTodosUseCase(private val repository: TodoRepository) {
    suspend operator fun invoke(): AppResult<List<TodoItem>> = try {
        AppResult.Success(repository.getTodos())
    } catch (e: Exception) {
        AppResult.Error(e.toFailure())
    }
}
```

Why so many tiny classes?

1. **One reason to change** — the Single Responsibility Principle.
2. **Validation lives here, not in the ViewModel**. Look at `CreateTodoUseCase`: it rejects empty titles by returning `Failure.Validation`.
3. **Testable in isolation** — pass a fake `TodoRepository` and assert on the `AppResult`.
4. **`operator fun invoke`** — lets you call `getTodos()` instead of `getTodos.execute()`. Reads naturally.

---

### 3.3 Data (`commonMain/.../feature/todo/data`)

#### `data/model/TodoDto.kt`

```kotlin
@Serializable
data class TodoDto(
    val id: Long = 0,
    val title: String,
    val completed: Boolean = false,
    val userId: Long = 1
)
```

This shape is dictated by the [JSONPlaceholder API](https://jsonplaceholder.typicode.com/todos). The DTO is annotated with `@Serializable` so kotlinx.serialization can decode JSON automatically. The defaults handle missing fields gracefully.

#### `data/source/TodoRemoteDataSource.kt`

```kotlin
class TodoRemoteDataSource(private val httpClient: HttpClient) {
    suspend fun getTodos(): List<TodoDto> =
        httpClient.get("$BASE_URL/todos").body()
    // ... post / put / delete
}
```

Thin Ktor wrapper. Returns DTOs only — never domain entities. Mapping happens one layer up in the repository.

#### `data/mapper/TodoMapper.kt`

```kotlin
fun TodoDto.toDomain(): TodoItem = TodoItem(...)
fun TodoItem.toDto(): TodoDto = TodoDto(...)
```

Extension functions for the conversions. Keeping them as extensions (instead of methods on the DTO/Entity) keeps the data classes clean and means the DTO doesn't have to know about the domain model.

#### `data/repository/TodoRepositoryImpl.kt`

This is the meatiest data-layer file. It implements the domain `TodoRepository` interface and combines:

- `TodoRemoteDataSource` for the actual HTTP calls
- An in-memory `cache: MutableList<TodoItem>` so the UI behaves correctly even though JSONPlaceholder doesn't actually persist writes
- A `Mutex` so concurrent reads/writes don't corrupt the cache
- A local-id counter so multiple new todos don't collide on JSONPlaceholder's hard-coded `id=201`

In a real app, the cache would be a SQLDelight or Room database, and you'd implement an offline-first strategy (read from DB → background-refresh from network → emit again). The structure here is the same; only the cache backing changes.

---

### 3.4 Presentation (`commonMain/.../feature/todo/presentation`)

#### `presentation/event/TodoEvent.kt`

```kotlin
sealed interface TodoEvent {
    data object Refresh : TodoEvent
    data class Create(val title: String) : TodoEvent
    data class ToggleCompleted(val id: Long, val currentTitle: String, val newCompleted: Boolean) : TodoEvent
    data class Delete(val id: Long) : TodoEvent
    data object DismissError : TodoEvent
}
```

User intents flowing from the UI to the ViewModel. Modeled as a sealed interface so the `when` block in `onEvent` is **exhaustive** — adding a new event causes a compile error in any handler that hasn't been updated.

#### `presentation/state/TodoUiState.kt`

```kotlin
data class TodoUiState(
    val todos: List<TodoItem> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: Failure? = null
)
```

A **single immutable state class** instead of separate Loading/Success/Error sealed cases. This is deliberate: we want errors and loading indicators to *overlay* existing content (e.g., show a snackbar error while still displaying the cached list).

`error` is nullable — when null, there's nothing to show.

#### `presentation/viewmodel/TodoViewModel.kt`

```kotlin
class TodoViewModel(
    private val getTodos: GetTodosUseCase,
    private val createTodo: CreateTodoUseCase,
    private val updateTodo: UpdateTodoUseCase,
    private val deleteTodo: DeleteTodoUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TodoUiState())
    val state: StateFlow<TodoUiState> = _state.asStateFlow()

    fun onEvent(event: TodoEvent) { ... }
}
```

Three things to notice:

1. **Extends `androidx.lifecycle.ViewModel`** (the JetBrains KMP fork: `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel`). That class works on Android, iOS, and Desktop — `viewModelScope` is provided automatically and gets cancelled when the ViewModel is no longer needed.

2. **All inputs come through one `onEvent(TodoEvent)` function**. The UI never calls private methods. This makes it trivial to add logging, analytics, or undo/redo — wrap or intercept `onEvent`.

3. **Optimistic updates** in `toggle()` and `delete()`: we mutate the state immediately, then reconcile with the server response. On failure we roll back. This is what makes the UI feel snappy.

#### `presentation/ui/TodoListScreen.kt`

```kotlin
@Composable
fun TodoListScreen(
    onItemClick: (TodoItem) -> Unit = {},
    viewModel: TodoViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    ...
}
```

`koinViewModel()` retrieves the ViewModel scoped to the current `ViewModelStoreOwner` — which inside the `composable<TodoListKey>` block is the `NavBackStackEntry`. So the ViewModel survives navigating to the detail screen and back: the same instance is re-attached when the list resumes.

`onItemClick` is supplied by `AppNavHost` and pushes a `TodoDetailKey(item)` onto the back stack. Defaulting it to `{}` keeps the screen usable in previews/tests without a NavController.

`collectAsState()` turns the `StateFlow` into a Compose `State<TodoUiState>` — every time the flow emits, the composables that read `state` recompose.

The rest of the file is straight Material 3 layout: `Scaffold` → `TopAppBar` + `FloatingActionButton` + `LazyColumn` of `TodoItemCard`s. No platform-specific code anywhere.

#### `presentation/ui/TodoDetailScreen.kt`

```kotlin
@Composable
fun TodoDetailScreen(
    todo: TodoItem,
    onBack: () -> Unit,
    viewModel: TodoDetailViewModel = koinViewModel { parametersOf(todo) },
) { ... }
```

The detail screen mirrors the list-screen pattern but with two extras:

1. **Parametric ViewModel injection.** The route arg (a fully-deserialized `TodoItem`) is passed via `parametersOf(todo)`. Koin matches it against the `viewModel { (todo: TodoItem) -> ... }` registration and constructs the ViewModel with the right initial state.
2. **`NavigateBack` effect.** Deletion isn't a state change — it's a transition. The ViewModel emits `TodoDetailEffect.NavigateBack` after a successful delete; the composable collects it and calls `onBack()`. That keeps navigation policy (when to pop) inside the ViewModel without making the ViewModel depend on a NavController.

The screen has an editable `OutlinedTextField` for the title (with a `draftTitle` buffer in state, so each keystroke isn't a network call), a `Checkbox` for `completed` that updates immediately, a Save button enabled only when there are unsaved changes, and a Delete button that opens an `AlertDialog` confirmation.

---

### 3.5 DI (`commonMain/.../feature/todo/di` and `commonMain/.../di`)

#### `feature/todo/di/TodoModule.kt`

DI is declared with **Koin Annotations**: a `@Module`-annotated class whose member functions are tagged `@Single`, `@Factory`, or `@KoinViewModel`. KSP scans these classes at build time and generates a `TodoModule().module` extension property in package `org.koin.ksp.generated` that we feed to `startKoin`.

```kotlin
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

    // Default `TodoRepository` binding is the decorator. Koin auto-binds the
    // function's return type to every interface it implements, so consumers
    // of `TodoRepository` resolve here.
    @Single
    fun loggingTodoRepository(@RealRepo delegate: TodoRepository): TodoRepository =
        LoggingTodoRepository(delegate)

    @Factory fun getTodos(repo: TodoRepository) = GetTodosUseCase(repo)
    @Factory fun createTodo(repo: TodoRepository) = CreateTodoUseCase(repo)
    @Factory fun updateTodo(repo: TodoRepository) = UpdateTodoUseCase(repo)
    @Factory fun deleteTodo(repo: TodoRepository) = DeleteTodoUseCase(repo)

    @KoinViewModel
    fun todoViewModel(
        getTodos: GetTodosUseCase, createTodo: CreateTodoUseCase,
        updateTodo: UpdateTodoUseCase, deleteTodo: DeleteTodoUseCase,
    ) = TodoViewModel(getTodos, createTodo, updateTodo, deleteTodo)

    @KoinViewModel
    fun todoDetailViewModel(
        @InjectedParam initialTodo: TodoItem,
        updateTodo: UpdateTodoUseCase, deleteTodo: DeleteTodoUseCase,
    ) = TodoDetailViewModel(initialTodo, updateTodo, deleteTodo)
}

@Named annotation class RealRepo
```

- **`@Module`** marks the class for KSP discovery. There's no `@ComponentScan` needed when modules are declared explicitly in `Initializer.kt` (see below).
- **`@Single` / `@Factory`** = the lifetime of the bound instance. Same semantics as the `single { }` / `factory { }` DSL — singleton vs new-per-resolution.
- **`@KoinViewModel`** binds the function as a Koin `viewModel` definition. Note this annotation lives in `org.koin.android.annotation` even when used from `commonMain` — that's the intended import for Koin Annotations 2.x; the artifact is multiplatform despite the package name.
- **`@Named` meta-annotation** (`@Named annotation class RealRepo`) creates a typed qualifier. Applying `@RealRepo` to a definition or a parameter (`@RealRepo delegate: TodoRepository`) routes resolution through that qualifier — no string keys, no typos.
- **`@InjectedParam`** marks a constructor parameter as supplied at resolution time via `parametersOf(...)`. That's how the detail ViewModel receives the route's `TodoItem`.

##### What KSP generates

For the `TodoModule` above, KSP emits something like this in `build/generated/ksp/metadata/commonMain/kotlin/org/koin/ksp/generated/TodoModuleGen…kt`:

```kotlin
public val TodoModule.module : Module get() = module {
    val moduleInstance = TodoModule()
    single { _ -> moduleInstance.remoteDataSource(get()) } bind TodoRemoteDataSource::class
    single(qualifier = StringQualifier(".../RealRepo")) { _ ->
        moduleInstance.todoRepositoryImpl(get())
    } bind TodoRepository::class
    single { _ ->
        moduleInstance.loggingTodoRepository(
            get(qualifier = StringQualifier(".../RealRepo"))
        )
    } bind TodoRepository::class
    factory { _ -> moduleInstance.getTodos(get()) } bind GetTodosUseCase::class
    // ... and so on
    viewModel { (initialTodo: TodoItem) ->
        moduleInstance.todoDetailViewModel(initialTodo, get(), get())
    } bind TodoDetailViewModel::class
}
```

The annotations are pure declaration — the generated DSL is what Koin actually runs.

##### Decorator pattern, applied to DI

This is the textbook GoF Decorator pattern: the wrapper `LoggingTodoRepository` and the wrapped `TodoRepositoryImpl` implement the **same** `TodoRepository` interface; the wrapper holds a reference to the wrapped instance and delegates after adding behaviour (entry log, latency measurement, exception logging). Consumers don't know — and don't need to know — that they're talking to a decorated implementation.

DI makes this even cleaner: the choice of "use the real impl directly" vs "wrap it in logging" lives in **one place** (the `@Single` declarations above), and we can stack decorators (caching, retry, telemetry) by chaining annotated functions. Use cases stay unaware. This is exactly the architectural point the PDF makes about cross-cutting concerns.

Why `@Single` (not `@Factory`) for the decorator? The wrapped delegate is a `@Single` because its in-memory cache must be shared. The decorator must therefore also be a `@Single` so every consumer sees the same wrapped instance.

##### Decorator pattern, applied to DI

This is the textbook GoF Decorator pattern: the wrapper `LoggingTodoRepository` and the wrapped `TodoRepositoryImpl` implement the **same** `TodoRepository` interface; the wrapper holds a reference to the wrapped instance and delegates after adding behaviour (entry log, latency measurement, exception logging). Consumers don't know — and don't need to know — that they're talking to a decorated implementation.

DI makes this even cleaner: the choice of "use the real impl directly" vs "wrap it in logging" lives in **one place** (the module), and we can stack decorators (caching, retry, telemetry) by chaining `single<TodoRepository> { Decorator(get(named(...))) }` registrations. Use cases are unaware. This is exactly the architectural point the PDF makes about cross-cutting concerns.

Why `single` (not `factory`) for the decorator? The wrapped delegate is a `single` because its in-memory cache must be shared. The decorator must therefore also be a `single` so every consumer sees the same wrapped instance — a `factory` would create a fresh wrapper each time, multiplying log instrumentation without breaking correctness, but also defeating the decorator's intended single-source semantics.

#### `di/Initializer.kt`

```kotlin
fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}) {
    startKoin {
        appDeclaration()
        modules(coreModule, todoModule)
    }
}
```

Single entry point for every platform. The `appDeclaration` lambda lets the Android side inject `androidContext(this)`.

---

### 3.6 Navigation (`commonMain/.../navigation`)

#### `navigation/NavKeys.kt`

```kotlin
sealed interface NavKey

@Serializable
data object TodoListKey : NavKey

@Serializable
data class TodoDetailKey(val todo: TodoItem) : NavKey
```

Type-safe routes modeled in the **Navigation 3** style: a sealed `NavKey` hierarchy plus `@Serializable` data objects/classes that the navigation library encodes into the back stack. There are no string routes anywhere — destinations are values of a closed type, so adding/removing a screen is a compile-time check rather than a runtime "route not found" surprise.

`TodoDetailKey` carries a full `TodoItem`. That makes the route self-describing: pop the back stack, get a `TodoDetailKey`, you have everything you need to render. The trade-off is the abstraction-leak called out in §3.2 — `TodoItem` had to be `@Serializable`. The principled alternative is a `TodoNavArg` mirror class; we keep the simpler shape for clarity in this reference.

#### `navigation/AppNavHost.kt`

```kotlin
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = TodoListKey) {
        composable<TodoListKey> {
            TodoListScreen(
                onItemClick = { todo -> navController.navigate(TodoDetailKey(todo)) },
            )
        }
        composable<TodoDetailKey> { entry ->
            val key = entry.toRoute<TodoDetailKey>()
            TodoDetailScreen(
                todo = key.todo,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
```

One `composable<KeyType>` block per destination. The library handles the back stack, configuration changes, and (on Android) state restoration. Each entry has its own `NavBackStackEntry` which doubles as a `ViewModelStoreOwner` — so `koinViewModel<TodoViewModel>()` inside `TodoListScreen` gives you the *same* ViewModel when you pop back from detail, not a fresh one. The same applies to `TodoDetailViewModel`, but that one is constructed parametrically:

```kotlin
viewModel: TodoDetailViewModel = koinViewModel { parametersOf(todo) }
```

The route arg is deserialized at the navigation boundary, then handed to Koin for ViewModel construction — so the ViewModel never reads back-stack state directly.

##### Navigation 3 caveat

`androidx.navigation3` (the new Jetpack library with `NavBackStack` + `NavDisplay`) is **not yet published as a Compose Multiplatform / KMP artifact**. We use the JetBrains KMP fork `org.jetbrains.androidx.navigation:navigation-compose` (Navigation 2.x) and shape the API in nav3 style — sealed `NavKey`, `@Serializable` routes — so the migration to real nav3 KMP, when it ships, is near-mechanical (rename `NavHost` → `NavDisplay`, swap `composable<X>` for `entry<X>`, swap `rememberNavController()` for `rememberNavBackStack()`). The route shape itself doesn't move.

##### Refresh-on-resume

`TodoListScreen` observes its `LocalLifecycleOwner.lifecycle.currentStateFlow` and, on every transition into `RESUMED` *after* the first one, fires `TodoEvent.Refresh`. That picks up edits the detail screen made (via the in-memory cache shared through `TodoRepositoryImpl`) without any explicit signaling between the two screens. It's the cheapest possible coupling — the list re-reads its own source of truth.

A purer alternative is to expose a `Flow<List<TodoItem>>` from the repository and have both ViewModels collect it; mutations propagate automatically and there's no nav-aware code in the list. That's a recommended next step but kept out of scope here.

---

### 3.7 Platform launchers

Each platform is a thin shell that calls `initKoin` once and renders `App()`.

#### `androidMain/.../KmpTodoApplication.kt` (~10 lines)

```kotlin
class KmpTodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@KmpTodoApplication)
        }
    }
}
```

#### `androidMain/.../MainActivity.kt` (~10 lines)

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}
```

The Manifest registers both classes.

#### `iosMain/.../MainViewController.kt` (~10 lines)

```kotlin
fun MainViewController(): UIViewController {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
    return ComposeUIViewController { App() }
}
```

`ComposeUIViewController` (provided by Compose Multiplatform) hosts the Composable tree inside a UIKit `UIViewController`. The Swift side just embeds it via `UIViewControllerRepresentable`:

```swift
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View { ComposeView().ignoresSafeArea(.keyboard) }
}
```

Kotlin/Native generates the `MainViewControllerKt` class automatically — top-level Kotlin functions become static methods on a `<FileName>Kt` Swift class.

#### `jvmMain/.../desktop/Main.kt` (~15 lines)

```kotlin
fun main() {
    initKoin()
    application {
        Window(onCloseRequest = ::exitApplication, ...) {
            App()
        }
    }
}
```

Compose Desktop's `application { }` block opens a native window and hosts the Composable tree.

---

## 4. Build configuration

### `composeApp/build.gradle.kts` highlights

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget { ... }
    jvm()
    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies { ... }   // shared
        androidMain.dependencies { ... }  // OkHttp + activity-compose
        iosMain.dependencies { ... }      // ktor-client-darwin
        jvmMain.dependencies { ... }      // compose.desktop.currentOs + OkHttp
    }
}
```

- **`baseName = "ComposeApp"`** — Swift code does `import ComposeApp`. This must match what the Xcode project's `OTHER_LDFLAGS` references (`-framework ComposeApp`).
- **`isStatic = true`** — produces a static framework, which is what the Xcode build script expects.
- **`jvm()` (no name)** — creates the `jvmMain` source set. JetBrains's tutorial uses this convention (older templates used `jvm("desktop")` → `desktopMain`, but the current standard is `jvm()`).

### `gradle/libs.versions.toml`

The version catalog is the single source of truth for all dependency versions. To upgrade Ktor app-wide, change `ktor = "2.3.13"` once.

---

## 5. Where each pattern from the PDF lives

| PDF section                                  | File in this project                                     |
|----------------------------------------------|----------------------------------------------------------|
| Sealed Classes                               | `core/result/Result.kt`, `presentation/event/TodoEvent.kt` |
| Suspend Functions                            | every `domain/usecase/*` and `data/source/*`             |
| Coroutines (`viewModelScope.launch`)         | `presentation/viewmodel/TodoViewModel.kt`                |
| Extension Functions                          | `data/mapper/TodoMapper.kt`, `core/exception/Failure.kt` |
| Data Classes                                 | `domain/entity/TodoItem.kt`, `presentation/state/TodoUiState.kt`, `data/model/TodoDto.kt` |
| Clean Architecture                           | `feature/todo/{domain,data,presentation}/`               |
| Dependency Injection (Koin)                  | `feature/todo/di/TodoModule.kt`, `core/di/CoreModule.kt`, `di/Initializer.kt` |
| Networking (Ktor)                            | `core/network/HttpClientFactory.kt`, `data/source/TodoRemoteDataSource.kt` |
| Error Handling (custom Failure sealed class) | `core/exception/Failure.kt`, propagated through `AppResult` |
| State Management (StateFlow + ViewModel)     | `presentation/viewmodel/TodoViewModel.kt`                |
| Platform-specific code (`expect`/`actual`)   | `core/network/HttpClientFactory.kt` + per-platform actual |
| Compose Multiplatform (shared UI)            | every file under `feature/todo/presentation/ui/`         |
| Decorator pattern (GoF)                      | `data/repository/LoggingTodoRepository.kt` + qualifier wiring in `feature/todo/di/TodoModule.kt` |
| Type-safe navigation (Nav3 style)            | `navigation/NavKeys.kt`, `navigation/AppNavHost.kt`      |

---

## 6. How to add a new feature

Following the same pattern, here's the recipe (PDF section "Project Structure & Setup"):

1. Create `commonMain/.../feature/<name>/` with `domain/`, `data/`, `presentation/`, `di/` subfolders.
2. Domain layer:
   - Define the entity (`<Name>Item.kt`).
   - Write the repository interface (`I<Name>Repository.kt`).
   - One use case per operation (`Get<Name>UseCase.kt`, etc.).
3. Data layer:
   - Define the DTO (with `@Serializable`).
   - `<Name>RemoteDataSource` calling Ktor.
   - `<Name>Mapper` (extension functions).
   - `<Name>RepositoryImpl` that combines them.
4. Presentation layer:
   - `<Name>Event` sealed interface.
   - `<Name>UiState` data class.
   - `<Name>ViewModel` extending `ViewModel()`.
   - Composables under `presentation/ui/`.
5. Wire it up:
   - Create `<name>Module` Koin module with `single`/`factory`/`viewModelOf` declarations.
   - Add `<name>Module` to the list passed in `Initializer.kt`.
6. Add it to the back stack:
   - Add a new entry to the `NavKey` sealed interface in `navigation/NavKeys.kt`.
   - Add a `composable<NewKey> { ... }` block in `navigation/AppNavHost.kt` that calls your screen.
   - From the calling screen, push it via `navController.navigate(NewKey(...))`.

That's it — no platform-specific work needed unless you have to call iOS/Android-specific APIs (in which case use `expect`/`actual` like `HttpClientFactory.kt` does).

---

## 7. Testing strategy (not implemented yet, but here's the plan)

Following the PDF's "Testing Architecture" section:

- **Domain tests** (in `commonTest`) — assert use cases by passing a fake `TodoRepository`.
- **Data tests** — assert `TodoRepositoryImpl` by mocking `TodoRemoteDataSource`.
- **ViewModel tests** — assert state transitions by passing fake use cases that return canned `AppResult`s.

All three layers can be tested without spinning up an Android emulator or iOS simulator, because they live entirely in `commonMain` and depend only on coroutines and `kotlinx.serialization`.
