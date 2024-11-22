package cloud.app.vvf.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.text.Spanned
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import cloud.app.vvf.R

import cloud.app.vvf.databinding.DialogBottomSelectionBinding
import cloud.app.vvf.databinding.DialogBottomTextBinding
import cloud.app.vvf.databinding.DialogBottomInputBinding
import cloud.app.vvf.databinding.OptionsPopupTvBinding
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber


fun Dialog?.dismissSafe(activity: Activity?) {
  if (this?.isShowing == true && activity?.isFinishing == false) {
    this.dismiss()
  }
}

private fun Activity?.showOptionSelect(
  view: View?,
  poster: String?,
  options: List<String>,
  tvOptions: List<String>,
  callback: (Pair<Boolean, Int>) -> Unit
) {
  if (this == null) return

  if (isLayout(TV or EMULATOR)) {
    val binding = OptionsPopupTvBinding.inflate(layoutInflater)
    val dialog = AlertDialog.Builder(this, R.style.AlertDialogCustom)
      .setView(binding.root)
      .create()

    dialog.show()

    binding.listview1.let { listView ->
      listView.choiceMode = AbsListView.CHOICE_MODE_SINGLE
      listView.adapter =
        ArrayAdapter<String>(this, R.layout.sort_bottom_single_choice_color).apply {
          addAll(tvOptions)
        }

      listView.setOnItemClickListener { _, _, i, _ ->
        callback.invoke(Pair(true, i))
        dialog.dismissSafe(this)
      }
    }

    binding.imageView.apply {
      isGone = poster.isNullOrEmpty()
      poster?.toImageHolder().loadInto(this)
    }
  } else {
    view?.popupMenuNoIconsAndNoStringRes(options.mapIndexed { index, s ->
      Pair(
        index,
        s
      )
    }) {
      callback(Pair(false, this.itemId))
    }
  }
}

@SuppressLint("RestrictedApi")
fun View.popupMenuNoIconsAndNoStringRes(
  items: List<Pair<Int, String>>,
  onMenuItemClick: MenuItem.() -> Unit,
): PopupMenu {
  val ctw = ContextThemeWrapper(context, R.style.PopupMenu)
  val popup =
    PopupMenu(ctw, this, Gravity.NO_GRAVITY, androidx.appcompat.R.attr.actionOverflowMenuStyle, 0)

  items.forEach { (id, string) ->
    popup.menu.add(0, id, 0, string)
  }

  (popup.menu as? MenuBuilder)?.setOptionalIconsVisible(true)

  popup.setOnMenuItemClickListener {
    it.onMenuItemClick()
    true
  }

  popup.show()
  return popup
}

fun Activity?.showOptionSelectStringRes(
  view: View?,
  poster: String?,
  options: List<Int>,
  tvOptions: List<Int> = listOf(),
  callback: (Pair<Boolean, Int>) -> Unit
) {
  if (this == null) return

  this.showOptionSelect(
    view,
    poster,
    options.map { this.getString(it) },
    tvOptions.map { this.getString(it) },
    callback
  )
}


fun Activity?.showDialog(
  binding: DialogBottomSelectionBinding,
  dialog: Dialog,
  items: List<String>,
  selectedIndex: List<Int>,
  name: String,
  showApply: Boolean,
  isMultiSelect: Boolean,
  callback: (List<Int>) -> Unit,
  dismissCallback: () -> Unit,
  itemLayout: Int = R.layout.sort_bottom_single_choice
) {
  if (this == null) return

  val realShowApply = showApply || isMultiSelect
  val listView = binding.listview1
  val textView = binding.text1
  val applyButton = binding.applyBtt
  val cancelButton = binding.cancelBtt
  val applyHolder =
    binding.applyBttHolder

  applyHolder.isVisible = realShowApply
  if (!realShowApply) {
    val params = listView.layoutParams as LinearLayout.LayoutParams
    params.setMargins(listView.marginLeft, listView.marginTop, listView.marginRight, 0)
    listView.layoutParams = params
  }

  textView.text = name
  textView.isGone = name.isBlank()

  val arrayAdapter = ArrayAdapter<String>(this, itemLayout)
  arrayAdapter.addAll(items)

  listView.adapter = arrayAdapter
  if (isMultiSelect) {
    listView.choiceMode = AbsListView.CHOICE_MODE_MULTIPLE
  } else {
    listView.choiceMode = AbsListView.CHOICE_MODE_SINGLE
  }

  for (select in selectedIndex) {
    listView.setItemChecked(select, true)
  }

  selectedIndex.minOrNull()?.let {
    listView.setSelection(it)
  }

  //  var lastSelectedIndex = if(selectedIndex.isNotEmpty()) selectedIndex.first() else -1

  dialog.setOnDismissListener {
    dismissCallback.invoke()
  }

  listView.setOnItemClickListener { _, _, which, _ ->
    //  lastSelectedIndex = which
    if (realShowApply) {
      if (!isMultiSelect) {
        listView.setItemChecked(which, true)
      }
    } else {
      callback.invoke(listOf(which))
      dialog.dismissSafe(this)
    }
  }
  if (realShowApply) {
    applyButton.setOnClickListener {
      val list = ArrayList<Int>()
      for (index in 0 until listView.count) {
        if (listView.checkedItemPositions[index])
          list.add(index)
      }
      callback.invoke(list)
      dialog.dismissSafe(this)
    }
    cancelButton.setOnClickListener {
      dialog.dismissSafe(this)
    }
  }
}

