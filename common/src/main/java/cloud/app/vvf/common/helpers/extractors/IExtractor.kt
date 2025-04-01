package cloud.app.vvf.common.helpers.extractors

import cloud.app.vvf.common.models.stream.StreamData
import cloud.app.vvf.common.models.subtitle.SubtitleData

interface IExtractor {
  val name: String
  val baseUrl: String

  suspend fun extract(
    url: String,
    referer: String? = null,
    subtitleCallback: (SubtitleData) -> Unit,
    callback: (StreamData) -> Unit
  )
}
