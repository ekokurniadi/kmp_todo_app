package com.example.kmptodo.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.kmptodo.App
import com.example.kmptodo.di.initKoin

fun main() {
    initKoin()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            state = rememberWindowState(width = 480.dp, height = 720.dp),
            title = "KMP Todo"
        ) {
            App()
        }
    }
}
