package cloud.app.avp.glide

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import cloud.app.common.models.Request
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import java.io.InputStream


@GlideModule
class AppGlideModule : com.bumptech.glide.module.AppGlideModule() {
  override fun registerComponents(
    context: Context, glide: Glide, registry: Registry
  ) {
    registry.append(Request::class.java, InputStream::class.java, RequestLoaderFactory.Factory())
  }

  override fun isManifestParsingEnabled(): Boolean {
    return false
  }
  override fun applyOptions(context: Context, builder: GlideBuilder) {
    builder.setLogLevel(Log.ERROR)
    val diskCacheSizeBytes = 1024 * 1024 * 100L // 100 MB
    builder.setDiskCache(
      InternalCacheDiskCacheFactory(
        context,
        "imageCache",
        diskCacheSizeBytes
      )
    )
    builder.setDefaultTransitionOptions(
      Drawable::class.java,
      DrawableTransitionOptions.withCrossFade()
    )
  }
}
