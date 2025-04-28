package cloud.app.vvf.features.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import androidx.media3.common.C
import androidx.media3.common.C.TrackType
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import cloud.app.vvf.R
import cloud.app.vvf.databinding.DialogAudioTrackSelectionBinding
import cloud.app.vvf.features.player.utils.getName
import cloud.app.vvf.ui.widget.dialog.DockingDialog
import cloud.app.vvf.utils.UIHelper.hideSystemUI
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.dismissSafe

@UnstableApi
class AudioVideoTrackSelectionDialog(
  private val tracks: Tracks,
  private val trackType: @TrackType Int,
  private val onTrackSelected: (trackIndex: Int) -> Unit
) : DockingDialog() {

  private var binding by autoCleared<DialogAudioTrackSelectionBinding>()

  override val widthPercentage: Float = 0.5f

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = DialogAudioTrackSelectionBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupDialog()
  }

  private fun setupDialog() {
    val context = context ?: return // Early return if context is null

    // Validate track type
    require(trackType == C.TRACK_TYPE_VIDEO || trackType == C.TRACK_TYPE_AUDIO) {
      "Invalid track type: $trackType"
    }

    // Filter supported tracks
    val trackGroups = tracks.groups.filter { it.type == trackType && it.isSupported }
    if (trackGroups.isEmpty()) {
      binding.text1.text = context.getString(
        if (trackType == C.TRACK_TYPE_AUDIO) R.string.no_audio_tracks_found
        else R.string.no_video_tracks_found
      )
      return
    }

    // Determine selected track index
    val selectedTrackIndex = trackGroups.indexOfFirst { it.isSelected }.takeIf { it != -1 }
      ?: trackGroups.size

    // Prepare track names with "Disable" option
    val trackNames = trackGroups.mapIndexed { index, trackGroup ->
      trackGroup.mediaTrackGroup.getName(trackType, index)
    }.toMutableList().apply {
      if (trackType != C.TRACK_TYPE_VIDEO) add(context.getString(R.string.disable))
    }.toTypedArray()

    // Setup UI
    with(binding) {
      text1.text = context.getString(
        if (trackType == C.TRACK_TYPE_AUDIO) R.string.select_audio_track
        else R.string.select_video_track
      )
      listview1.apply {
        adapter = ArrayAdapter(
          context,
          R.layout.sort_bottom_single_choice,
          trackNames
        )
        choiceMode = AbsListView.CHOICE_MODE_SINGLE
        setSelection(selectedTrackIndex)
        setItemChecked(selectedTrackIndex, true)
        setOnItemClickListener { _, _, position, _ ->
          onTrackSelected(position.takeIf { it < trackGroups.size } ?: -1)
          dismissSafe()
        }
      }
      cancelBtt.setOnClickListener { dismissSafe() }
    }
  }

  override fun onDestroyView() {
    activity?.hideSystemUI()
    super.onDestroyView()
  }

  private fun dismissSafe() {
    dialog?.dismissSafe(activity)
  }
}
