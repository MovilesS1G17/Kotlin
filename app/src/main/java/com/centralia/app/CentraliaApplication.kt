package com.centralia.app

import android.app.Application
import com.centralia.app.app.DependencyContainer


class CentraliaApplication : Application() {

    lateinit var container: DependencyContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DependencyContainer.make(this)
    }
}
