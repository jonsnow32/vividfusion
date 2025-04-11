package cloud.app.vvf.features.player.utils

import androidx.annotation.StringRes
import cloud.app.vvf.R

enum class ResizeMode(@StringRes val nameRes: Int) {
    Fit(R.string.resize_fit),
    Fill(R.string.resize_fill),
    Zoom(R.string.resize_zoom),
}
