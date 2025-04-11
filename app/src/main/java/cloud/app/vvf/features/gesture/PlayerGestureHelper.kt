package cloud.app.vvf.features.gesture

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.res.Configuration
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import cloud.app.vvf.R
import cloud.app.vvf.common.utils.millisecondsToReadable
import cloud.app.vvf.common.utils.millisecondsToReadableWithSign
import cloud.app.vvf.features.player.PlayerFragment
import cloud.app.vvf.features.player.PlayerFragment.Companion.HIDE_DELAY_MILLIS
import cloud.app.vvf.features.player.PlayerViewModel
import cloud.app.vvf.features.player.seekBack
import cloud.app.vvf.features.player.seekForward
import cloud.app.vvf.utils.toDp
import cloud.app.vvf.utils.toPx
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt


class PlayerGestureHelper(
  private val fragment: PlayerFragment,
  private val volumeManager: VolumeManager,
  private val brightnessManager: BrightnessManager,
) {
  private val viewModel by lazy { fragment.viewModel }
  private val useZoomControls by lazy {
    viewModel.preference.getBoolean(
      fragment.getString(R.string.pref_zoom_control),
      true
    )
  }
  private val useSeekControls by lazy {
    viewModel.preference.getBoolean(
      fragment.getString(R.string.pref_use_seek_control),
      true
    )
  }
  private val useSwipeControls by lazy {
    viewModel.preference.getBoolean(
      fragment.getString(R.string.pref_use_swipe_control),
      true
    )
  }
  private val seekSpeed by lazy {
    viewModel.preference.getLong(
      fragment.getString(R.string.pref_seek_speed),
      20
    )
  }
  private val playerView by lazy { fragment.binding.playerView }
  private val binding by lazy { fragment.binding }

  private val shouldFastSeek: Boolean = true;
  private var hideVolumeIndicatorJob: Job? = null
  private var hideBrightnessIndicatorJob: Job? = null
  private var hideInfoLayoutJob: Job? = null

  @UnstableApi
  private var exoContentFrameLayout: AspectRatioFrameLayout =
    playerView.findViewById(R.id.exo_content_frame)

  private var currentGestureAction: GestureAction? = null
  private var seekStart = 0L
  private var position = 0L
  private var seekChange = 0L
  private var pointerCount = 1
  private var isPlayingOnSeekStart: Boolean = false
  private var currentPlaybackSpeed: Float? = null

  private val tapGestureDetector = GestureDetector(
    playerView.context,
    object : GestureDetector.SimpleOnGestureListener() {
      override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        with(playerView) {
          @UnstableApi
          fragment.animateLayoutChanges(!isControllerFullyVisible, true)
        }
        return true
      }


      override fun onDoubleTap(event: MotionEvent): Boolean {
        if (viewModel.isControlsLocked.value) return false

        playerView.player?.run {
          val viewCenterX = playerView.measuredWidth / 2
          if (event.x.toInt() < viewCenterX) {
            val newPosition = currentPosition - seekSpeed * 1000
            seekBack(newPosition.coerceAtLeast(0), shouldFastSeek)
          } else {
            val newPosition = currentPosition + seekSpeed * 1000
            seekForward(newPosition.coerceAtMost(duration), shouldFastSeek)
          }
        } ?: return false
        return true
      }
    },
  )

  private val seekGestureDetector = GestureDetector(
    playerView.context,
    object : GestureDetector.SimpleOnGestureListener() {
      override fun onScroll(
        firstEvent: MotionEvent?,
        currentEvent: MotionEvent,
        distanceX: Float,
        distanceY: Float,
      ): Boolean {
        if (firstEvent == null) return false
        if (inExclusionArea(firstEvent)) return false
        if (!useSeekControls) return false
        if (viewModel.isControlsLocked.value) return false
       // if (!viewModel.isMediaItemReady.value) return false
        if (abs(distanceX / distanceY) < 2) return false

        if (currentGestureAction == null) {
          seekChange = 0L
          seekStart = playerView.player?.currentPosition ?: 0L
          @UnstableApi
          playerView.controllerAutoShow = playerView.isControllerFullyVisible
          if (playerView.player?.isPlaying == true) {
            playerView.player?.pause()
            isPlayingOnSeekStart = true
          }
          currentGestureAction = GestureAction.SEEK
        }
        if (currentGestureAction != GestureAction.SEEK) return false

        val distanceDiff = abs(distanceX.toDp / 4).coerceIn(0.5f, 10f)
        val change = (distanceDiff * SEEK_STEP_MS).toLong()

        playerView.player?.run {
          if (distanceX < 0L) {
            seekChange = (seekChange + change)
              .takeIf { it + seekStart < duration } ?: (duration - seekStart)
            position = (seekStart + seekChange).coerceAtMost(duration)
            seekForward(positionMs = position, shouldFastSeek = shouldFastSeek)
          } else {
            seekChange = (seekChange - change)
              .takeIf { it + seekStart > 0 } ?: (0 - seekStart)
            position = seekStart + seekChange
            seekBack(positionMs = position, shouldFastSeek = shouldFastSeek)
          }
          showPlayerInfo(
            info = this.currentPosition.millisecondsToReadable() ?: "",
            subInfo = "[${seekChange.millisecondsToReadableWithSign()}]",
          )
          return true
        }
        return false
      }
    },
  )

  private val volumeAndBrightnessGestureDetector = GestureDetector(
    playerView.context,
    object : GestureDetector.SimpleOnGestureListener() {
      override fun onScroll(
        firstEvent: MotionEvent?,
        currentEvent: MotionEvent,
        distanceX: Float,
        distanceY: Float,
      ): Boolean {
        if (firstEvent == null) return false
        if (inExclusionArea(firstEvent)) return false
        if (!useSwipeControls) return false
        if (viewModel.isControlsLocked.value) return false
        if (abs(distanceY / distanceX) < 2) return false

        if (currentGestureAction == null) {
          currentGestureAction = GestureAction.SWIPE
        }
        if (currentGestureAction != GestureAction.SWIPE) return false

        val viewCenterX = playerView.measuredWidth / 2
        val distanceFull = playerView.measuredHeight * FULL_SWIPE_RANGE_SCREEN_RATIO
        val ratioChange = distanceY / distanceFull

        if (firstEvent.x.toInt() > viewCenterX) {
          val change = ratioChange * volumeManager.maxStreamVolume
          volumeManager.setVolume(
            volumeManager.currentVolume + change,
            true
          )
          showVolumeGestureLayout()
        } else {
          val change = ratioChange * brightnessManager.maxBrightness
          brightnessManager.setBrightness(brightnessManager.currentBrightness + change)
          showBrightnessGestureLayout()
        }
        return true
      }
    },
  )

  private val zoomGestureDetector = ScaleGestureDetector(
    playerView.context,
    object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
      private val SCALE_RANGE = 0.25f..4.0f

      override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        if (!useZoomControls || viewModel.isControlsLocked.value) return false
        currentGestureAction = GestureAction.ZOOM
        return true
      }
      @UnstableApi
      override fun onScale(detector: ScaleGestureDetector): Boolean {
        if (!useZoomControls || viewModel.isControlsLocked.value) return false
        if (currentGestureAction != GestureAction.ZOOM) return false

        playerView.player?.videoSize?.let { videoSize ->
          // Get current scale
          val currentScale = exoContentFrameLayout.scaleX
          val scaleFactor = currentScale * detector.scaleFactor

          // Calculate video scale based on orientation
          val isLandscape = playerView.context.resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE
          val referenceDimension = if (isLandscape)
            videoSize.width.toFloat()
          else
            videoSize.height.toFloat()
          val viewDimension = if (isLandscape)
            exoContentFrameLayout.width.toFloat()
          else
            exoContentFrameLayout.height.toFloat()

          val updatedVideoScale = (viewDimension * scaleFactor) / referenceDimension

          if (updatedVideoScale in SCALE_RANGE) {
            exoContentFrameLayout.apply {
              // Set pivot to finger midpoint
              pivotX = detector.focusX
              pivotY = detector.focusY

              // Apply scale
              scaleX = scaleFactor
              scaleY = scaleFactor
            }

            fragment.onScaleChanged(scaleFactor)

            // Show zoom percentage using the appropriate dimension
            val currentVideoScale = (viewDimension * scaleFactor) / referenceDimension
            showPlayerInfo("${(currentVideoScale * 100).roundToInt()}%")
          }
        }
        return true
      }

      override fun onScaleEnd(detector: ScaleGestureDetector) {
        currentGestureAction = null
      }
    }
  )

