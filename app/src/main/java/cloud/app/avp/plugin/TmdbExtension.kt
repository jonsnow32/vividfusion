package cloud.app.avp.plugin

import cloud.app.avp.plugin.tmdb.AppTmdb
import cloud.app.avp.plugin.tmdb.SearchSuggestion
import cloud.app.avp.plugin.tmdb.companies
import cloud.app.avp.plugin.tmdb.formatSeasonEpisode
import cloud.app.avp.plugin.tmdb.iso8601ToMillis
import cloud.app.avp.plugin.tmdb.movieGenres
import cloud.app.avp.plugin.tmdb.networks
import cloud.app.avp.plugin.tmdb.showGenres
import cloud.app.avp.plugin.tmdb.toMediaItem
import cloud.app.avp.plugin.tmdb.toMediaItemsList
import cloud.app.avp.plugin.tvdb.AppTheTvdb
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.ExtensionMetadata
import cloud.app.common.clients.mvdatabase.FeedClient
import cloud.app.common.clients.mvdatabase.SearchClient
import cloud.app.common.helpers.Page
import cloud.app.common.helpers.PagedData
import cloud.app.common.helpers.network.HttpHelper
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.AVPMediaItem.Companion.toMediaItemsContainer
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.LoginType
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.QuickSearchItem
import cloud.app.common.models.SortBy
import cloud.app.common.models.Tab
import cloud.app.common.models.movie.Episode
import cloud.app.common.models.movie.GeneralInfo
import cloud.app.common.models.movie.Ids
import cloud.app.common.models.movie.Season
import cloud.app.common.models.movie.Show
import cloud.app.common.settings.PrefSettings
import cloud.app.common.settings.Setting
import cloud.app.common.settings.SettingList
import cloud.app.common.settings.SettingSwitch
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.DiscoverFilter
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem

class TmdbExtension : BaseExtension, FeedClient, SearchClient {
  private lateinit var tmdb: AppTmdb
  private lateinit var tvdb: AppTheTvdb
  override val metadata: ExtensionMetadata
    get() = ExtensionMetadata(
      name = "The extension of TMDB",
      ExtensionType.DATABASE,
      description = "A sample extension that does nothing",
      author = "avp",
      version = "v001",
      icon = "https://www.freepnglogos.com/uploads/netflix-logo-0.png",
      loginType = LoginType.API_KEY
    )

  override val defaultSettings: List<Setting> = listOf(
    SettingSwitch(
      "Include Adult",
      "tmdb_include_adult",
      "Include adult",
      false
    ),
    SettingList(
      "Language",
      "tmdb_language",
      "Language",
      entryTitles = listOf("English", "Spanish"),
      entryValues = listOf("en", "es")
    ),
    SettingList(
      "Region",
      "tmdb_Region",
      "Region",
      entryTitles = listOf("English", "Spanish"),
      entryValues = listOf("en", "es")
    )
  )

  private lateinit var prefSettings: PrefSettings
  override fun init(prefSettings: PrefSettings, httpHelper: HttpHelper) {
    this.prefSettings = prefSettings
    tmdb = AppTmdb(
      httpHelper.okHttpClient,
      prefSettings.getString("pref_tmdb_api_key") ?: "4ef60b9d635f533695cbcaccb6603a57"
    )
    tvdb = AppTheTvdb(
      httpHelper.okHttpClient,
      prefSettings.getString("pref_tvdb_api_key") ?: "4ef60b9d635f533695cbcaccb6603a57",
      prefSettings,
    )
  }

  private val includeAdult get() = prefSettings.getBoolean("tmdb_include_adult")
  private val region get() = prefSettings.getString("tmdb_region")
  private val language get() = prefSettings.getString("tmdb_language")

  override suspend fun onExtensionSelected() {
    TODO("TMDB EXTENSION Not yet implemented")
  }


  override suspend fun getHomeTabs(): List<Tab> =
    listOf("Movies", "TV Shows", "Networks", "Companies").map { Tab(it, it) }

