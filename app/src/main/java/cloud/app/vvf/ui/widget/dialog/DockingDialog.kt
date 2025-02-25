package cloud.app.vvf.ui.widget.dialog

import android.app.Dialog
import android.content.res.Configuration
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import cloud.app.vvf.R

open class DockingDialog : DialogFragment() {

  enum class Docking {
    RIGHT, BOTTOM
  }

  private var orientation: Docking = Docking.RIGHT

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    orientation = when (context?.resources?.configuration?.orientation) {
      Configuration.ORIENTATION_LANDSCAPE -> Docking.RIGHT
      Configuration.ORIENTATION_PORTRAIT -> Docking.BOTTOM
      else -> Docking.BOTTOM
    }
    val dialogStyle: Int = if (orientation == Docking.RIGHT)
      R.style.RightMaterialDialogTheme
    else
      R.style.BottomMaterialDialogTheme
    return Dialog(requireContext(), dialogStyle).apply {
      setCanceledOnTouchOutside(true)
    }
  }

  override fun onStart() {
    super.onStart()
    dialog?.window?.apply {
      val width: Int
      val height: Int
      val gravity: Int

      when (orientation) {
        Docking.RIGHT -> {
          width = (resources.displayMetrics.widthPixels * 0.3).toInt() // 30% of screen width
          height = ViewGroup.LayoutParams.MATCH_PARENT // Full height
          gravity = Gravity.END // Anchor to the right
        }

        Docking.BOTTOM -> {
          width = ViewGroup.LayoutParams.MATCH_PARENT // Full width
          height = ViewGroup.LayoutParams.WRAP_CONTENT // Wrap height
          gravity = Gravity.BOTTOM // Anchor to the bottom
        }
      }

      setLayout(width, height)
      setGravity(gravity)
    }
  }
}
