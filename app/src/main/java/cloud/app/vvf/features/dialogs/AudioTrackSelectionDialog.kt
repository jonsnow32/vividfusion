package cloud.app.vvf.features.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.media3.common.C
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import cloud.app.vvf.R
import cloud.app.vvf.databinding.DialogAudioTrackSelectionBinding
import cloud.app.vvf.features.player.utils.getName
import cloud.app.vvf.ui.widget.dialog.DockingDialog
import cloud.app.vvf.utils.UIHelper.hideSystemUI
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.dismissSafe

class AudioTrackSelectionDialog(
  private val tracks: Tracks,
  private val onTrackSelected: (trackIndex: Int) -> Unit
) : DockingDialog() {
  private var binding by autoCleared<DialogAudioTrackSelectionBinding>()

  override val widthPercentage: Float
    get() = 0.5f
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = DialogAudioTrackSelectionBinding.inflate(inflater, container, false)
    return binding.root
  }
  @UnstableApi
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val context = this.context ?: return

    val audioTracks = tracks.groups
      .filter { it.type == C.TRACK_TYPE_AUDIO && it.isSupported }

    val selectedTrackIndex = audioTracks
      .indexOfFirst { it.isSelected }.takeIf { it != -1 } ?: audioTracks.size


    val trackNames = audioTracks.mapIndexed { index, trackGroup ->

      trackGroup.mediaTrackGroup.getName(C.TRACK_TYPE_AUDIO, index)
    }.toTypedArray()

    binding.apply {
      if (trackNames.isNotEmpty()) {
        text1.text = getString(R.string.select_audio_track)
        val arrayAdapter = ArrayAdapter(
          context,
          R.layout.sort_bottom_single_choice,
          arrayOf(*trackNames, getString(R.string.disable))
        )
        listview1.adapter = arrayAdapter
        listview1.choiceMode = AbsListView.CHOICE_MODE_SINGLE
        listview1.setSelection(selectedTrackIndex)
        listview1.setItemChecked(selectedTrackIndex, true)
      } else {
        text1.text = getString(R.string.no_audio_tracks_found)
      }
      listview1.setOnItemClickListener { _, _, which, _ ->
        onTrackSelected(which.takeIf { it < trackNames.size } ?: -1)
        dialog?.dismissSafe(activity)
      }
      cancelBtt.setOnClickListener {
        dialog?.dismissSafe(activity)
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    activity?.hideSystemUI()
  }
}
