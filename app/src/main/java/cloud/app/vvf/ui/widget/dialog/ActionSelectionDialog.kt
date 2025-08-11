package cloud.app.vvf.ui.widget.dialog

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import cloud.app.vvf.R
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.databinding.DialogBottomActionSelectionBinding
import cloud.app.vvf.databinding.DialogBottomSelectionBinding
import cloud.app.vvf.ui.widget.dialog.actionOption.IconTextAdapter
import cloud.app.vvf.ui.widget.dialog.actionOption.IconTextItem
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.dismissSafe
import cloud.app.vvf.utils.getSerialized
import cloud.app.vvf.utils.putSerialized
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ActionSelectionDialog(private val callback: (IconTextItem) -> Unit) : DockingDialog() {


  private var binding by autoCleared<DialogBottomActionSelectionBinding>()
  private val args by lazy { requireArguments() }
  private val items by lazy { args.getSerialized<List<IconTextItem>>(ARG_ITEMS) ?: emptyList() }
  private val name by lazy { args.getString(ARG_NAME, "") }

  companion object {
    private const val ARG_ITEMS = "ARG_ITEMS"
    private const val ARG_SELECTED_INDEX = "ARG_SELECTED_INDEX"
    private const val ARG_NAME = "ARG_NAME"
    fun newInstance(
      items: List<IconTextItem>,
      name: String,
      callback : (IconTextItem) ->  Unit
    ) = ActionSelectionDialog(callback).apply {
      arguments = Bundle().apply {
        putSerialized(ARG_ITEMS, items)
        putString(ARG_NAME, name)
      }
    }
  }


  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = DialogBottomActionSelectionBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val context = this.context ?: return
    binding.apply {

      text1.text = name
      text1.isGone = name.isBlank()

      val arrayAdapter = IconTextAdapter(context, items)
      listview1.adapter = arrayAdapter
      listview1.choiceMode = AbsListView.CHOICE_MODE_SINGLE

      listview1.setOnItemClickListener { _, _, which, _ ->
        callback(items[which])
        dialog?.dismissSafe(activity)
      }

    }
  }
}