  override fun getHomeFeed(tab: Tab?) = PagedData.Continuous<MediaItemsContainer> {
    val page = it?.toInt() ?: 1
    val sortBy = tab?.extras?.get("sort_by")?.let { value ->
      sortMapper[value]
    }
    val items = when (tab?.id) {
      "Movies" -> getMoviesList(movieGenres, page, pageSize, sortBy)
      "TV Shows" -> getShowList(showGenres, page, pageSize, sortBy)
      "Networks" -> getNetworks(networks, page, pageSize, sortBy)
      "Companies" -> getCompanies(companies, page, pageSize, sortBy)
      else -> TODO()
    }

    val continuation = if (items.size < pageSize) null else (page + 1).toString()
    Page(items, continuation)
  }

  override suspend fun getMediaDetail(avpMediaItem: AVPMediaItem): AVPMediaItem? {
    return when (avpMediaItem) {
      is AVPMediaItem.MovieItem -> getMovieDetail(avpMediaItem.movie.ids.tmdbId)
      is AVPMediaItem.ShowItem -> getShowDetail(avpMediaItem.show.ids.tmdbId)
      is AVPMediaItem.EpisodeItem -> getEpisodeDetail(avpMediaItem.episode.ids.tmdbId)
      is AVPMediaItem.SeasonItem -> getSeasonDetail(avpMediaItem)
      else -> null
    }
  }

  private fun getSeasonDetail(seasonItem: AVPMediaItem.SeasonItem): AVPMediaItem? {
    val season = seasonItem.season;

    return if (season.showIds.tmdbId != null) {
      val response = tmdb.tvSeasonsService().season(
        season.showIds.tmdbId!!,
        season.number,
        language
      ).execute().body()
      response?.let {
        AVPMediaItem.SeasonItem(
          season = Season(
            title = it.name,
            number = it.season_number ?: -1,
            overview = it.overview,
            episodeCount = it.episodes?.size ?: 0,
            posterPath = it.poster_path,
            episodes = it.episodes?.map { episode ->
              Episode(
                Ids(tmdbId = episode.id),
                GeneralInfo(
                  title = episode.name,
                  backdrop = episode.still_path,
                  poster = episode.still_path,
                  overview = episode.overview,
                  releaseDateMsUTC = episode.air_date?.time,
                  originalTitle = episode.name
                ),
                seasonNumber = episode.season_number,
                episodeNumber = episode.episode_number,
                showIds = season.showIds,
                showOriginTitle = season.showOriginTitle ?: ""
              )
            },
            showIds = season.showIds,
            showOriginTitle = season.showOriginTitle,
            backdrop = season.backdrop,
            releaseDateMsUTC = it.air_date?.time
          ),
          showItem = seasonItem.showItem
        )
      }
    } else {
      null // Handle case where no show ID is available
    }
  }

  private fun getEpisodeDetail(tmdbId: Int?): AVPMediaItem.EpisodeItem? {
    TODO("Not yet implemented")
  }

  private fun getShowDetail(tmdbId: Int?): AVPMediaItem.ShowItem? {
    tmdbId ?: return null
    val response = tmdb.tvService().tv(
      tmdbId,
      language,
      AppendToResponse(
        AppendToResponseItem.MOVIES,
        AppendToResponseItem.EXTERNAL_IDS,
        AppendToResponseItem.CREDITS,
        AppendToResponseItem.COMBINED_CREDITS,
        AppendToResponseItem.IMAGES,
        AppendToResponseItem.RECOMMENDATIONS,
        AppendToResponseItem.VIDEOS,
        AppendToResponseItem.REVIEWS
      )
    )
    val body = response.execute().body()
    return body?.toMediaItem()
  }

  private fun getMovieDetail(tmdbId: Int?): AVPMediaItem.MovieItem? {

    tmdbId ?: return null
    val response = tmdb.moviesService().summary(
      tmdbId,
      language,
      AppendToResponse(
        AppendToResponseItem.MOVIES,
        AppendToResponseItem.EXTERNAL_IDS,
        AppendToResponseItem.MOVIE_CREDITS,
        AppendToResponseItem.CREDITS,
        AppendToResponseItem.COMBINED_CREDITS,
        AppendToResponseItem.IMAGES,
        AppendToResponseItem.RECOMMENDATIONS,
        AppendToResponseItem.VIDEOS,
        AppendToResponseItem.REVIEWS
      )
    )
    return response.execute().body()?.toMediaItem()
  }

