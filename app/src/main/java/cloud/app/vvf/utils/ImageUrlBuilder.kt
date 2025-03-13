package cloud.app.vvf.utils

import timber.log.Timber

object ImageUrlBuilder {

  const val BASE_URL = "https://image.tmdb.org/t/p"

  fun getUrl(path: String, width: Int): String {
    if (path.startsWith("http://") || path.startsWith("https://")) {
      return path
    }

    val widthPath = when {
      width <= 92 -> "/w92"
      width <= 154 -> "/w154"
      width <= 185 -> "/w185"
      width <= 342 -> "/w342"
      width <= 500 -> "/w500"
      width <= 780 -> "/w780"
      width <= 1920 -> "/w1280"
      else -> "/original"
    }

    val url = "$BASE_URL$widthPath$path"
    Timber.i(url)
    return url
  }
}