private fun Activity?.showInputDialog(
  binding: DialogBottomInputBinding,
  dialog: Dialog,
  value: String,
  name: String,
  textInputType: Int?,
  callback: (String) -> Unit,
  dismissCallback: () -> Unit
) {
  if (this == null) return

  val inputView = binding.nginxTextInput
  val textView = binding.text1
  val applyButton = binding.applyBtt
  val cancelButton = binding.cancelBtt
  val applyHolder = binding.applyBttHolder

  applyHolder.isVisible = true
  textView.text = name

  if (textInputType != null) {
    inputView.inputType = textInputType // 16 for website url input type
  }
  inputView.setText(value, TextView.BufferType.EDITABLE)


  applyButton.setOnClickListener {
    callback.invoke(inputView.text.toString())  // try to save the setting, using callback
    dialog.dismissSafe(this)
  }

  cancelButton.setOnClickListener {  // just dismiss
    dialog.dismissSafe(this)
  }

  dialog.setOnDismissListener {
    dismissCallback.invoke()
  }

}

fun Activity?.showMultiDialog(
  items: List<String>,
  selectedIndex: List<Int>,
  name: String,
  dismissCallback: () -> Unit,
  callback: (List<Int>) -> Unit,
) {
  if (this == null) return

  val binding: DialogBottomSelectionBinding = DialogBottomSelectionBinding.inflate(
    LayoutInflater.from(this)
  )
  val builder =
    AlertDialog.Builder(this, R.style.AlertDialogCustom)
      .setView(binding.root)

  val dialog = builder.create()
  dialog.show()
  showDialog(
    binding,
    dialog,
    items,
    selectedIndex,
    name,
    showApply = true,
    isMultiSelect = true,
    callback,
    dismissCallback
  )
}

fun Activity?.showDialog(
  items: List<String>,
  selectedIndex: Int,
  name: String,
  showApply: Boolean,
  dismissCallback: () -> Unit,
  callback: (Int) -> Unit,
) {
  if (this == null) return

  val binding: DialogBottomSelectionBinding = DialogBottomSelectionBinding.inflate(
    LayoutInflater.from(this)
  )
  val builder = MaterialAlertDialogBuilder(this)
    .setView(binding.root)

  val dialog = builder.create()
  dialog.show()


  showDialog(
    binding,
    dialog,
    items,
    listOf(selectedIndex),
    name,
    showApply,
    false,
    { if (it.isNotEmpty()) callback.invoke(it.first()) },
    dismissCallback
  )
}

/** Only for a low amount of items */
fun Activity?.showBottomDialog(
  items: List<String>,
  selectedIndex: Int,
  name: String,
  showApply: Boolean,
  dismissCallback: () -> Unit,
  callback: (Int) -> Unit,
) {
  if (this == null) return

  val binding: DialogBottomSelectionBinding = DialogBottomSelectionBinding.inflate(
    LayoutInflater.from(this)
  )

  val builder =
    BottomSheetDialog(this)
  builder.setContentView(binding.root)

  builder.show()
  showDialog(
    binding,
    builder,
    items,
    listOf(selectedIndex),
    name,
    showApply,
    false,
    { if (it.isNotEmpty()) callback.invoke(it.first()) },
    dismissCallback
  )
}

fun Activity.showBottomDialogInstant(
  items: List<String>,
  name: String,
  dismissCallback: () -> Unit,
  callback: (Int) -> Unit,
): BottomSheetDialog {
  val builder =
    BottomSheetDialog(this)

  val binding: DialogBottomSelectionBinding = DialogBottomSelectionBinding.inflate(
    LayoutInflater.from(this)
  )

  //builder.setContentView(R.layout.bottom_selection_dialog_direct)
  builder.setContentView(binding.root)
  builder.show()
  showDialog(
    binding,
    builder,
    items,
    emptyList(),
    name,
    showApply = false,
    isMultiSelect = false,
    callback = { if (it.isNotEmpty()) callback.invoke(it.first()) },
    dismissCallback = dismissCallback,
    itemLayout = R.layout.sort_bottom_single_choice_color
  )
  return builder
}

fun Activity.showNginxTextInputDialog(
  name: String,
  value: String,
  textInputType: Int?,
  dismissCallback: () -> Unit,
  callback: (String) -> Unit,
) {
  val builder = BottomSheetDialog(this)

  val binding: DialogBottomInputBinding = DialogBottomInputBinding.inflate(
    LayoutInflater.from(this)
  )

  builder.setContentView(binding.root)

  builder.show()
  showInputDialog(
    binding,
    builder,
    value,
    name,
    textInputType,  // type is a uri
    callback,
    dismissCallback
  )
}

fun Activity.showBottomDialogText(
  title: String,
  text: Spanned,
  dismissCallback: () -> Unit
) {
  val binding = DialogBottomTextBinding.inflate(layoutInflater)
  val dialog = BottomSheetDialog(this)

  dialog.setContentView(binding.root)

  binding.dialogTitle.text = title
  binding.dialogText.text = text

  dialog.setOnDismissListener {
    dismissCallback.invoke()
  }
  dialog.show()
}

fun Activity.shareItem(item : AVPMediaItem) {
  try {
    val i = Intent(Intent.ACTION_SEND)
    i.type = "text/plain"
    i.putExtra(Intent.EXTRA_SUBJECT, item.title)
    startActivity(Intent.createChooser(i, item.title))
  } catch (e: Exception) {
    Timber.e(e)
  }
}
