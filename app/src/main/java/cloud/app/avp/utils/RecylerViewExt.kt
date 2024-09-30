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

fun RecyclerView.firstVisible() =
  (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

fun RecyclerView.scrollTo(position: Int, offset: Int = 0, block: (Int) -> Unit) = doOnLayout {
  if (position < 0) return@doOnLayout
  (layoutManager as LinearLayoutManager).run {
    scrollToPositionWithOffset(position, offset)
    post { block(findFirstVisibleItemPosition()) }
  }
}
