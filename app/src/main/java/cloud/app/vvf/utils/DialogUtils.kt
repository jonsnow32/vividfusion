package cloud.app.vvf.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
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
import cloud.app.vvf.databinding.DialogAddRepoInputBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import timber.log.Timber

/**
 * Sets the focus to the negative button when in TV and Emulator layout.
 **/
fun AlertDialog.setDefaultFocus(buttonFocus: Int = DialogInterface.BUTTON_NEGATIVE) {
  if (!context.isLayout(TV or EMULATOR)) return
  this.getButton(buttonFocus).run {
    isFocusableInTouchMode = true
    requestFocus()
  }
}

fun Dialog?.dismissSafe(activity: Activity?) {
  if (this?.isShowing == true && activity?.isFinishing == false) {
    this.dismiss()
  }
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

