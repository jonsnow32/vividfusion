package cloud.app.vvf.extension.builtIn.providers

import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.subtitle.SubtitleData

interface SubtitleProvider {
  suspend fun loadSubtitles(
    httpHelper: HttpHelper,
    searchItem: SearchItem,
    callback: suspend (SubtitleData) -> Unit
  )
}
