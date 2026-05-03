package com.example.kmptodo

import android.app.Application
import com.example.kmptodo.di.initKoin
import org.koin.android.ext.koin.androidContext

class KmpTodoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@KmpTodoApplication)
        }
    }
}
