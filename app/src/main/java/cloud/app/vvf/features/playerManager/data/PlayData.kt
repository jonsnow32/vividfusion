package cloud.app.vvf.features.playerManager.data

import android.net.Uri
import androidx.core.net.toUri
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.stream.StreamData
import kotlinx.serialization.Serializable

@Serializable
data class PlayData(
    val streamEntities: List<StreamData>,
    val selectedId: Int = 0,
    var avpMediaItem: AVPMediaItem? = null,
    val title: String? = null,
    val needToShowAd: Boolean = false,
) {
    fun getDataUri(index: Int): Uri? {
        if(index < 0 || index >= streamEntities.size)
            return null;
        return streamEntities[index].resolvedUrl?.toUri()
    }
}
