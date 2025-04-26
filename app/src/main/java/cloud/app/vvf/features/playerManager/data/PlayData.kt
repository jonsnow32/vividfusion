package cloud.app.vvf.features.playerManager.data

import android.net.Uri
import androidx.core.net.toUri
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.common.models.video.Video
import kotlinx.serialization.Serializable

@Serializable
data class PlayData(
    val videos: List<Video>,
    val subtitles: List<SubtitleData>? = null,
    val selectedId: Int = 0,
    var avpMediaItem: AVPMediaItem? = null,
    val title: String? = null,
    val needToShowAd: Boolean = false,
) {
  fun getDataUri(index: Int): Uri? {
    if (index < 0 || index >= videos.size)
      return null;
    return videos[index].uri.toUri()
  }
}
