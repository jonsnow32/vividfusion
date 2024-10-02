package cloud.app.avp.utils

import timber.log.Timber

object ImageUrlBuilder {
    val baseUrl = "https://image.tmdb.org/t/p"
    fun getUrl(path: String, width: Int): String {
        return ""
        if (path.contains("http://") || path.contains("https://")) return path
        var widthPath = ""
        if (width <= 92) widthPath = "/w92"
        else if (width <= 154) widthPath = "/w154"
        else if (width <= 185) widthPath = "/w185"
        else if (width <= 342) widthPath = "/w342"
        else if (width <= 500) widthPath = "/w500"
        else if (width <= 780) widthPath = "/w780"
        else if (width <= 1920) widthPath = "/w1280"
        else widthPath = "/original"
        Timber.i(baseUrl + widthPath + path)
        return baseUrl + widthPath + path
    }
}
