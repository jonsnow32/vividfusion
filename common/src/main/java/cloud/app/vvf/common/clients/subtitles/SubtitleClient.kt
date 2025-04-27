package cloud.app.vvf.common.clients.subtitles

import cloud.app.vvf.common.clients.BaseClient
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.common.models.video.Video

interface SubtitleClient : BaseClient {
  suspend fun loadSubtitles(
    mediaItem: AVPMediaItem,
    callback: (SubtitleData) -> Unit
  ): Boolean
}