//    // View setup
//    init {
//        playerView.setOnTouchListener { _, event ->
//            zoomGestureDetector.onTouchEvent(event)
//            true
//        }
//        // Ensure the view can receive touch events
//        playerView.isFocusable = true
//        playerView.isFocusableInTouchMode = true
//    }

  private fun releaseGestures() {
    // hide the volume indicator
    hideVolumeGestureLayout()
    // hide the brightness indicator
    hideBrightnessGestureLayout()
    // hide info layout
    hidePlayerInfo(0L)
    // hides fast playback top info layout
    hideTopInfo()

    currentPlaybackSpeed?.let {
      playerView.player?.setPlaybackSpeed(it)
      currentPlaybackSpeed = null
    }
    @UnstableApi
    playerView.controllerAutoShow = true
    if (isPlayingOnSeekStart) playerView.player?.play()
    isPlayingOnSeekStart = false
    currentGestureAction = null
  }

  fun showVolumeGestureLayout() {
    hideVolumeIndicatorJob?.cancel()
    with(binding) {
      volumeGestureLayout.visibility = View.VISIBLE
      volumeProgressBar.max = volumeManager.maxVolume.times(100)
      volumeProgressBar.progress = volumeManager.currentVolume.times(100).toInt()
      volumeProgressText.text = volumeManager.volumePercentage.toString()
    }
  }

  fun showBrightnessGestureLayout() {
    hideBrightnessIndicatorJob?.cancel()
    with(binding) {
      brightnessGestureLayout.visibility = View.VISIBLE
      brightnessProgressBar.max = brightnessManager.maxBrightness.times(100).toInt()
      brightnessProgressBar.progress = brightnessManager.currentBrightness.times(100).toInt()
      brightnessProgressText.text = brightnessManager.brightnessPercentage.toString()
    }
  }

  fun showPlayerInfo(info: String, subInfo: String? = null) {
    hideInfoLayoutJob?.cancel()
    with(binding) {
      infoLayout.visibility = View.VISIBLE
      infoText.text = info
      infoSubtext.visibility = View.GONE.takeIf { subInfo == null } ?: View.VISIBLE
      infoSubtext.text = subInfo
    }
  }

  fun showTopInfo(info: String) {
    with(binding) {
      topInfoLayout.visibility = View.VISIBLE
      topInfoText.text = info
    }
  }

  fun hideVolumeGestureLayout(delayTimeMillis: Long = HIDE_DELAY_MILLIS) {
    if (binding.volumeGestureLayout.visibility != View.VISIBLE) return
    hideVolumeIndicatorJob = fragment.lifecycleScope.launch {
      delay(delayTimeMillis)
      binding.volumeGestureLayout.visibility = View.GONE
    }
  }

  fun hideBrightnessGestureLayout(delayTimeMillis: Long = HIDE_DELAY_MILLIS) {
    if (binding.brightnessGestureLayout.visibility != View.VISIBLE) return
    hideBrightnessIndicatorJob = fragment.lifecycleScope.launch {
      delay(delayTimeMillis)
      binding.brightnessGestureLayout.visibility = View.GONE
    }
//    if (playerPreferences.rememberPlayerBrightness) {
//      viewModel.setPlayerBrightness(window.attributes.screenBrightness)
//    }
  }

  fun hidePlayerInfo(delayTimeMillis: Long = HIDE_DELAY_MILLIS) {
    if (binding.infoLayout.visibility != View.VISIBLE) return
    hideInfoLayoutJob = fragment.lifecycleScope.launch {
      delay(delayTimeMillis)
      binding.infoLayout.visibility = View.GONE
    }
  }

  fun hideTopInfo() {
    binding.topInfoLayout.visibility = View.GONE
  }

  @UnstableApi
  internal fun resetExoContentFrameWidthAndHeight() {
    exoContentFrameLayout.layoutParams.width = LayoutParams.MATCH_PARENT
    exoContentFrameLayout.layoutParams.height = LayoutParams.MATCH_PARENT
    exoContentFrameLayout.scaleX = 1.0f
    exoContentFrameLayout.scaleY = 1.0f
    exoContentFrameLayout.requestLayout()
  }

  @UnstableApi
  internal fun applyVideoScale(videoScale: Float) {
    exoContentFrameLayout.scaleX = videoScale
    exoContentFrameLayout.scaleY = videoScale
    exoContentFrameLayout.requestLayout()
  }

  /**
   * Check if [firstEvent] is in the gesture exclusion area
   */
  private fun inExclusionArea(firstEvent: MotionEvent): Boolean {
    val gestureExclusionBorder = GESTURE_EXCLUSION_AREA.toPx

    return firstEvent.y < gestureExclusionBorder || firstEvent.y > playerView.height - gestureExclusionBorder ||
      firstEvent.x < gestureExclusionBorder || firstEvent.x > playerView.width - gestureExclusionBorder
  }

  private fun setVideoScale(f: kotlin.Float) {}

  init {
    playerView.setOnTouchListener { _, motionEvent ->
      pointerCount = motionEvent.pointerCount
      when (motionEvent.pointerCount) {
        1 -> {
          tapGestureDetector.onTouchEvent(motionEvent)
          volumeAndBrightnessGestureDetector.onTouchEvent(motionEvent)
          seekGestureDetector.onTouchEvent(motionEvent)
        }

        2 -> {
          zoomGestureDetector.onTouchEvent(motionEvent)
        }
      }

      if (motionEvent.action == MotionEvent.ACTION_UP || motionEvent.pointerCount >= 3) {
        releaseGestures()
      }
      true
    }
  }

  companion object {
    const val FULL_SWIPE_RANGE_SCREEN_RATIO = 0.66f
    const val GESTURE_EXCLUSION_AREA = 20f
    const val SEEK_STEP_MS = 1000L
  }
}

inline val Int.toMillis get() = this * 1000

enum class GestureAction {
  SWIPE,
  SEEK,
  ZOOM,
  FAST_PLAYBACK,
}
