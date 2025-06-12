package cloud.app.vvf.extension.tmdb.providers

import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.helpers.network.utils.JsUnpacker
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.common.models.video.Video

abstract class WebScraper(val httpHelper: HttpHelper) {
  abstract val name: String
  abstract val baseUrl: String

  abstract suspend operator fun invoke(
    avpMediaItem: AVPMediaItem,
    subtitleCallback: (SubtitleData) -> Unit,
    callback: (Video) -> Unit
  )

  private val packedRegex = Regex("""eval\(function\(p,a,c,k,e,.*\)\)""")
  fun getPacked(string: String): String? {
    return packedRegex.find(string)?.value
  }
  fun getAndUnpack(string: String): String {
    val packedText = getPacked(string) ?: return string
    return JsUnpacker.unpackAndCombine(packedText) ?: string
  }
}
