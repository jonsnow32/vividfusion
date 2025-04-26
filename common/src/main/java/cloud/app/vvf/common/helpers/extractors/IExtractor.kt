package cloud.app.vvf.common.helpers.extractors

import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.common.models.video.Video

interface IExtractor {
  val name: String
  val baseUrl: String

  suspend fun extract(
    url: String,
    referer: String? = null,
    subtitleCallback: (SubtitleData) -> Unit,
    callback: (Video) -> Unit
  )
}
