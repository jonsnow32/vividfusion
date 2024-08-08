package cloud.app.avp.utils

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

fun SwipeRefreshLayout.configure(block:()->Unit){
    setProgressViewOffset(true, 0, 64.toPx)
    setOnRefreshListener(block)
}
