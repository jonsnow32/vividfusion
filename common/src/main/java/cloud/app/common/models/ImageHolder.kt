package cloud.app.common.models

import android.graphics.Bitmap
import android.net.Uri

import cloud.app.common.models.Request.Companion.toRequest
import kotlinx.serialization.Serializable

@Serializable
sealed class ImageHolder {
  constructor()
  abstract val crop: Boolean

  @Serializable
  data class UrlRequestImageHolder(val request: Request, override val crop: Boolean) :
    ImageHolder()

  @Serializable
  data class UriImageHolder(val uri: String, override val crop: Boolean) : ImageHolder()

  companion object {
    fun String.toImageHolder(
      headers: Map<String, String> = mapOf(),
      crop: Boolean = false
    ) = UrlRequestImageHolder(this.toRequest(headers), crop)

    fun String.toUriImageHolder(crop: Boolean = false) = UriImageHolder(this, crop)
  }
}

