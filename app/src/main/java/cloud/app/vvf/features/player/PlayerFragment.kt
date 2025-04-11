package cloud.app.vvf.features.player

import android.animation.ObjectAnimator
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.databinding.CustomControllerBinding
import cloud.app.vvf.databinding.FragmentPlayerBinding
import cloud.app.vvf.features.gesture.BrightnessManager
import cloud.app.vvf.features.gesture.PlayerGestureHelper
import cloud.app.vvf.features.gesture.VolumeManager
import cloud.app.vvf.features.player.utils.ResizeMode
import cloud.app.vvf.utils.UIHelper.hideSystemUI
import cloud.app.vvf.utils.UIHelper.showSystemUI
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.putSerialized
import cloud.app.vvf.utils.showToast
import cloud.app.vvf.utils.toPx
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlayerFragment : Fragment() {

  internal var binding by autoCleared<FragmentPlayerBinding>()
  internal var playerControlBinding by autoCleared<CustomControllerBinding>()
  internal val viewModel by viewModels<PlayerViewModel>()

  private val mediaItems by lazy { arguments?.getSerialized<List<AVPMediaItem>>("mediaItems") }
  private val currentMediaIdx by lazy { arguments?.getInt("selectedMediaIdx") ?: 0 }
  private val subtitles by lazy { arguments?.getSerialized<List<SubtitleData>>("subtitles") }
  private val currentSubtitleIdx by lazy { arguments?.getInt("selectedSubtitleIdx") ?: 0 }
  private val currentActivity by lazy { requireActivity() }

  private lateinit var volumeManager: VolumeManager
  private lateinit var brightnessManager: BrightnessManager
  private lateinit var playerGestureHelper: PlayerGestureHelper

  private var hideControllerJob: Job? = null
  private var hideUnlockBtnJob: Job? = null
  private var playerView: PlayerView? = null

  companion object {
    internal const val HIDE_DELAY_MILLIS = 200L
    private const val CONTROLLER_HIDE_DELAY = 2000L
    private const val UNLOCK_BTN_HIDE_DELAY = 1000L
    private const val ANIMATION_DURATION = 200L

    fun newInstance(
      mediaItems: List<AVPMediaItem>,
      selectedMediaIdx: Int = 0,
      subtitles: List<SubtitleData>? = null,
      selectedSubtitleIdx: Int = 0
    ) = PlayerFragment().apply {
      val bundle = bundleOf()
      bundle.putSerialized("mediaItems", mediaItems)
      bundle.putInt("selectedMediaIdx", selectedMediaIdx)
      subtitles?.let {
        bundle.putSerialized("subtitles", subtitles)
        bundle.putInt("selectedSubtitleIdx", selectedSubtitleIdx)
      }
      arguments = bundle
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentPlayerBinding.inflate(inflater, container, false)
    playerControlBinding = CustomControllerBinding.bind(
      binding.playerView.findViewById(R.id.player_holder)
    )
    @UnstableApi
    playerView = binding.playerView.also { view ->
      view.controllerHideOnTouch = false
      view.controllerAutoShow = false
      view.controllerShowTimeoutMs = 0
    }
    return binding.root
  }

  @UnstableApi
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    mediaItems?.let { initializePlayer(it, subtitles) } ?: run {
      currentActivity.showToast(R.string.failed_loading)
      parentFragmentManager.popBackStack()
    }
  }

  @UnstableApi
  private fun initializePlayer(
    mediaItems: List<AVPMediaItem>,
    subtitles: List<SubtitleData>? = null
  ) {
    // Set up system UI
    setupSystemUI()

    // Initialize player via ViewModel
    viewModel.initialize(
      mediaItems = mediaItems,
      selectedMediaIdx = currentMediaIdx,
      subtitles = subtitles ?: emptyList(),
      selectedSubtitleIdx = currentSubtitleIdx,
      initialPosition = viewModel.playbackPosition.value
    )
    binding.playerView.player = viewModel.player
    binding.playerView.player?.addListener(PlayerListener(viewModel, this))

    // Set up additional components
    setupManagers()
    setupControls()

    // Apply initial resize mode
    resize(ResizeMode.entries[viewModel.resizeMode.value], false)

    // Observe ViewModel states
    observeViewModel()
  }

  private fun setupSystemUI() {
    currentActivity.hideSystemUI(viewModel.fullscreenNotch.value)
  }

  @UnstableApi
  private fun setupManagers() {
    val audioManager = currentActivity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    volumeManager = VolumeManager(audioManager)
    brightnessManager = BrightnessManager(currentActivity)
    playerGestureHelper = PlayerGestureHelper(this, volumeManager, brightnessManager)
  }

  private fun setupControls() {
    with(playerControlBinding) {
      setupControlButtons()
      setupLockControls()
    }
    binding.btnUnlockControls.setOnClickListener { lockControls(false) }
  }

  private fun CustomControllerBinding.setupControlButtons() {
    playerGoBack.setOnClickListener { parentFragmentManager.popBackStack() }
    playerRestart.setOnClickListener { viewModel.player?.seekTo(0); viewModel.player?.play() }
    playerGoForward.setOnClickListener { viewModel.player?.seekToNext() }
    btnResize.setOnClickListener { viewModel.toggleResizeMode() }
    btnRotate.setOnClickListener { rotateScreen() }
  }

  private fun CustomControllerBinding.setupLockControls() {
    btnLockControls.setOnClickListener { lockControls(true) }
  }

  private fun rotateScreen() {
    val currentOrientation = resources.configuration.orientation
    val newOrientation = when (currentOrientation) {
      Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
      Configuration.ORIENTATION_PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }
    viewModel.requestedOrientation.value = newOrientation
  }

  private fun observeViewModel() {
    observe(viewModel.resizeMode) {
      resize(ResizeMode.entries[it], false)
    }
    observe(viewModel.requestedOrientation) { orientation ->
      if (orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
        currentActivity.requestedOrientation = orientation
      }
    }
    observe(viewModel.isPlaying) { isPlaying ->
      playerView?.keepScreenOn = isPlaying
    }

    observe(viewModel.playbackPosition) {
      // Optional: Update UI if needed based on position
    }

    observe(viewModel.playbackState) {
      if (it == STATE_ENDED || it == STATE_IDLE) {
        animateLayoutChanges(true, fromUser = false)
      }
    }

  }

  internal fun resize(mode: ResizeMode, showToast: Boolean) {
    @UnstableApi
    playerGestureHelper.resetExoContentFrameWidthAndHeight()
    @UnstableApi
    playerView?.resizeMode = when (mode) {
      ResizeMode.Fill -> AspectRatioFrameLayout.RESIZE_MODE_FILL
      ResizeMode.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
      ResizeMode.Zoom -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }
    if (showToast) currentActivity.showToast(mode.nameRes)
  }

  internal fun setOrientation(videoSize: VideoSize) {
    val requestedOrientation = viewModel.requestedOrientation.value
    if (requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
      currentActivity.requestedOrientation = requestedOrientation
    } else {
      currentActivity.requestedOrientation = when {
        videoSize.width == 0 || videoSize.height == 0 -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        videoSize.height > videoSize.width -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
      }
    }
  }

  internal fun restartHideControllerTimeout() {
    hideControllerJob?.cancel()
    hideControllerJob = viewLifecycleOwner.lifecycleScope.launch {
      delay(CONTROLLER_HIDE_DELAY)
      @UnstableApi if (playerView?.player?.isPlaying == true) {
        animateLayoutChanges(false)
      }
    }
  }

  fun animateLayoutChanges(isShow: Boolean, fromUser: Boolean = false) {

    if (fromUser && viewModel.isControlsLocked.value) {
      lockControls(true)
      return
    }

    @UnstableApi
    playerView?.apply {
      if (isShow) showController()
      else postDelayed({ hideController() }, HIDE_DELAY_MILLIS)
    }

    with(playerControlBinding) {
      playerTopHolder.moveTopAnimation(isShow)
      bottomPlayerBar.moveBottomAnimation(isShow)
      listOf(extraControls, shadowOverlay, playerCenterControls)
        .forEach { it.playFadeAnimation(isShow) }
    }

    if (isShow) restartHideControllerTimeout()
  }

  private fun lockControls(isLocked: Boolean) {
    playerControlBinding.root.visibility = if (isLocked) View.GONE else View.VISIBLE
    with(binding.btnUnlockControls) {
      visibility = if (isLocked) View.VISIBLE else View.GONE
      moveBottomAnimation(isLocked)
      playFadeAnimation(isLocked)
    }

    animateLayoutChanges(!isLocked)

    if (isLocked) {
      hideUnlockBtnJob?.cancel()
      hideUnlockBtnJob = viewLifecycleOwner.lifecycleScope.launch {
        delay(UNLOCK_BTN_HIDE_DELAY)
        binding.btnUnlockControls.apply {
          moveBottomAnimation(false)
          playFadeAnimation(false)
        }
      }
    }

    viewModel.setControlsLocked(isLocked)
  }

  private fun View.moveTopAnimation(isShow: Boolean) {
    animateTranslationY(if (isShow) 0f else -100.toPx.toFloat())
  }

  private fun View.moveBottomAnimation(isShow: Boolean) {
    animateTranslationY(if (isShow) 0f else 100.toPx.toFloat())
  }

  private fun View.animateTranslationY(target: Float) {
    ObjectAnimator.ofFloat(this, "translationY", target).apply {
      duration = ANIMATION_DURATION
      start()
    }
  }

  private fun View.playFadeAnimation(isShow: Boolean) {
    AlphaAnimation(if (isShow) 0f else 1f, if (isShow) 1f else 0f).apply {
      duration = ANIMATION_DURATION
      fillAfter = true
      startAnimation(this)
    }
  }

  fun onScaleChanged(scale: Float) {
    binding.infoText.text = "${(scale * 100).toInt()}%"
  }

  private fun applyVideoScale(videoScale: Float) {
    @UnstableApi
    playerGestureHelper.applyVideoScale(videoScale)
  }

  override fun onDestroyView() {
    playerView = null
    currentActivity.showSystemUI()
    hideControllerJob?.cancel()
    hideUnlockBtnJob?.cancel()
    super.onDestroyView()
  }

  fun handleKeyDown(keyCode: Int, event: KeyEvent?): Boolean = false
  fun handleKeyUp(keyCode: Int, event: KeyEvent?): Boolean = false
}

