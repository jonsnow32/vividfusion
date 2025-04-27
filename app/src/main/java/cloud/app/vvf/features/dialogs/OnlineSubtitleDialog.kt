package cloud.app.vvf.features.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.databinding.DialogOnlineSubtitleBinding
import cloud.app.vvf.ui.main.search.QuickSearchViewHolder
import cloud.app.vvf.ui.widget.dialog.DockingDialog
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.dismissSafe
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.observe
import cloud.app.vvf.utils.putSerialized
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnlineSubtitleDialog : DockingDialog() {
  override val widthPercentage: Float
    get() = 1.0f
  val viewmodel by viewModels<SubtitleViewModel>()
  private var binding by autoCleared<DialogOnlineSubtitleBinding>()
  private val query by lazy { arguments?.getString("query") }
  private val avpMediaItem by lazy { arguments?.getSerialized<AVPMediaItem>("avpMediaItem") }
  val adapter by lazy {
    ArrayAdapter(
      requireContext(),
      R.layout.sort_bottom_single_choice,
      mutableListOf<SubtitleData>()
    )
  }

  companion object {
    fun newInstance(query: String?, avpMediaItem: AVPMediaItem?) =
      OnlineSubtitleDialog().apply {
        arguments = Bundle().apply {
          putSerialized("avpMediaItem", avpMediaItem)
          putString("query", query)
        }
      }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = DialogOnlineSubtitleBinding.inflate(inflater, container, false)
    return binding.root
  }

  @UnstableApi
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val context = this.context ?: return
    binding.apply {
      listview1.adapter = adapter
      listview1.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
      listview1.setOnItemClickListener { parent, view, position, id ->
        adapter.getItem(position)?.let {
          addSubtitle(it)
        }
      }
      mainSearch.setQuery(query, true)
      applyBtt.setOnClickListener {
        dialog?.dismissSafe(activity)
      }
      cancelBtt.setOnClickListener {
        selectedItems.clear()
        dialog?.dismissSafe(activity)
      }
    }


    observe(viewmodel.subtitles) {
      adapter.clear()
      adapter.addAll(it)
      adapter.notifyDataSetChanged()
    }
    avpMediaItem?.let { viewmodel.findSubtitle(it) }
  }

  private var selectedItems = mutableListOf<SubtitleData>()
  private fun addSubtitle(subtitle: SubtitleData) {
    if (!selectedItems.contains(subtitle))
      selectedItems.add(subtitle)
  }

  override fun getResultBundle(): Bundle? {
    return if (selectedItems.isNotEmpty()) {
      Bundle().apply {
        putSerialized("selected_items", selectedItems)
      }
    } else {
      null
    }
  }
}