  private fun Map<Int, String>.toPage(page: Int, pageSize: Int): Map<Int, String> {
    val startIndex = (page - 1) * pageSize
    val endIndex = minOf(startIndex + pageSize, size)
    return entries.toList().subList(startIndex, endIndex).associate { it.key to it.value }
  }

  private suspend fun getMoviesList(
    genres: Map<Int, String>,
    page: Int,
    pageSize: Int,
    sortBy: com.uwetrottmann.tmdb2.enumerations.SortBy? = null
  ) = genres.toPage(page, pageSize).map { genre ->
    val data = PagedData.Continuous<AVPMediaItem> {
      val continuation = it?.toInt() ?: 1
      val builder = tmdb.discoverMovie()
        .sort_by(sortBy)
        .with_genres(DiscoverFilter(genre.key))
        .page(continuation)
        .language(language)
        .region(region)
        .includeAdult()

      if (includeAdult == true)
        builder.includeAdult()

      val items = builder.build().execute().body()!!
      Page(
        items.toMediaItemsList(),
        if (items.results.isNullOrEmpty()) null else (continuation + 1).toString()
      )
    }
    data.loadFirst()
    toMediaItemsContainer(
      genre.value,
      genre.value,
      data
    )
  }.toList()


  private suspend fun getShowList(
    genres: Map<Int, String>,
    page: Int,
    pageSize: Int,
    sortBy: com.uwetrottmann.tmdb2.enumerations.SortBy?
  ) = genres.toPage(page, pageSize).map { genre ->
    val data = PagedData.Continuous<AVPMediaItem> {
      val continuation = it?.toInt() ?: 1
      val more = tmdb.discoverTv()
        .sort_by(sortBy)
        .with_genres(DiscoverFilter(genre.key))
        .page(continuation)
        .language(language)
        .watch_region(region)
        .build().execute().body()!!
      Page(
        more.toMediaItemsList(),
        if (more.results.isNullOrEmpty()) null else (continuation + 1).toString()
      )
    }
    data.loadFirst() //preload first page to home feed
    toMediaItemsContainer(
      genre.value,
      genre.value,
      data
    )
  }.toList()

  //Searching
  override suspend fun quickSearch(query: String): List<QuickSearchItem> {
    val results = mutableListOf<QuickSearchItem>()
    val searchResult = SearchSuggestion.search(query)
    searchResult?.d?.forEach {
      results.add(QuickSearchItem.SearchQueryItem(it.l, false))
    }
    return results
  }

  override suspend fun searchTabs(query: String?): List<Tab> =
    listOf("All", "Movies", "TV Shows", "Actors").map { Tab(it, it) }

  override fun searchFeed(query: String?, tab: Tab?): PagedData<MediaItemsContainer> {
    query ?: return emptyList<MediaItemsContainer>().toPaged()
    return when (tab?.id) {
      "Movies" -> searchMoviesFeed(query, tab.extras)
      "TV Shows" -> searchTvShowsFeed(query, tab.extras)
      "Actors" -> searchActorsFeed(query, tab.extras)
      else -> searchAll(query, tab?.extras)
    }
  }

  private fun searchActorsFeed(
    query: String?,
    extras: Map<String, String>?
  ): PagedData<MediaItemsContainer> {
    val more = PagedData.Continuous<AVPMediaItem> {
      val continuation = it?.toInt() ?: 1
      val pageResult = tmdb.searchService()
        .person(query, continuation, language, region, includeAdult)
        .execute()
        .body()
      Page(
        pageResult?.toMediaItemsList() ?: emptyList(),
        if (pageResult?.results.isNullOrEmpty()) null else (continuation + 1).toString()
      )
    }
    val category = toMediaItemsContainer(query ?: "Actor search", query ?: "Actor search", more)
    return PagedData.Single { listOf(category) }
  }

