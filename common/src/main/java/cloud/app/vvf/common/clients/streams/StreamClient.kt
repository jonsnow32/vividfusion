package cloud.app.vvf.common.clients.streams

import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.SubtitleData
import cloud.app.vvf.common.models.stream.StreamData

interface StreamClient : BaseClient {

  suspend fun loadLinks(
    mediaItem: AVPMediaItem,
    subtitleCallback: (SubtitleData) -> Unit,
    callback: (StreamData) -> Unit
  ): Boolean


}
