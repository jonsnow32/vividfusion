package cloud.app.vvf.features.player

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mkv.MatroskaExtractor
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.extractor.flac.FlacExtractor
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.SubtitleParser.OutputOptions.allCues
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.datastore.app.AppDataStore
import cloud.app.vvf.features.player.subtitle.DelayedSubtitleParserFactory
import cloud.app.vvf.features.player.utils.MediaItemUtils.toMediaItem
import cloud.app.vvf.features.player.utils.getSelected
import cloud.app.vvf.features.player.utils.getSubtitleMime
import cloud.app.vvf.features.player.subtitle.SubtitleCue
import cloud.app.vvf.features.player.utils.PlayerCacheProvider
import cloud.app.vvf.features.player.utils.uriToSubtitleConfiguration
import cloud.app.vvf.utils.showToast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject


@HiltViewModel
@UnstableApi
class PlayerViewModel @Inject constructor(
  val defaultAppSetting: SharedPreferences,
  private val application: Application,
  private val appDataStoreFlow: MutableStateFlow<AppDataStore>
) : AndroidViewModel(application) {

  // Public fields
  var player: ExoPlayer? = null
  var playbackPositionMs = MutableStateFlow(0L)
  var isPlaying = MutableStateFlow(false)
  var playbackState = MutableStateFlow(0)
  var videoSize = MutableStateFlow<VideoSize?>(null)
  var textTrackIdx = MutableStateFlow<Int?>(null)
  var audioTrackIdx = MutableStateFlow<Int?>(null)
  var videoTrackIdx = MutableStateFlow<Int?>(null)

  var resizeMode = MutableStateFlow<Int?>(null) // 0 = Fit, 1 = Fill, 2 = Zoom
  var requestedOrientation = MutableStateFlow(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
  var fullscreenNotch = MutableStateFlow(false)
  var isControlsLocked = MutableStateFlow(false)
  var cuesWithTiming = MutableStateFlow<List<SubtitleCue>>(emptyList())
  var tracks = MutableStateFlow<Tracks?>(null)
  var mediaMetaData = MutableStateFlow<MediaMetadata?>(null)
  var playbackException = MutableStateFlow<PlaybackException?>(null)

  private val playerListener by lazy { PlayerListener(this@PlayerViewModel) }
  lateinit var delayedFactory: DelayedSubtitleParserFactory // Expose for fragment access
  private var simpleCache: SimpleCache? = null

  private val currentPrefCacheSize by lazy {
    defaultAppSetting.getInt(
      application.getString(R.string.pref_video_cache_size_on_ram), 0
    ) * 1024 * 1024L
  }
  private val currentPrefDiskSize by lazy {
    defaultAppSetting.getInt(
      application.getString(R.string.pref_video_cache_size_on_disk), 0
    ) * 1024 * 1024L
  }
  private val currentPrefBufferSec by lazy {
    defaultAppSetting.getInt(
      application.getString(R.string.pref_buffer_second),
      DefaultLoadControl.DEFAULT_MAX_BUFFER_MS / 1000
    )
  }

  init {
    fullscreenNotch.value = defaultAppSetting.getBoolean(
      application.getString(R.string.pref_key_overlap_notch), true
    )
  }

  fun initialize(
    context: Context,
    mediaItems: List<AVPMediaItem>,
    selectedMediaIdx: Int = 0,
    subtitles: List<SubtitleData> = emptyList(),
    selectedSubtitleIdx: Int = 0,
    initialPosition: Long = 0L,
    subtitleOffsetMs: Long
  ) {
    if (player != null) {
      play()
      return
    }

    val exoMediaItems = mediaItems.mapNotNull { it.toMediaItem() }
    if (exoMediaItems.isEmpty()) return

    require(selectedMediaIdx in exoMediaItems.indices) { "selectedMediaIdx must be within mediaItems range" }
    require(selectedSubtitleIdx in subtitles.indices || subtitles.isEmpty()) { "selectedSubtitleIdx must be within subtitles range or subtitles can be empty" }

    // Initialize cache with ExoDatabaseProvider
    simpleCache = PlayerCacheProvider.getInstance(context)

    delayedFactory = DelayedSubtitleParserFactory(DefaultSubtitleParserFactory())
    delayedFactory.setDelayMs(subtitleOffsetMs)

    val dataSourceFactory = DefaultDataSource.Factory(context)
    val cacheDataSourceFactory = CacheDataSource.Factory().setCache(simpleCache!!)
      .setUpstreamDataSourceFactory(dataSourceFactory)
      .setCacheWriteDataSinkFactory(null) // Optional: Disable writing to cache for specific cases
      .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    // Create enhanced extractors factory to support more media formats
    val extractorsFactory = DefaultExtractorsFactory()
      .setConstantBitrateSeekingEnabled(true)
      .setAdtsExtractorFlags(0)
      .setFlacExtractorFlags(FlacExtractor.FLAG_DISABLE_ID3_METADATA)
//      .setMatroskaExtractorFlags(MatroskaExtractor.FLAG_DISABLE_SEEK_FOR_CUES)
      .setMp4ExtractorFlags(Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)
      .setFragmentedMp4ExtractorFlags(FragmentedMp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)
      .setTsExtractorFlags(0) // Use default flags for TS extractor

    val mediaSourceFactory =
      DefaultMediaSourceFactory(cacheDataSourceFactory, extractorsFactory).setSubtitleParserFactory(delayedFactory)

    val subtitleConfigurations = subtitles.map { subtitle ->
      MediaItem.SubtitleConfiguration.Builder(subtitle.url.toUri())
        .setMimeType(subtitle.mimeType)
        .setLanguage(subtitle.languageCode)
        .setLabel(subtitle.name)
        .setId(subtitle.url)
        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT).build()
    }

    val enhancedMediaItems = exoMediaItems.map { mediaItem ->
      mediaItem.buildUpon().setSubtitleConfigurations(subtitleConfigurations).build()
    }

    val mediaSources = enhancedMediaItems.map { mediaItem ->
      mediaSourceFactory.createMediaSource(mediaItem)
    }

    val loadControl = DefaultLoadControl.Builder()
      .setTargetBufferBytes(
        ((if (currentPrefCacheSize <= 0)
          DefaultLoadControl.DEFAULT_TARGET_BUFFER_BYTES
        else
          currentPrefCacheSize)).toInt()
      )
      .setBackBuffer(30000, true)
      .setBufferDurationsMs(
        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
        if (currentPrefBufferSec <= 0) DefaultLoadControl.DEFAULT_MAX_BUFFER_MS
        else currentPrefBufferSec * 1000,
        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS,
        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
      ).build()

    player = ExoPlayer.Builder(application).setLoadControl(loadControl)
      .setMediaSourceFactory(mediaSourceFactory).build().apply {
        addListener(playerListener)
        setMediaSources(mediaSources, selectedMediaIdx, initialPosition)
        playWhenReady = true
        setSeekParameters(SeekParameters.NEXT_SYNC)
        pauseAtEndOfMediaItems = true
        if (subtitles.isNotEmpty()) {
          val trackSelectorParameters = trackSelector?.parameters?.buildUpon()
            ?.setPreferredTextLanguage(subtitles[selectedSubtitleIdx].languageCode)
            ?.setPreferredTextRoleFlags(C.ROLE_FLAG_SUBTITLE)?.build()

          if (trackSelectorParameters != null) {
            trackSelector?.parameters = trackSelectorParameters
          }
        }
        prepare()
      }
  }

  fun parseSubtitles(context: Context, onResult: (Boolean) -> Unit) {
    val subtitleGroup =
      tracks.value?.groups?.filter { trackGroup -> trackGroup.type == C.TRACK_TYPE_TEXT }
        ?.getSelected()
    val selectedSubtitleIdx = subtitleGroup?.second ?: -1
    val subtitleUri =
      player?.currentMediaItem?.localConfiguration?.subtitleConfigurations?.getOrNull(
        selectedSubtitleIdx
      )?.uri

    viewModelScope.launch(Dispatchers.IO) {
      try {
        if (subtitleUri == null || simpleCache == null) {
          onResult(false)
          return@launch
        }
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val cacheDataSourceFactory = CacheDataSource.Factory().setCache(simpleCache!!)
          .setUpstreamDataSourceFactory(dataSourceFactory)
        val dataSource = cacheDataSourceFactory.createDataSource()
        val dataSpec = DataSpec(subtitleUri)
        dataSource.open(dataSpec)

        val buffer = ByteArrayOutputStream()
        val byteArray = ByteArray(1024)
        var bytesRead: Int
        while (dataSource.read(byteArray, 0, byteArray.size)
            .also { bytesRead = it } != C.RESULT_END_OF_INPUT
        ) {
          buffer.write(byteArray, 0, bytesRead)
        }
        dataSource.close()

        val subtitleData = buffer.toByteArray()
        val parser = delayedFactory.create(
          MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(subtitleUri.getSubtitleMime()).build().toFormat()
        )
        val cues = mutableListOf<CuesWithTiming>()
        parser.parse(
          subtitleData,
          0,
          subtitleData.size,
          allCues(),
          cues::add
        )
        cuesWithTiming.value = cues.map { cue ->
          SubtitleCue(
            cue.startTimeUs / 1000, cue.durationUs / 1000, cue.cues.map { it.text.toString() })
        }
        onResult(true)
      } catch (e: Exception) {
        e.printStackTrace()
        cuesWithTiming.value = emptyList()
        onResult(false)
      }
    }
  }

  fun toggleResizeMode() {
    val oldValue = resizeMode.value ?: 0
    resizeMode.value = (oldValue + 1) % 3
  }

  fun setOrientation(videoSize: VideoSize) {
    val orientation =
      if (requestedOrientation.value != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
        requestedOrientation.value
      } else {
        when {
          videoSize.width == 0 || videoSize.height == 0 -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
          videoSize.height > videoSize.width -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
          else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
      }
    requestedOrientation.value = orientation
    this.videoSize.value = videoSize
  }

  fun setControlsLocked(locked: Boolean) {
    isControlsLocked.value = locked
  }

  override fun onCleared() {
    player?.release()
    player = null
    playerListener.release()
    super.onCleared()
  }

  fun pause() {
    if (player?.isPlaying == true) player?.pause()
  }

  fun play() {
    if (player?.isPlaying == false) player?.play()
  }

  fun seekTo(position: Long) {
    player?.seekTo(position)
  }

  fun seekToNext() {
    player?.seekToNext()
  }

  fun selectAudioTrack(audioTrackIndex: Int) {
    player?.switchTrack(C.TRACK_TYPE_AUDIO, audioTrackIndex)
    audioTrackIdx.value = audioTrackIndex
  }

  fun selectVideoTrack(videoTrackIndex: Int) {
    player?.switchTrack(C.TRACK_TYPE_VIDEO, videoTrackIndex)
    videoTrackIdx.value = videoTrackIndex
  }

  @OptIn(UnstableApi::class)
  fun selectTextTrack(textTrackIndex: Int) {
    player?.switchTrack(C.TRACK_TYPE_TEXT, textTrackIndex)
    textTrackIdx.value = textTrackIndex
  }

  fun addSubtitleData(context: Context, subtitles: List<SubtitleData>) {
    viewModelScope.launch {
      val index = player?.addAdditionalSubtitleConfiguration(subtitles.map {
        context.uriToSubtitleConfiguration(it.url.toUri())
          .buildUpon()
          .setLanguage(it.languageCode ?: "unknown")
          .setLabel(it.name)
          .build()
      })
      textTrackIdx.value = index ?: textTrackIdx.value
      showSubtitleToast(context, index)
    }
  }

  fun addSubtitleUri(context: Context, uris: List<Uri>) {
    viewModelScope.launch {
      val index =
        player?.addAdditionalSubtitleConfiguration(uris.map { context.uriToSubtitleConfiguration(it) })
      textTrackIdx.value = index ?: textTrackIdx.value
      showSubtitleToast(context, index)
    }
  }


  private fun showSubtitleToast(context: Context, index: Int?) {
    if (index != null) {
      val subConfig =
        player?.currentMediaItem?.localConfiguration?.subtitleConfigurations?.get(index)
      context.showToast(
        context.getString(
          R.string.subtitles_added,
          subConfig?.label
        )
      )
    } else {
      context.showToast(context.getString(R.string.subtitles_not_added))
    }
  }
  fun updateSubtitleOffset(context: Context, offsetSeconds: Long) {
    viewModelScope.launch(Dispatchers.Main) {
      try {
        delayedFactory.setDelayMs(offsetSeconds)

        val currentTextTrackIdx = textTrackIdx.value
        val player = player ?: return@launch

        // Force subtitle re-parsing by removing and re-adding the subtitle configuration
        val subtitleConfigs = player.currentMediaItem?.localConfiguration?.subtitleConfigurations
        val subtitleUri = subtitleConfigs?.getOrNull(currentTextTrackIdx ?: -1)?.uri ?: return@launch
        val subtitleMime = subtitleUri.getSubtitleMime()

        val newConfig = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
          .setMimeType(subtitleMime)
          .setLanguage("und")
          .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
          .build()

        player.addAdditionalSubtitleConfiguration(listOf(newConfig))
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }


  private var updateJob: Job? = null
  private val delayMs = 500L

  fun registerUpdateProgress() {
    player?.let { player ->
      playbackPositionMs.value = player.currentPosition
      updateJob?.cancel() // Cancel any existing job
      if (player.playWhenReady && player.playbackState == ExoPlayer.STATE_READY) {
        updateJob = viewModelScope.launch {
          while (isActive) {
            playbackPositionMs.value = player.currentPosition
            delay(delayMs)
          }
        }
      }
    }
  }
  fun cancelUpdateProgress() {
    updateJob?.cancel()
  }
}

// Extension to convert SubtitleConfiguration to Format
@OptIn(UnstableApi::class)
private fun MediaItem.SubtitleConfiguration.toFormat(): androidx.media3.common.Format {
  return androidx.media3.common.Format.Builder().setSampleMimeType(mimeType).setLanguage(language)
    .setLabel(label).setId(id).setSelectionFlags(selectionFlags).build()
}


/**
 * Switches to selected track.
 *
 * @param trackType The type of track to switch.
 * @param trackIndex The index of the track to switch to, or null to enable the track.
 *
 * if trackIndex is a negative number, the track will be disabled
 * if trackIndex is a valid index, the track will be switched to that index
 */
fun Player.switchTrack(trackType: @C.TrackType Int, trackIndex: Int) {
  val trackTypeText = when (trackType) {
    C.TRACK_TYPE_AUDIO -> "audio"
    C.TRACK_TYPE_TEXT -> "subtitle"
    else -> throw IllegalArgumentException("Invalid track type: $trackType")
  }

  if (trackIndex < 0) {
    Timber.d("Disabling $trackTypeText")
    trackSelectionParameters = trackSelectionParameters
      .buildUpon()
      .setTrackTypeDisabled(trackType, true)
      .build()
  } else {
    val tracks = currentTracks.groups.filter { it.type == trackType }

    if (tracks.isEmpty() || trackIndex >= tracks.size) {
      Timber.d("Operation failed: Invalid track index: $trackIndex")
      return
    }

    Timber.d("Setting $trackTypeText track: $trackIndex")
    val trackSelectionOverride = TrackSelectionOverride(tracks[trackIndex].mediaTrackGroup, 0)

    // Override the track selection parameters to force the selection of the specified track.
    trackSelectionParameters = trackSelectionParameters
      .buildUpon()
      .setTrackTypeDisabled(trackType, false)
      .setOverrideForType(trackSelectionOverride)
      .build()
  }
}

/**
 * Sets the seek parameters for the player.
 *
 * @param seekParameters The seek parameters to set.
 */
@UnstableApi
fun Player.setSeekParameters(seekParameters: SeekParameters) {
  when (this) {
    is ExoPlayer -> this.setSeekParameters(seekParameters)
  }
}

/**
 * Seeks to the specified position.
 *
 * @param positionMs The position to seek to, in milliseconds.
 * @param shouldFastSeek Whether to seek to the nearest keyframe.
 */
@UnstableApi
fun Player.seekBack(positionMs: Long, shouldFastSeek: Boolean = false) {
  setSeekParameters(if (shouldFastSeek) SeekParameters.PREVIOUS_SYNC else SeekParameters.DEFAULT)
  this.seekTo(positionMs)
}

/**
 * Seeks to the specified position.
 *
 * @param positionMs The position to seek to, in milliseconds.
 * @param shouldFastSeek Whether to seek to the nearest keyframe.
 */
@UnstableApi
fun Player.seekForward(positionMs: Long, shouldFastSeek: Boolean = false) {
  setSeekParameters(if (shouldFastSeek) SeekParameters.NEXT_SYNC else SeekParameters.DEFAULT)
  this.seekTo(positionMs)
}

fun Player.addAdditionalSubtitleConfiguration(subtitles: List<MediaItem.SubtitleConfiguration>): Int? {
  val currentMediaItem = currentMediaItem ?: return null
  val existingSubConfigurations =
    currentMediaItem.localConfiguration?.subtitleConfigurations.orEmpty()

  val filerSubtitles = subtitles.filterNot { subtitle ->
    existingSubConfigurations.any { it.id == subtitle.id }
  }

  val subtitleConfigurations = existingSubConfigurations + filerSubtitles

  val updatedMediaItem = currentMediaItem
    .buildUpon()
    .setSubtitleConfigurations(subtitleConfigurations)
    .build()

  val index = currentMediaItemIndex
  addMediaItem(index + 1, updatedMediaItem)
  removeMediaItem(index)

  return if (filerSubtitles.isEmpty()) return null else subtitleConfigurations.indexOf(filerSubtitles[0])
}