  private fun searchTvShowsFeed(
    query: String?,
    extras: Map<String, String>?
  ): PagedData<MediaItemsContainer> {
    val more = PagedData.Continuous<AVPMediaItem> {
      val continuation = it?.toInt() ?: 1
      val firstAirDateYear = extras?.get("first_air_date_year")?.toInt()
      val pageResult = tmdb.searchService()
        .tv(query, continuation, language, firstAirDateYear, includeAdult)
        .execute()
        .body()
      Page(
        pageResult?.toMediaItemsList() ?: emptyList(),
        if (pageResult?.results.isNullOrEmpty()) null else (continuation + 1).toString()
      )
    }
    val category = toMediaItemsContainer(query ?: "TV Show search", query ?: "TV Show search", more)
    return PagedData.Single { listOf(category) }
  }

  private fun searchMoviesFeed(
    query: String?,
    extras: Map<String, String>?
  ): PagedData<MediaItemsContainer> {
    val more = PagedData.Continuous<AVPMediaItem> {
      val continuation = it?.toInt() ?: 1
      val year = extras?.get("year")?.toInt()
      val pageResult =
        tmdb.searchService()
          .movie(query, continuation, language, region, includeAdult, year, year)
          .execute().body()
      Page(
        pageResult?.toMediaItemsList() ?: emptyList(),
        if (pageResult?.results.isNullOrEmpty()) null else (continuation + 1).toString()
      )
    }
    val category = toMediaItemsContainer(query ?: "Movie search", query ?: "Movie search", more)
    return PagedData.Single { (listOf(category)) }
  }

  private fun List<AVPMediaItem>.toPaged() = PagedData.Single { this }
  private fun searchAll(query: String?, extras: Map<String, String>?) = PagedData.Single {
    val movies = mutableListOf<AVPMediaItem.MovieItem>()
    val shows = mutableListOf<AVPMediaItem.ShowItem>()
    val casts = mutableListOf<AVPMediaItem.ActorItem>()
    val mediaResultsPage =
      tmdb.searchService().multi(query, 1, "en", "en_US", true).execute().body()
    mediaResultsPage?.results?.forEach {
      it.movie?.let { movie ->
        movies.add(movie.toMediaItem())
      }
      it.tvShow?.let { tvShow ->
        shows.add(tvShow.toMediaItem())
      }
      it.person?.let { person ->
        casts.add(person.toMediaItem())
      }
    }
    val mediaContainer = mutableListOf<MediaItemsContainer>()
    if (movies.isNotEmpty()) {
      mediaContainer.add(toMediaItemsContainer("Movies", "", movies.toPaged()))
    }
    if (shows.isNotEmpty()) {
      mediaContainer.add(toMediaItemsContainer("TV Shows", "", shows.toPaged()))
    }
    if (casts.isNotEmpty()) {
      mediaContainer.add(toMediaItemsContainer("Actors", "", casts.toPaged()))
    }
    mediaContainer
  }


  companion object {
    private fun List<MediaItemsContainer>.toPaged() = PagedData.Single { this }
    val sortMapper: Map<String, com.uwetrottmann.tmdb2.enumerations.SortBy> = mapOf(
      SortBy.POPULARITY.serializedName to com.uwetrottmann.tmdb2.enumerations.SortBy.POPULARITY_DESC,
      SortBy.RELEASED.serializedName to com.uwetrottmann.tmdb2.enumerations.SortBy.RELEASE_DATE_DESC,
      SortBy.RATING.serializedName to com.uwetrottmann.tmdb2.enumerations.SortBy.VOTE_AVERAGE_DESC,
      SortBy.TITLE.serializedName to com.uwetrottmann.tmdb2.enumerations.SortBy.ORIGINAL_TITLE_DESC,
      // Add other mappings as necessary
    )
    const val pageSize = 3;
  }


