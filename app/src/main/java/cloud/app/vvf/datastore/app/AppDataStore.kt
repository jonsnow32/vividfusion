package cloud.app.vvf.datastore.app

import android.content.Context
import android.graphics.Color
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.AVPMediaItem.PlaybackProgress
import cloud.app.vvf.datastore.DataStore
import cloud.app.vvf.datastore.account.Account
import cloud.app.vvf.datastore.app.helper.PlayerSettingItem
import cloud.app.vvf.datastore.app.helper.UriHistoryItem
import cloud.app.vvf.features.player.subtitle.DEF_SUBS_ELEVATION
import cloud.app.vvf.features.player.subtitle.SubtitleStyle
import cloud.app.vvf.services.downloader.DownloadData
import cloud.app.vvf.services.downloader.DownloadStatus


const val URI_HISTORY_FOLDER = "uri_history"
const val PLAYER_SETTING_FOLDER = "player_setting"
const val PlaybackProgressFolder = "history_progress"
const val DOWNLOAD_FOLDER = "downloads"

class AppDataStore(val context: Context, val account: Account) :
  DataStore(context.getSharedPreferences("account_${account.getSlug()}", Context.MODE_PRIVATE)) {

  fun updateProgress(data: PlaybackProgress): Boolean {
    data.lastUpdated = System.currentTimeMillis()
    if (data.item is AVPMediaItem.VideoItem) {
      set("$PlaybackProgressFolder/${data.item.id}", data)
      return true
    }
    return false
  }

  fun findPlaybackProgress(mediaItem: AVPMediaItem): PlaybackProgress? =
    when (mediaItem) {
      is AVPMediaItem.VideoItem ->
        getAll<PlaybackProgress>("$PlaybackProgressFolder/${mediaItem.id}")?.maxByOrNull { it.lastUpdated }
      else -> null
    }

  fun findPlaybackProgress(slug: String): PlaybackProgress? =
    getAll<PlaybackProgress>("$PlaybackProgressFolder/$slug")?.maxByOrNull { it.lastUpdated }

  fun getALlPlayback(): List<PlaybackProgress>? {
    return getAll<PlaybackProgress>("$PlaybackProgressFolder/")
      ?.filter { it.item is AVPMediaItem.VideoItem }
      ?.sortedByDescending { it.lastUpdated }
  }

  fun saveUriHistory(item: UriHistoryItem) {
    return set("$URI_HISTORY_FOLDER/${item.id}", item)
  }

  fun getUriHistory(): List<UriHistoryItem>? {
    return getAll<UriHistoryItem>("$URI_HISTORY_FOLDER/")?.sortedByDescending { it.lastUpdated }
  }

  fun deleteUriHistory(item: UriHistoryItem) {
    return removeKey("$URI_HISTORY_FOLDER/${item.id}")
  }

  fun cleanUriHistory() {
    return removeKey("$URI_HISTORY_FOLDER/")
  }

  @UnstableApi
  fun getPlayerSetting(): PlayerSettingItem {
    return get<PlayerSettingItem>("$PLAYER_SETTING_FOLDER/") ?: PlayerSettingItem(
      subtitleStyle = SubtitleStyle(
        foregroundColor = getDefColor(0),
        backgroundColor = getDefColor(2),
        windowColor = getDefColor(3),
        edgeType = CaptionStyleCompat.EDGE_TYPE_OUTLINE,
        edgeColor = getDefColor(1),
        typeface = null,
        typefaceFilePath = null,
        elevation = DEF_SUBS_ELEVATION,
        fixedTextSize = null,
      )
    )
  }

  private fun getDefColor(id: Int): Int {
    return when (id) {
      0 -> Color.WHITE
      1 -> Color.BLACK
      2 -> Color.TRANSPARENT
      3 -> Color.TRANSPARENT
      else -> Color.TRANSPARENT
    }
  }

  fun getAllDownloads(): List<DownloadData>? {
    return getAll<DownloadData>("$DOWNLOAD_FOLDER/")?.sortedByDescending { it.updatedAt }
  }

  fun saveDownload(downloadData: DownloadData) {
    val updateData = downloadData.copy(updatedAt = System.currentTimeMillis())
    set("$DOWNLOAD_FOLDER/${updateData.id}", updateData)
  }

  fun getDownload(downloadId: String): DownloadData? {
    return get<DownloadData>("$DOWNLOAD_FOLDER/$downloadId")
  }

  fun removeDownload(downloadId: String) {
    removeKey("$DOWNLOAD_FOLDER/$downloadId")
  }

  fun removeDownload(downloadData: DownloadData) {
    removeKey("$DOWNLOAD_FOLDER/${downloadData.id}")
  }

  fun updateDownloadProgress(downloadId: String, progress: Int, downloadedBytes: Long) {
    val downloadData = getDownload(downloadId) ?: return
    saveDownload(downloadData.copy(progress = progress, downloadedBytes = downloadedBytes))
  }

  fun updateDownloadStatus(downloadId: String, status: DownloadStatus) {
    val downloadData = getDownload(downloadId) ?: return
    saveDownload(downloadData.copy(status = status))
  }

  fun getDownloadsByStatus(status: DownloadStatus): List<DownloadData>? {
    return getAllDownloads()?.filter { it.status == status }
  }

  fun getActiveDownloads(): List<DownloadData>? {
    return getAllDownloads()?.filter { it.isActive() }
  }

  fun getCompletedDownloads(): List<DownloadData>? {
    return getAllDownloads()?.filter { it.isCompleted() }
  }

  fun clearAllDownloads() {
    removeKey("$DOWNLOAD_FOLDER/")
  }

  fun getDownloadByMediaItem(mediaItem: AVPMediaItem): DownloadData? {
    return getAllDownloads()?.firstOrNull { it.mediaItem?.id == mediaItem.id }
  }
}
