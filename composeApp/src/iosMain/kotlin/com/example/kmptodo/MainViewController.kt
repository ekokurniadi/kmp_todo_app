package com.example.kmptodo

import androidx.compose.ui.window.ComposeUIViewController
import com.example.kmptodo.di.initKoin
import platform.UIKit.UIViewController

/**
 * Entry point exposed to Swift. The first time it's called we initialize
 * Koin (idempotent guard so hot reloads don't crash), then return a
 * UIViewController that hosts the shared Compose UI.
 *
 * Swift-side this becomes `MainViewControllerKt.MainViewController()`.
 */
private var koinStarted = false

fun MainViewController(): UIViewController {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
    return ComposeUIViewController { App() }
}
