package cloud.app.vvf.ui.main.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import cloud.app.vvf.R
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.utils.loadInto

data class DropdownItem(val icon: String?, val text: String, val clientID: String, val selected: Boolean = false)
class ExtensionMenuAdapter(private val context: Context, private val items: List<DropdownItem>) :
  BaseAdapter() {
  override fun getCount(): Int = items.size
  override fun getItem(position: Int): Any = items[position]
  override fun getItemId(position: Int): Long = position.toLong()


  override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
    val view = convertView ?: LayoutInflater.from(context)
      .inflate(R.layout.home_extension_menu_item, parent, false)
    val textView = view.findViewById<TextView>(R.id.item_text)
    textView.setCompoundDrawablesRelative(null, null, null, null)
    val item = items[position]
    textView.text = item.text
    val itemIcon = view.findViewById<ImageView>(R.id.itemIcon)
    item.icon?.toImageHolder().loadInto(itemIcon)

    return view
  }
}
