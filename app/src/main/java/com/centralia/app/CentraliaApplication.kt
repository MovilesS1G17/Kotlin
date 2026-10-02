package com.centralia.app

import android.app.Application
import com.centralia.app.app.DependencyContainer

/**
 * `@main struct CentraliaApp` holds the container in `@State` so it outlives the
 * view tree. The Android equivalent that outlives every activity and
 * configuration change is the [Application], so the container is built once here.
 */
class CentraliaApplication : Application() {

    lateinit var container: DependencyContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DependencyContainer.mock(this)
    }
}
