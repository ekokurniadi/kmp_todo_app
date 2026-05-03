package com.example.kmptodo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.example.kmptodo.navigation.AppNavHost

/**
 * The single root composable. Each platform launcher (Android, iOS, Desktop)
 * sets this as its content — that's why the UI works everywhere with no
 * platform-specific UI code.
 *
 * Koin is assumed to be initialized before [App] is called. The Android
 * Application class, the iOS MainViewController, and the desktop `main()`
 * each call `initKoin(...)` once at startup.
 *
 * Navigation lives inside [AppNavHost]; this composable only owns the
 * theme/surface chrome.
 */
@Composable
fun App() {
    MaterialTheme {
        Surface {
            AppNavHost()
        }
    }
}
