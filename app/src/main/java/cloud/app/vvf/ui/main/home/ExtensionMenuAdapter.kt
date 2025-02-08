package cloud.app.vvf.ui.main.home

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import cloud.app.vvf.R
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

data class DropdownItem(val icon: String?, val text: String, val clientID: String)
class ExtensionMenuAdapter(private val context: Context, private val items: List<DropdownItem>) : BaseAdapter() {
  override fun getCount(): Int = items.size
  override fun getItem(position: Int): Any = items[position]
  override fun getItemId(position: Int): Long = position.toLong()

  override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
    val view = convertView ?: LayoutInflater.from(context)
      .inflate(R.layout.home_extension_menu_item, parent, false)

    // Get reference to the TextView
    val textView = view.findViewById<TextView>(R.id.item_text)

    // Retrieve current item
    val item = items[position]
    textView.text = item.text

    // Optionally, if you want to set the drawable dynamically instead of using the XML drawableStart:
    // Load image using Glide and set it as the left drawable for the TextView
    Glide.with(context)
      .load(item.icon)
      .into(object : CustomTarget<Drawable>() {
        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
          // Convert your desired dp dimensions to pixels
          val widthInDp = 24  // Desired width in dp
          val heightInDp = 24 // Desired height in dp
          val density = context.resources.displayMetrics.density
          val widthInPx = (widthInDp * density).toInt()
          val heightInPx = (heightInDp * density).toInt()

          // Set bounds on the drawable
          resource.setBounds(0, 0, widthInPx, heightInPx)

          // Apply the drawable as the start compound drawable
          textView.setCompoundDrawablesRelative(resource, null, null, null)
        }

        override fun onLoadCleared(placeholder: Drawable?) {
          // Optionally handle the placeholder here
          placeholder?.let {
            it.setBounds(0, 0, it.intrinsicWidth, it.intrinsicHeight)
          }
          textView.setCompoundDrawablesRelative(placeholder, null, null, null)
        }
      })


    return view
  }
}
