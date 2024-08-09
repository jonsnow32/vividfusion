package cloud.app.avp.utils

import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
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

fun RecyclerView.first() =
  (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

fun RecyclerView.scrollTo(position: Int, block: (Int) -> Unit) = doOnLayout {
  if (position < 1) return@doOnLayout
  (layoutManager as LinearLayoutManager).run {
    scrollToPositionWithOffset(position, 0)
    post { block(findFirstVisibleItemPosition()) }
  }
}
