package cloud.app.common.clients.streams

import cloud.app.common.clients.BaseClient
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.SubtitleData
import cloud.app.common.models.stream.StreamData

interface StreamClient : BaseClient {

  suspend fun loadLinks(
    mediaItem: AVPMediaItem,
    subtitleCallback: (SubtitleData) -> Unit,
    callback: (StreamData) -> Unit
  ): Boolean


}
