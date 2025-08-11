package cloud.app.vvf.ui.widget.dialog.actionOption

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import cloud.app.vvf.R

class IconTextAdapter(
  context: Context,
  private val items: List<IconTextItem>
) : ArrayAdapter<IconTextItem>(context, R.layout.item_icon_text, items) {
  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_icon_text, parent, false)
    val item = items[position]
    view.findViewById<ImageView>(R.id.item_icon).setImageResource(item.iconRes)
    view.findViewById<TextView>(R.id.item_text).text = context.getString(item.textRes)
    return view
  }
}
