package cloud.app.avp.plugin.tmdb

import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

object SearchSuggestion {
  fun search(query: String): ImdbSuggestion? {
    val re = Regex("[^A-Za-z0-9 ]")
    val newQuery = re.replace(query.lowercase(), "");

    if (newQuery.length < 2) return null;
    val url = "https://v2.sg.media-imdb.com/suggests/%s/%s.json".format(
      newQuery[0],
      newQuery.replace("\\s+", "")
    )
    val request: Request = Request.Builder().url(url).build()
    val okHttpClient = OkHttpClient();
    val response = okHttpClient.newCall(request).execute();
    response.body?.string()?.let { html ->
      Timber.tag("SearchSuggestion").i(html)
      val regex = "\\((.*)\\)".toRegex();
      val json = regex.find(html)?.destructured?.toList();
      json?.let {
        val imdbSuggestion = Gson().fromJson(it[0], ImdbSuggestion::class.java)
        imdbSuggestion.d.let {
          return imdbSuggestion;
        }
      }
    }
    return null
  }

  data class ImdbSuggestion(
    val d: List<ImdbSearchItem>,
    val q: String,
    val v: Int,
  )

  data class ImdbSearchItem(
    val id: String,
    val l: String,
    val y: Int,
    val i: List<String>,
    val q: String
  )
}
