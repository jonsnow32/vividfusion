package cloud.app.vvf.features.player

import android.app.Application
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.features.player.utils.MediaItemUtils.toMediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
  val preference: SharedPreferences,
  private val application: Application,
  val appDataStoreFlow: MutableStateFlow<AppDataStore>
) : AndroidViewModel(application) {

  // Public fields
  var player: ExoPlayer? = null
  var playbackPosition = MutableStateFlow(0L)
  var isPlaying = MutableStateFlow(false)
  var playbackState = MutableStateFlow(0)
  var resizeMode = MutableStateFlow(0) // 0 = Fit, 1 = Fill, 2 = Zoom
  var requestedOrientation = MutableStateFlow(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
  var fullscreenNotch = MutableStateFlow(false)
  var isControlsLocked = MutableStateFlow(false) // Added for lock control state

  init {
    fullscreenNotch.value = preference.getBoolean(
      application.getString(R.string.pref_key_overlap_notch), true
    )
  }

  // Initialize ExoPlayer with a list of MediaItems and SubtitleData
  @androidx.media3.common.util.UnstableApi
  fun initialize(
    mediaItems: List<AVPMediaItem>,
    selectedMediaIdx: Int = 0,
    subtitles: List<SubtitleData> = emptyList(),
    selectedSubtitleIdx: Int = 0,
    initialPosition: Long = 0L
  ) {
    if (player != null) return

    val exoMediaItems = mediaItems.mapNotNull { it.toMediaItem() }
    if (exoMediaItems.isEmpty()) return

    require(selectedMediaIdx in exoMediaItems.indices) { "selectedMediaIdx must be within mediaItems range" }
    require(selectedSubtitleIdx in subtitles.indices || subtitles.isEmpty()) { "selectedSubtitleIdx must be within subtitles range or subtitles can be empty" }

    val mediaSourceFactory = DefaultMediaSourceFactory(application)

    val subtitleConfigurations = subtitles.map { subtitle ->
      MediaItem.SubtitleConfiguration.Builder(subtitle.url.toUri())
        .setMimeType(subtitle.mimeType)
        .setLanguage(subtitle.languageCode)
        .setLabel(subtitle.name)
        .setId(subtitle.url)
        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
        .build()
    }

    val enhancedMediaItems = exoMediaItems.map { mediaItem ->
      mediaItem.buildUpon()
        .setSubtitleConfigurations(subtitleConfigurations)
        .build()
    }

    val mediaSources = enhancedMediaItems.map { mediaItem ->
      mediaSourceFactory.createMediaSource(mediaItem)
    }

    player = ExoPlayer.Builder(application)
      .setMediaSourceFactory(mediaSourceFactory)
      .build()
      .apply {
        setMediaSources(mediaSources, selectedMediaIdx, initialPosition)
        playWhenReady = true
        pauseAtEndOfMediaItems = true
        if (subtitles.isNotEmpty()) {
          val trackSelectorParameters = trackSelector?.parameters?.buildUpon()
            ?.setPreferredTextLanguage(subtitles[selectedSubtitleIdx].languageCode)
            ?.setPreferredTextRoleFlags(C.ROLE_FLAG_SUBTITLE)
            ?.build()

          if (trackSelectorParameters != null) {
            trackSelector?.parameters = trackSelectorParameters
          }
        }
        prepare()
      }
  }


  // Control resize
  fun toggleResizeMode() {
    resizeMode.value = (resizeMode.value + 1) % 3
  }

  // Set orientation
  fun setOrientation(videoSize: VideoSize) {
    val orientation = if (requestedOrientation.value != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
      requestedOrientation.value
    } else {
      when {
        videoSize.width == 0 || videoSize.height == 0 -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        videoSize.height > videoSize.width -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      }
    }
    requestedOrientation.value = orientation
  }
  // Set controls locked state
  fun setControlsLocked(locked: Boolean) {
    isControlsLocked.value = locked
  }

  // Release resources
  override fun onCleared() {
    player?.release()
    player = null
    super.onCleared()
  }
}
