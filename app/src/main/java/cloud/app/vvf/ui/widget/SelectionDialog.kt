package cloud.app.vvf.ui.widget

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
import cloud.app.vvf.databinding.DialogBottomSelectionBinding
import cloud.app.vvf.utils.autoCleared
import cloud.app.vvf.utils.dismissSafe
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectionDialog : DockingDialog() {
  private var binding by autoCleared<DialogBottomSelectionBinding>()
  private val viewModel by activityViewModels<SelectionViewModel>()
  private val args by lazy { requireArguments() }
  private val items by lazy { args.getStringArrayList(ARG_ITEMS) ?: emptyList() }
  private val selectedIndex by lazy { args.getIntegerArrayList(ARG_SELECTED_INDEX) ?: emptyList() }
  private val name by lazy { args.getString(ARG_NAME, "") }
  private val showApply by lazy { args.getBoolean(ARG_SHOW_APPLY, false) }
  private val isMultiSelect by lazy { args.getBoolean(ARG_IS_MULTI_SELECT, false) }
  private val itemLayout by lazy { args.getInt(ARG_ITEM_LAYOUT, R.layout.sort_bottom_single_choice) }

  companion object {
    private const val ARG_ITEMS = "ARG_ITEMS"
    private const val ARG_SELECTED_INDEX = "ARG_SELECTED_INDEX"
    private const val ARG_NAME = "ARG_NAME"
    private const val ARG_SHOW_APPLY = "ARG_SHOW_APPLY"
    private const val ARG_IS_MULTI_SELECT = "ARG_IS_MULTI_SELECT"
    private const val ARG_ITEM_LAYOUT = "ARG_ITEM_LAYOUT"

    fun single(
      items: List<String>,
      selectedIndex: Int,
      name: String,
      showApply: Boolean,
      dismissCallback: () -> Unit,
      callback: (Int) -> Unit
    ): SelectionDialog {
      return newInstance(
        items,
        listOf(selectedIndex),
        name,
        showApply,
        false,
        R.layout.sort_bottom_single_choice
      ).apply {
        dismissCallbackSetter = dismissCallback
        callbackSetter = { if (it.isNotEmpty()) callback(it.first()) }
      }
    }

    fun multiple(
      items: List<String>,
      selectedIndex: List<Int>,
      name: String,
      dismissCallback: () -> Unit,
      callback: (List<Int>) -> Unit
    ): SelectionDialog {
      return newInstance(items, selectedIndex, name, true, true, R.layout.sort_bottom_single_choice).apply {
        dismissCallbackSetter = dismissCallback
        callbackSetter = callback
      }
    }

    private fun newInstance(
      items: List<String>,
      selectedIndex: List<Int>,
      name: String,
      showApply: Boolean,
      isMultiSelect: Boolean,
      itemLayout: Int
    ) = SelectionDialog().apply {
      arguments = Bundle().apply {
        putStringArrayList(ARG_ITEMS, ArrayList(items))
        putIntegerArrayList(ARG_SELECTED_INDEX, ArrayList(selectedIndex))
        putString(ARG_NAME, name)
        putBoolean(ARG_SHOW_APPLY, showApply)
        putBoolean(ARG_IS_MULTI_SELECT, isMultiSelect)
        putInt(ARG_ITEM_LAYOUT, itemLayout)
      }
    }
  }

  private var dismissCallbackSetter: (() -> Unit)? = null
  private var callbackSetter: ((List<Int>) -> Unit)? = null

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View {
    binding = DialogBottomSelectionBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    val context = this.context ?: return

    val realShowApply = showApply || isMultiSelect
    binding.apply {
      applyBttHolder.isVisible = realShowApply

      if (!realShowApply) {
        (listview1.layoutParams as? LinearLayout.LayoutParams)?.let { params ->
          params.bottomMargin = 0
          listview1.layoutParams = params
        }
      }

      text1.text = name
      text1.isGone = name.isBlank()

      val arrayAdapter = ArrayAdapter(context, itemLayout, items)
      listview1.adapter = arrayAdapter
      listview1.choiceMode = if (isMultiSelect) AbsListView.CHOICE_MODE_MULTIPLE else AbsListView.CHOICE_MODE_SINGLE

      selectedIndex.forEach { index ->
        listview1.setItemChecked(index, true)
      }

      selectedIndex.minOrNull()?.let {
        listview1.setSelection(it)
      }

      dialog?.setOnDismissListener {
        dismissCallbackSetter?.invoke()
      }

      listview1.setOnItemClickListener { _, _, which, _ ->
        if (realShowApply) {
          if (!isMultiSelect) listview1.setItemChecked(which, true)
        } else {
          callbackSetter?.invoke(listOf(which))
          dialog?.dismissSafe(activity)
        }
      }

      if (realShowApply) {
        applyBtt.setOnClickListener {
          val selectedItems = (0 until listview1.count).filter {
            listview1.checkedItemPositions[it]
          }
          callbackSetter?.invoke(selectedItems)
          dialog?.dismissSafe(activity)
        }

        cancelBtt.setOnClickListener {
          dialog?.dismissSafe(activity)
        }
      }
    }
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    // Dismiss the dialog when a configuration change (e.g., rotation) is detected
    dialog?.dismissSafe(activity)
  }
}
