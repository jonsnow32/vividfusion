package cloud.app.vvf.features.player

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.text.format.Formatter.formatShortFileSize
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player.STATE_BUFFERING
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.databinding.CustomControllerBinding
import cloud.app.vvf.databinding.FragmentPlayerBinding
import cloud.app.vvf.features.dialogs.AudioVideoTrackSelectionDialog
import cloud.app.vvf.features.dialogs.OnlineSubtitleDialog
import cloud.app.vvf.features.dialogs.TextTrackSelectionDialog
import cloud.app.vvf.features.gesture.BrightnessManager
import cloud.app.vvf.features.gesture.PlayerGestureHelper
import cloud.app.vvf.features.gesture.VolumeManager
import cloud.app.vvf.features.player.torrent.TorrentManager
import cloud.app.vvf.features.player.torrent.TorrentPlayerViewModel
import cloud.app.vvf.features.player.utils.ResizeMode
import cloud.app.vvf.features.player.utils.getSelected
import cloud.app.vvf.network.api.torrentserver.TorrentStatus
import cloud.app.vvf.utils.UIHelper.hideSystemUI
import cloud.app.vvf.utils.UIHelper.showSystemUI
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.putSerialized
import cloud.app.vvf.utils.setDefaultFocus
import cloud.app.vvf.utils.setTextWithVisibility
import cloud.app.vvf.utils.showToast
import cloud.app.vvf.utils.toPx
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
@UnstableApi
class PlayerFragment : Fragment() {

  internal var binding by autoCleared<FragmentPlayerBinding>()
  internal var playerControlBinding by autoCleared<CustomControllerBinding>()
  internal val viewModel by viewModels<PlayerViewModel>()
  private val torrentPlayerViewModel: TorrentPlayerViewModel by viewModels()

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

