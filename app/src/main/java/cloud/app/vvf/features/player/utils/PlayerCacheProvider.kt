package cloud.app.vvf.features.player.utils

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object PlayerCacheProvider {
  @Volatile
  private var simpleCache: SimpleCache? = null

  fun getInstance(context: Context): SimpleCache {
    return simpleCache ?: synchronized(this) {
      try {
        simpleCache ?: SimpleCache(
          File(context.cacheDir, "player_cache"),
          LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024)
        ).also { simpleCache = it }
      } catch (e: IllegalStateException) {
        // Cache is likely corrupted, delete and recreate
        val cacheDir = File(context.cacheDir, "player_cache")
        cacheDir.deleteRecursively()
        SimpleCache(
          cacheDir,
          LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024)
        ).also { simpleCache = it }
      }
    }
  }
}
