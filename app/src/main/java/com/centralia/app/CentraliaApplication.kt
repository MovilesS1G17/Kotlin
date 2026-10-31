package com.centralia.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.centralia.app.app.DependencyContainer


class CentraliaApplication : Application(), ImageLoaderFactory {

    lateinit var container: DependencyContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DependencyContainer.make(this)
    }

    /**
     * Caching (Sprint 3): covers are cached in two layers — decoded bitmaps in
     * memory (up to 20% of the app's memory) and the downloaded files on disk
     * (up to 100 MB). The disk copy ignores the platforms' short cache headers,
     * so covers still show offline. Coil decodes and downloads off the main
     * thread.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("covers"))
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
}
