package com.example

import android.app.Application
import com.example.util.CrashHandler

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize the crash handler before any other code or activity is run
        CrashHandler.initialize(this)
    }
}
