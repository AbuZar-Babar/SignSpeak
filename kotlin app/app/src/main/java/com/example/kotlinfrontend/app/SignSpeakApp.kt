package com.example.kotlinfrontend.app

import android.app.Application

class SignSpeakApp : Application() {
    val appContainer: AppContainer by lazy {
        AppContainer(applicationContext)
    }
}
