package cloud.app.avp.utils

import androidx.recyclerview.widget.RecyclerView
import cloud.app.avp.ui.main.ClientNotSupportedAdapter
import cloud.app.avp.ui.main.home.ClientLoadingAdapter
import cloud.app.common.clients.BaseExtension

inline fun <reified T> RecyclerView.applyAdapter(
  extension: BaseExtension?,
  name: Int,
  adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>,
  block: ((T?) -> Unit) = {}
) {
  block(extension as? T)
  setAdapter(
    if (extension == null)
      ClientLoadingAdapter()
    else if (extension !is T)
      ClientNotSupportedAdapter(name, extension::class.java.simpleName)
    else adapter
  )
}
