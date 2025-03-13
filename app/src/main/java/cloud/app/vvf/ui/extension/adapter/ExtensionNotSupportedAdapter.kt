package cloud.app.vvf.ui.extension.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.R
import cloud.app.vvf.common.clients.Extension
import cloud.app.vvf.databinding.ItemClientNotSupportedBinding

class ExtensionNotSupportedAdapter(
  val
  extension: Extension<*>, val type: String
) : RecyclerView.Adapter<ExtensionNotSupportedAdapter.ViewHolder>() {

  override fun getItemCount() = 1

  class ViewHolder(val binding: ItemClientNotSupportedBinding) :
    RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
    ItemClientNotSupportedBinding
      .inflate(LayoutInflater.from(parent.context), parent, false)
  )


  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val binding = holder.binding
    val clientType = type
    binding.notSupportedTextView.text =
      binding.root.context.getString(R.string.not_supported, clientType, extension.name)
  }
}
