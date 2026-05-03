# Getting Started

## Prerequisites

| Platform | Tools                                                                      |
|----------|----------------------------------------------------------------------------|
| All      | JDK 17 (Android Studio's bundled JBR works), Gradle wrapper (already in repo) |
| Android  | Android Studio Hedgehog or later, an emulator on API 24+                   |
| iOS      | macOS with Xcode 15+, an iOS Simulator                                     |
| Desktop  | Just JDK 17                                                                |

## Open in Android Studio

```bash
cd kmp_todo_app
open -a "Android Studio" .
```

When Android Studio opens, let it finish syncing (this downloads Compose Multiplatform, Ktor, Koin, etc.).

## Run Android

1. In the Run Configurations dropdown, pick `composeApp` and a device/emulator.
2. Click ▶.

Or from the command line:

```bash
./gradlew :composeApp:installDebug
adb shell am start -n com.example.kmptodo/.MainActivity
```

## Run Desktop

```bash
./gradlew :composeApp:run
```

A native window opens running the same Composable UI as the mobile apps.

## Run iOS

You need to use Xcode for the iOS build. The Gradle build script is wired up to run automatically.

```bash
open iosApp/iosApp.xcodeproj
```

In Xcode:

1. Select an iPhone simulator from the toolbar.
2. Wait for Xcode to index.
3. Click ▶.

The build phase script (in *Build Phases → Run Script*) calls `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` first, which builds the Kotlin/Native framework and drops it into a path Xcode can find.

If the iOS build complains about missing framework, run this once manually:

```bash
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode \
  -Pkotlin.native.cocoapods.platform=iphonesimulator \
  -Pkotlin.native.cocoapods.archs=arm64 \
  -Pkotlin.native.cocoapods.configuration=Debug
```

## What you should see

- A toolbar titled **Todos** with a refresh icon.
- A list of 200 todos fetched from `https://jsonplaceholder.typicode.com/todos`.
- A FAB (+ button) that opens a dialog to create a new todo.
- Each row has a checkbox (toggles completion) and a trash icon (deletes).
- Errors appear as a snackbar at the bottom.

> ⚠️ JSONPlaceholder is a *mock* API — it returns realistic responses for POST/PUT/DELETE but doesn't actually persist anything server-side. The repository keeps an in-memory cache so the UI behaves correctly across the session. Restarting the app re-fetches the original 200 todos.

## Common issues

**"Unable to locate a Java Runtime"** — set `JAVA_HOME` to Android Studio's bundled JBR:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

**iOS build can't find `ComposeApp` framework** — make sure you're running through Xcode (not just `./gradlew`), or run `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` first.

**Stale Gradle cache** —

```bash
./gradlew --stop
./gradlew clean
```