    internal const val HIDE_DELAY_MILLIS = 200L
    private const val CONTROLLER_HIDE_DELAY = 2000L
    private const val UNLOCK_BTN_HIDE_DELAY = 1000L
    private const val ANIMATION_DURATION = 200L
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
    playerView = binding.playerView.also { view ->
      view.controllerHideOnTouch = false
      view.controllerAutoShow = false
      view.controllerShowTimeoutMs = 0
    }
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    mediaItems?.let { initializePlayer(it, subtitles) } ?: run {
      currentActivity.showToast(R.string.failed_loading)
      parentFragmentManager.popBackStack()
    }
  }


  private fun initializePlayer(
    mediaItems: List<AVPMediaItem>,
    subtitles: List<SubtitleData>? = null
  ) {
    setupSystemUI()

    // Ensure managers (and torrentPlayerHelper) are initialized before any usage
    setupManagers()
    // --- Begin torrent/magnet integration ---
    val mediaItem = mediaItems.getOrNull(currentMediaIdx)
    if (mediaItem != null) {
      val url = when (mediaItem) {
        is AVPMediaItem.VideoItem -> mediaItem.video.uri
        is AVPMediaItem.TrackItem -> mediaItem.track.uri
        else -> null
      }
      if (url != null && (url.startsWith("magnet:") || url.endsWith(
          ".torrent",
          ignoreCase = true
        ))
      ) {
        if (!TorrentManager.hasAcceptedTorrentForThisSession) {
          showTorrentConsentDialog(mediaItems, subtitles)
          return
        }
      }
      lifecycleScope.launch {
        val transformedMediaItem = torrentPlayerViewModel.processMediaItem(mediaItem)
        if (transformedMediaItem != null) {
          continuePlayerInit(listOf(transformedMediaItem), subtitles)
        } else {
          continuePlayerInit(mediaItems, subtitles)
        }
      }
    } else {
      continuePlayerInit(mediaItems, subtitles)
    }
  }

  private fun continuePlayerInit(
    mediaItems: List<AVPMediaItem>,
    subtitles: List<SubtitleData>? = null
  ) {
    viewModel.initialize(
      requireActivity(),
      mediaItems = mediaItems,
      selectedMediaIdx = currentMediaIdx,
      subtitles = subtitles ?: emptyList(),
      selectedSubtitleIdx = currentSubtitleIdx,
      initialPosition = viewModel.playbackPositionMs.value,
      subtitleOffsetMs = 0L
    )

    binding.playerView.player = viewModel.player
    if (playerView?.player != null) {
      playerView?.defaultArtwork = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_music)
    }

    setupManagers()
    setupControls()
    observeViewModel()
  }

  private fun setupSystemUI() {
    currentActivity.hideSystemUI(viewModel.fullscreenNotch.value)
  }

  private fun setupManagers() {
    val audioManager = currentActivity.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Initialize torrent support using ViewModel
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
    binding.resetScale.setOnClickListener {
      playerGestureHelper.applyVideoScale(1.0f)
      binding.resetScale.playFadeAnimation(false)
      binding.resetScale.moveTopAnimation(false)
    }
  }

  private fun CustomControllerBinding.setupControlButtons() {
    playerGoBack.setOnClickListener { parentFragmentManager.popBackStack() }
    playerRestart.setOnClickListener { viewModel.seekTo(0); viewModel.play() }
    playerForward.setOnClickListener { viewModel.seekToNext() }
    btnResize.setOnClickListener { viewModel.toggleResizeMode() }
    btnRotate.setOnClickListener { rotateScreen() }
    btnAudioTrack.setOnClickListener {
      AudioVideoTrackSelectionDialog(
        viewModel.player?.currentTracks ?: return@setOnClickListener,
        C.TRACK_TYPE_AUDIO
      ) {
        viewModel.selectAudioTrack(it)
      }.show(parentFragmentManager, "AudioTrackSelectionDialog")
    }
    btnVideoTrack.setOnClickListener {
      AudioVideoTrackSelectionDialog(
        viewModel.player?.currentTracks ?: return@setOnClickListener,
        C.TRACK_TYPE_VIDEO
      ) {
        viewModel.selectVideoTrack(it)
      }.show(parentFragmentManager, "VideoTrackSelectionDialog")
    }
    btnTextTrack.setOnClickListener {
      TextTrackSelectionDialog(
        viewModel.player?.currentTracks ?: return@setOnClickListener,
        onTrackSelected = {
          viewModel.selectTextTrack(it)
        },
        openLocalSubtitle = {
          subtitleFileLauncherLaunchedForMediaItem = playerView?.player?.currentMediaItem
          subtitleFileLauncher.launch(
            arrayOf(
              MimeTypes.APPLICATION_SUBRIP,
              MimeTypes.APPLICATION_TTML,
              MimeTypes.TEXT_VTT,
              MimeTypes.TEXT_SSA,
              MimeTypes.BASE_TYPE_APPLICATION + "/octet-stream",
              MimeTypes.BASE_TYPE_TEXT + "/*"
            )
          )
        },
        openOnlineSubtitle = {
          OnlineSubtitleDialog.newInstance(
            viewModel.mediaMetaData.value?.title.toString(),
            mediaItems?.get(currentMediaIdx),
            viewModel.tracks.value?.groups?.filter { it.type == C.TRACK_TYPE_TEXT }
              ?.map { trackGroup ->
                trackGroup.mediaTrackGroup.getFormat(0).id
              }?.filterNotNull(),
            arrayOf("en", "vi")
          ).show(parentFragmentManager) {
            val selectedItems = it?.getSerialized<List<SubtitleData>>("selected_items")
            selectedItems?.let { it1 -> viewModel.addSubtitleData(requireContext(), it1) }
          }
        },
        openSubtitleOffsetDialog = {
          animateLayoutChanges(false, fromUser = true)
          binding.subtitleOffsetView.apply {
            isGone = false
            viewModel.parseSubtitles(context) { result ->
              lifecycleScope.launch(Dispatchers.Main) {
                if (result) {
                  viewModel.registerUpdateProgress()
                  initialize(
                    viewModel.cuesWithTiming.value,
                    viewModel.player?.currentPosition ?: 0L,
                    viewModel.delayedFactory.getDelayMs()
                  ) { offset ->
                    viewModel.updateSubtitleOffset(context, offset)
                  }
                } else {
                  context?.showToast(R.id.no_subtitles_loaded_notice)
                }
              }
            }
          }
        }
      ).show(parentFragmentManager, "TextTrackSelectionDialog")
    }
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
      resize(ResizeMode.entries[it ?: 0], it != null)
    }

    observe(viewModel.requestedOrientation) { orientation ->
      if (orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
        currentActivity.requestedOrientation = orientation
      }
    }

    playerControlBinding.playPauseToggle
      .addOnCheckedStateChangedListener { checkBox, state ->
        if (state == MaterialCheckBox.STATE_CHECKED) {
          viewModel.play()
        } else {
          viewModel.pause()
        }
      }

    observe(viewModel.isPlaying) { isPlaying ->
      playerView?.keepScreenOn = isPlaying
      playerControlBinding.run {
        playPauseToggle.isChecked = isPlaying
      }
      restartHideControllerTimeout()
    }

    observe(viewModel.mediaMetaData) {
      playerControlBinding.playerVideoTitle.setTextWithVisibility(it?.title.toString())
    }

    observe(viewModel.videoSize) {
      if (it != null) {
        playerControlBinding.playerVideoSize.setTextWithVisibility("${it.width}x${it.height}")
      }
    }

    observe(viewModel.playbackState) {
      if (it == STATE_ENDED || it == STATE_IDLE) {
        animateLayoutChanges(true, fromUser = false)
      }
      val buffering = (it == STATE_BUFFERING)
      binding.playingIndicator.alpha = if (buffering) 1f else 0f
      playerControlBinding.playPauseToggle.alpha = if (buffering) 0f else 1f
    }