  private suspend fun getTvdbEpisodes(show: Show, page: Int): List<Episode> {
    val list = mutableListOf<Episode>()
    if (show.ids.tvdbId != null) {
      val response = tvdb.series().episodes(
        show.ids.tvdbId!!,
        page,
        language
      ).execute()

      if (response.isSuccessful) {
        response.body()?.let {
          it.data?.forEach { episode ->
            val episodeTitle = episode.episodeName ?: formatSeasonEpisode(
              episode.airedSeason,
              episode.airedEpisodeNumber
            )
            list.add(
              Episode(
                Ids(
                  tvdbId = episode.id,
                ),
                GeneralInfo(
                  title = episodeTitle,
                  backdrop = if (episode.filename.isNullOrEmpty()) null else "https://thetvdb.com/banners/" + episode.filename,
                  poster = null,
                  overview = episode.overview,
                  releaseDateMsUTC = episode.firstAired?.iso8601ToMillis(),
                  originalTitle = episodeTitle
                ),
                seasonNumber = episode.airedSeason,
                episodeNumber = episode.airedEpisodeNumber,
                showIds = show.ids,
                showOriginTitle = show.generalInfo.title ?: ""
              )
            )
          }
          if (page < (it.links?.last ?: -1)) {
            list.addAll(getTvdbEpisodes(show, page + 1))
          }
        }
      }
    }
    return list
  }

  suspend fun getSeason(show: Show): List<Season> {
    val list = mutableListOf<Season>()
    if (show.ids.tmdbId != null) {
      val response = tmdb.tvService().tv(show.ids.tmdbId!!, language).execute()
      if (response.isSuccessful) {
        val tvShow = response.body();
        tvShow?.seasons?.sortedBy { season -> season.season_number }?.forEach { season ->
          if (season?.season_number != null) {
            list.add(
              Season(
                season.name,
                season.season_number,
                season.overview,
                season.episode_count,
                season.poster_path,
                show.generalInfo.backdrop,
                null,
                show.ids,
                show.generalInfo.originalTitle,
                season.air_date?.time
              )
            )
          }
        }
      }

    } else if (show.ids.tvdbId != null) {
      val episodes = getTvdbEpisodes(show, 1)
      val seasons = episodes.groupBy { it.seasonNumber }
        .map {
          Season(
            null,
            it.key,
            null,
            it.value.size,
            show.generalInfo.backdrop,
            null,
            it.value,
            show.ids,
            show.generalInfo.originalTitle,
            null
          )
        }
      list.addAll(seasons);
    } else
      throw Exception("No ids found")
    return list
  }

  private suspend fun getNetworks(
    networks: Map<Int, String>,
    page: Int,
    pageSize: Int,
    sortBy: com.uwetrottmann.tmdb2.enumerations.SortBy?
  ): List<MediaItemsContainer> {
    return networks.toPage(page, pageSize).map { network ->
      val data = PagedData.Continuous<AVPMediaItem> {
        val continuation = it?.toInt() ?: 1
        val discover = tmdb.discoverTv()
          .with_networks(DiscoverFilter(network.key))
          .page(continuation)
          .language(language)
          .watch_region(region)
          .build()
          .execute()
          .body()
        Page(
          discover?.toMediaItemsList() ?: emptyList(),
          if (discover?.results.isNullOrEmpty()) null else (continuation + 1).toString()
        )
      }
      data.loadFirst()
      toMediaItemsContainer(network.value, network.value, data)
    }.toList()
  }

  private suspend fun getCompanies(
    companies: Map<Int, String>,
    page: Int,
    pageSize: Int,
    sortBy: com.uwetrottmann.tmdb2.enumerations.SortBy?
  ): List<MediaItemsContainer> {
    return companies.toPage(page, pageSize).map { company ->
      val data = PagedData.Continuous<AVPMediaItem> {
        val continuation = it?.toInt() ?: 1
        val discover = tmdb.discoverMovie()
          .with_companies(DiscoverFilter(company.key))
          .page(continuation)
          .language(language)
          .region(region)
          .includeAdult()
          .build()
          .execute()
          .body()
        Page(
          discover?.toMediaItemsList() ?: emptyList(),
          if (discover?.results.isNullOrEmpty()) null else (continuation + 1).toString()
        )
      }
      data.loadFirst()
      toMediaItemsContainer(company.value, company.value, data)
    }.toList()
  }
}
