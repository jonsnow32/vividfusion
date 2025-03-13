package cloud.app.vvf.utils

import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.firstVisible() =
  (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

fun RecyclerView.scrollTo(position: Int, offset: Int = 0, block: (Int) -> Unit) = doOnLayout {
  if (position < 0) return@doOnLayout
  (layoutManager as LinearLayoutManager).run {
    scrollToPositionWithOffset(position, offset)
    post { block(findFirstVisibleItemPosition()) }
  }
}