//    observe(viewModel.playbackPositionMs) {
//      binding.subtitleOffsetView.updateAdapterCues(it)
//    }

    observe(torrentPlayerViewModel.torrentStatus) {
      if (it == null) {
        playerControlBinding.torrent.isGone = true
      } else {
        playerControlBinding.torrent.isGone = false
        showDownloadProgress(it)
      }
    }
    observe(viewModel.tracks) { tracks ->
      if (tracks == null) return@observe

      val audios = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
      val videos = tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
      val subtitles = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }.getSelected()

      playerControlBinding.btnAudioTrack.isGone = audios.isEmpty()
      playerControlBinding.btnVideoTrack.isGone = videos.size < 2
    }
  }


  private fun resize(mode: ResizeMode, showToast: Boolean) {
    playerGestureHelper.resetExoContentFrameWidthAndHeight()
    playerView?.resizeMode = when (mode) {
      ResizeMode.Fill -> AspectRatioFrameLayout.RESIZE_MODE_FILL
      ResizeMode.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
      ResizeMode.Zoom -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }
    if (showToast) currentActivity.showToast(mode.nameRes)
    binding.resetScale.playFadeAnimation(false)
    binding.resetScale.moveTopAnimation(false)

    val context = context ?: return@resize

    when (mode.ordinal) {
      0 -> playerControlBinding.btnResize.setImageDrawable(
        ContextCompat.getDrawable(
          context,
          R.drawable.round_fit_screen_24
        )
      )

      1 -> playerControlBinding.btnResize.setImageDrawable(
        ContextCompat.getDrawable(
          context,
          R.drawable.round_aspect_ratio_24
        )
      )

      2 -> playerControlBinding.btnResize.setImageDrawable(
        ContextCompat.getDrawable(
          context,
          R.drawable.round_crop_landscape_24
        )
      )

      else -> {

      }
    }
  }

  internal fun restartHideControllerTimeout() {
    hideControllerJob?.cancel()
    hideControllerJob = viewLifecycleOwner.lifecycleScope.launch {
      delay(CONTROLLER_HIDE_DELAY)
      if (playerView?.player?.isPlaying == true) {
        animateLayoutChanges(false)
      }
    }
  }

  fun animateLayoutChanges(isShow: Boolean, fromUser: Boolean = false) {
    if (!isAdded || view == null) {
      return // Exit early if Fragment is not added, view is null, or binding is cleared
    }

    if (fromUser && viewModel.isControlsLocked.value) {
      lockControls(true)
      return
    }

    playerView?.apply {
      if (isShow) {
        showController()
        if (TorrentManager.hasAcceptedTorrentForThisSession)
          torrentPlayerViewModel.startStatusPolling()
      } else {
        postDelayed({
          hideController()
        }, HIDE_DELAY_MILLIS)
        if (TorrentManager.hasAcceptedTorrentForThisSession)
          torrentPlayerViewModel.stopStatusPolling()
      }
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

  @SuppressLint("SetTextI18n")
  fun onScaleChanged(scale: Float) {
    binding.infoText.text = "${(scale * 100).toInt()}%"
    binding.resetScale.playFadeAnimation(scale != 1.0f)
    binding.resetScale.moveTopAnimation(scale != 1.0f)

  }

  private var subtitleFileLauncherLaunchedForMediaItem: MediaItem? = null
  private val subtitleFileLauncher = registerForActivityResult(OpenDocument()) { uri ->
    val activity = activity ?: return@registerForActivityResult
    if (uri != null && subtitleFileLauncherLaunchedForMediaItem != null) {
      activity.contentResolver.takePersistableUriPermission(
        uri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION
      )
      viewModel.addSubtitleUri(activity, listOf(uri))
    }
  }

  override fun onResume() {
    super.onResume()
    if (viewModel.isPlaying.value) {
      viewModel.play()
    }
  }

  override fun onPause() {
    viewModel.pause()
    super.onPause()
  }

  override fun onDestroyView(){
    playerView = null
    currentActivity.showSystemUI()
    hideControllerJob?.cancel()
    hideUnlockBtnJob?.cancel()
    // Clean up torrent resources
    super.onDestroyView()
  }

  private fun showDownloadProgress(status: TorrentStatus) {
    status.torrentSize ?: return
    status.bytesRead ?: return
    status.downloadSpeed ?: return

    activity?.runOnUiThread {

      playerControlBinding.playerVideoTitle.setTextWithVisibility(status.name ?: status.title)

      playerControlBinding.downloadedProgress.apply {
        val indeterminate = status.torrentSize <= 0 || status.bytesRead <= 0
        isIndeterminate = indeterminate
        if (!indeterminate) {
          max = (status.torrentSize / 1000).toInt()
          progress = (status.bytesRead / 1000).toInt()
        }
      }

      playerControlBinding.downloadedProgressText.setText(
        String.format(
          resources.getString(R.string.download_size_format),
          formatShortFileSize(
            context,
            status.bytesRead
          ),
          formatShortFileSize(context, status.torrentSize)
        )
      )
      val downloadSpeed =
        formatShortFileSize(context, status.downloadSpeed.toLong())
      playerControlBinding.downloadedProgressSpeedText.text =
        // todo string fmt
        status.activePeers?.let { connections ->
          "%s/s - %d Connections".format(downloadSpeed, connections)
        } ?: downloadSpeed

      // don't display when done
      playerControlBinding.downloadedProgressSpeedText.isGone =
        status.bytesRead != 0L && status.bytesRead - 1024 >= status.torrentSize
    }
  }

  fun showTorrentConsentDialog(
    mediaItems: List<AVPMediaItem>,
    subtitles: List<SubtitleData>? = null
  ) {
    val context = context ?: return
    MaterialAlertDialogBuilder(context)
      .setTitle(R.string.torrent_warning)
      .setMessage(R.string.torrent_warning_description)
      .setIcon(R.drawable.outline_warning_24) // Ensure you have a warning icon in your drawable resources
      .setCancelable(false)
      .setPositiveButton(R.string.ok) { _, _ ->
        TorrentManager.hasAcceptedTorrentForThisSession = true
        initializePlayer(mediaItems, subtitles)
      }
      .setNegativeButton(R.string.back) { _, _ ->
        TorrentManager.hasAcceptedTorrentForThisSession = false
        parentFragmentManager.popBackStack()
      }
      .setOnCancelListener {
        val errorMessage = context.getString(R.string.torrent_not_accepted)
        context.showToast(errorMessage)
      }
      .show().setDefaultFocus()
  }

  fun handleKeyDown(keyCode: Int, event: KeyEvent?): Boolean = false
  fun handleKeyUp(keyCode: Int, event: KeyEvent?): Boolean = false
}
