package cloud.app.vvf.extension.builtIn.providers.subtitles

import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.common.models.subtitle.SubtitleOrigin
import cloud.app.vvf.extension.builtIn.providers.SubtitleProvider
import cloud.app.vvf.utils.SubtitleHelper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SubSource : SubtitleProvider {

  companion object {
    private const val API_BASE_URL = "https://api.subsource.net/api"
    private const val API_DOWNLOAD_ENDPOINT = "https://api.subsource.net/api/downloadSub"
  }

  override suspend fun loadSubtitles(
    httpHelper: HttpHelper,
    searchItem: SearchItem,
    callback: suspend (SubtitleData) -> Unit
  ) {
    val params = searchItem.toQuery()
    if (params.imdbId == null) return

    val queryLangs = params.lang.mapNotNull { SubtitleHelper.fromTwoLettersToLanguage(it) }
    if (queryLangs.isEmpty()) return
    val isTV = (params.seasonNumber ?: 0) > 0

    val searchResponse = httpHelper.post(
      url = "$API_BASE_URL/searchMovie",
      data = mapOf("query" to params.imdbId)
    ).parsedSafe<ApiSearch>() ?: return

    val movieLinkName = searchResponse.found.firstOrNull()?.linkName ?: return

    val postData = buildPostData(isTV, movieLinkName, params)
    val subtitles = httpHelper.post(
      url = "$API_BASE_URL/getMovie",
      data = postData
    ).parsedSafe<ApiResponse>()?.subs?.filterSubtitles(isTV, queryLangs, params.epNumber)
      ?: return

    subtitles.forEach { subtitle ->
      val subResponse = httpHelper.post(
        url = "$API_BASE_URL/getSub",
        data = mapOf(
          "movie" to subtitle.linkName.orEmpty(),
          "lang" to subtitle.lang.orEmpty(),
          "id" to subtitle.subId.toString()
        )
      ).parsedSafe<SubTitleLink>() ?: return@forEach

      callback(
        SubtitleData(
          name = subtitle.releaseName.orEmpty(),
          url = "$API_DOWNLOAD_ENDPOINT/${subResponse.sub.downloadToken}",
          origin = SubtitleOrigin.URL,
          mimeType = null,
          headers = emptyMap(),
          languageCode = subtitle.lang.orEmpty(),
          isHearingImpaired = subtitle.hi == 1
        )
      )
    }
  }

  private fun buildPostData(
    isTV: Boolean,
    movieLinkName: String,
    params: Params
  ): Map<String, String> {
    return if (isTV) {
      mapOf(
        "langs" to params.lang.joinToString(","),
        "movieName" to movieLinkName,
        "season" to "season-${params.seasonNumber}"
      )
    } else {
      mapOf(
        "langs" to params.lang.joinToString(","),
        "movieName" to movieLinkName
      )
    }
  }

  private fun List<Sub>.filterSubtitles(
    isTV: Boolean,
    queryLangs: List<String>,
    epNumber: Int?
  ): List<Sub> {
    return if (isTV && epNumber != null) {
      filter { sub ->
        queryLangs.contains(sub.lang.orEmpty()) && sub.releaseName?.contains("E%02d".format(epNumber)) == true
      }
    } else {
      filter { sub -> queryLangs.contains(sub.lang.orEmpty()) }
    }
  }

  @Serializable
  data class ApiSearch(
    @SerialName("success") val success: Boolean,
    @SerialName("found") val found: List<Found>
  )

  @Serializable
  data class Found(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("seasons") val seasons: Long? = null,
    @SerialName("type") val type: String,
    @SerialName("releaseYear") val releaseYear: Long,
    @SerialName("linkName") val linkName: String
  )

  @Serializable
  data class ApiResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("movie") val movie: Movie,
    @SerialName("subs") val subs: List<Sub>
  )

  @Serializable
  data class Movie(
    @SerialName("id") val id: Long? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("year") val year: Long? = null,
    @SerialName("fullName") val fullName: String? = null
  )

  @Serializable
  data class Sub(
    @SerialName("hi") val hi: Int? = null,
    @SerialName("fullLink") val fullLink: String? = null,
    @SerialName("linkName") val linkName: String? = null,
    @SerialName("lang") val lang: String? = null,
    @SerialName("releaseName") val releaseName: String? = null,
    @SerialName("subId") val subId: Long? = null
  )

  @Serializable
  data class SubTitleLink(
    @SerialName("sub") val sub: SubToken
  )

  @Serializable
  data class SubToken(
    @SerialName("downloadToken") val downloadToken: String
  )

  data class Params(
    val query: String = "",
    val lang: List<String> = emptyList(),
    val imdbId: String? = null,
    val tmdbId: Int? = null,
    val malId: Int? = null,
    val aniListId: Int? = null,
    val epNumber: Int? = null,
    val seasonNumber: Int? = null,
    val year: Int? = null
  )

  private fun SearchItem.toQuery(): Params {
    return Params(
      query = query,
      lang = extras?.get("lang")?.split(",")?.map { it.trim() } ?: emptyList(),
      imdbId = extras?.get("imdbId"),
      tmdbId = extras?.get("tmdbId")?.toIntOrNull(),
      malId = extras?.get("malId")?.toIntOrNull(),
      aniListId = extras?.get("aniListId")?.toIntOrNull(),
      epNumber = extras?.get("epNumber")?.toIntOrNull(),
      seasonNumber = extras?.get("seasonNumber")?.toIntOrNull(),
      year = extras?.get("year")?.toIntOrNull()
    )
  }
}
