package cloud.app.avp.glide

import cloud.app.avp.utils.ImageUrlBuilder
import cloud.app.common.models.Request
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader
import java.io.InputStream

class RequestLoaderFactory private constructor(urlLoader: ModelLoader<GlideUrl, InputStream>) :
  BaseGlideUrlLoader<Request>(urlLoader) {

  override fun handles(model: Request): Boolean {
    return true
  }

  override fun getHeaders(model: Request, width: Int, height: Int, options: Options?): Headers {
    return Headers { model.headers }
  }

  override fun getUrl(model: Request, width: Int, height: Int, options: Options?): String {
    return ImageUrlBuilder.getUrl(model.url, width)
  }

  class Factory : ModelLoaderFactory<Request, InputStream> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Request, InputStream> {
      return RequestLoaderFactory(
        multiFactory.build(
          GlideUrl::class.java,
          InputStream::class.java
        )
      )
    }

    override fun teardown() {
      // Do nothing.
    }
  }
}
