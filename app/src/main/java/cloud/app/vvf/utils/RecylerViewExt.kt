package cloud.app.vvf.utils

import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cloud.app.vvf.ui.main.ClientNotSupportedAdapter
import cloud.app.vvf.ui.main.home.ClientLoadingAdapter
import cloud.app.vvf.common.clients.Extension

inline fun <reified T> RecyclerView.applyAdapter(
  extension: Extension<*>?,
  name: Int,
  adapter: RecyclerView.Adapter<out RecyclerView.ViewHolder>
) {
  val client = extension?.instance?.value?.getOrNull()
  setAdapter(
    if (extension == null)
      ClientLoadingAdapter()
    else if (client !is T)
      ClientNotSupportedAdapter(name, extension.name)
    else adapter
  )
}

fun RecyclerView.firstVisible() =
  (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

fun RecyclerView.scrollTo(position: Int, offset: Int = 0, block: (Int) -> Unit) = doOnLayout {
  if (position < 0) return@doOnLayout
  (layoutManager as LinearLayoutManager).run {
    scrollToPositionWithOffset(position, offset)
    post { block(findFirstVisibleItemPosition()) }
  }
}
