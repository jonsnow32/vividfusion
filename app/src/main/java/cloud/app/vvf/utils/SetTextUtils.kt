package cloud.app.vvf.utils

import android.view.View
import android.widget.TextView
import com.google.android.material.button.MaterialButton

fun TextView.setTextWithVisibility(text: String?) {
  if(text.isNullOrEmpty()) {
    this.visibility = View.GONE
  } else {
    this.visibility = View.VISIBLE
    this.text = text
  }
}

fun MaterialButton.setTextWithVisibility(text: String?) {
  if(text.isNullOrEmpty()) {
    this.visibility = View.GONE
  } else {
    this.visibility = View.VISIBLE
    this.text = text
  }
}
