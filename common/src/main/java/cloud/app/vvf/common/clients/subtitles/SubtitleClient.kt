package cloud.app.vvf.common.clients.subtitles

import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.subtitle.SubtitleData

interface SubtitleClient : BaseClient {
  suspend fun loadSubtitles(
    searchItem: SearchItem,
    callback: suspend (SubtitleData) -> Unit
  ): Boolean
}
