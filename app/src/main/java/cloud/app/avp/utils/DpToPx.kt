package cloud.app.avp.utils

import android.content.Context

fun Int.dpToPx(context: Context) = (this * context.resources.displayMetrics.density).toInt()
