package com.pawansingh.vp.update

import android.content.Context
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object VideoCache {
    @Volatile
    private var cache: Cache? = null

    fun get(context: Context): Cache {
        synchronized(this) {
            if (cache == null) {
                val cacheDir = File(context.cacheDir, "shorts_cache")
                val evictor = LeastRecentlyUsedCacheEvictor(200 * 1024 * 1024) // 200 MB
                cache = SimpleCache(cacheDir, evictor)
            }
            return cache!!
        }
    }
}