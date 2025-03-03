package cloud.app.vvf.extension.builtIn

import cloud.app.vvf.common.clients.mvdatabase.DatabaseClient
import cloud.app.vvf.common.clients.streams.StreamClient
import cloud.app.vvf.common.helpers.ImportType
import cloud.app.vvf.common.helpers.Page
import cloud.app.vvf.common.helpers.PagedData
import cloud.app.vvf.common.helpers.network.HttpHelper
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.AVPMediaItem.Companion.toMediaItemsContainer
import cloud.app.vvf.common.models.Actor
import cloud.app.vvf.common.models.ExtensionMetadata
import cloud.app.vvf.common.models.ExtensionType
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.common.models.MediaItemsContainer
import cloud.app.vvf.common.models.SearchItem
import cloud.app.vvf.common.models.SortBy
import cloud.app.vvf.common.models.subtitle.SubtitleData
import cloud.app.vvf.common.models.Tab
import cloud.app.vvf.common.models.movie.Episode
import cloud.app.vvf.common.models.movie.GeneralInfo
import cloud.app.vvf.common.models.movie.Ids
import cloud.app.vvf.common.models.movie.Season
import cloud.app.vvf.common.models.movie.Show
import cloud.app.vvf.common.models.stream.PremiumType
import cloud.app.vvf.common.models.stream.StreamData
import cloud.app.vvf.common.settings.PrefSettings
import cloud.app.vvf.common.settings.Setting
import cloud.app.vvf.common.settings.SettingCategory
import cloud.app.vvf.common.settings.SettingList
import cloud.app.vvf.common.settings.SettingSwitch
import cloud.app.vvf.common.settings.SettingTextInput
import cloud.app.vvf.network.api.trakt.AppTrakt
import cloud.app.vvf.extension.builtIn.tmdb.AppTmdb
import cloud.app.vvf.extension.builtIn.tmdb.SearchSuggestion
import cloud.app.vvf.extension.builtIn.tmdb.companies
import cloud.app.vvf.extension.builtIn.tmdb.formatSeasonEpisode
import cloud.app.vvf.extension.builtIn.tmdb.iso8601ToMillis
import cloud.app.vvf.extension.builtIn.tmdb.languageI3691Map
import cloud.app.vvf.extension.builtIn.tmdb.model.WatchProviders
import cloud.app.vvf.extension.builtIn.tmdb.movieGenres
import cloud.app.vvf.extension.builtIn.tmdb.networks
import cloud.app.vvf.extension.builtIn.tmdb.popularCountriesIsoToEnglishName
import cloud.app.vvf.extension.builtIn.tmdb.showGenres
import cloud.app.vvf.extension.builtIn.tmdb.toMediaItem
import cloud.app.vvf.extension.builtIn.tmdb.toMediaItemsList
import cloud.app.vvf.extension.builtIn.tvdb.AppTheTvdb
import com.uwetrottmann.tmdb2.entities.AppendToResponse
import com.uwetrottmann.tmdb2.entities.DiscoverFilter
import com.uwetrottmann.tmdb2.enumerations.AppendToResponseItem
import com.uwetrottmann.tmdb2.enumerations.MediaType
import com.uwetrottmann.tmdb2.enumerations.TimeWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class BuiltInClient : DatabaseClient, StreamClient {

  companion object {

    val metadata = ExtensionMetadata(
      className = "cloud.app.vvf.plugin.BuiltInDatabaseClient",
      path = "",
      importType = ImportType.BuiltIn,
      name = "The extension of TMDB",
      description = "BuiltInClient is TMDB's core media provider. It handles searching, metadata retrieval, applying JustWatch as a purchase streaming option, and more.",
      version = "1.0.0",
      author = "Avp",
      iconUrl = "https://www.freepnglogos.com/uploads/netflix-logo-0.png",
      types = listOf(ExtensionType.DATABASE, ExtensionType.STREAM)
    )

    private fun List<MediaItemsContainer>.toPaged() = PagedData.Single { this }
    val sortMapper: Map<String, com.uwetrottmann.tmdb2.enumerations.SortBy> = mapOf(
      SortBy.POPULARITY.serializedName to com.uwetrottmann.tmdb2.enumerations.SortBy.POPULARITY_DESC,
      SortBy.RELEASED.serializedName to com.uwetrottmann.tmdb2.enumerations.SortBy.RELEASE_DATE_DESC,
      SortBy.RATING.serializedName to com.uwetrottmann.tmdb2.enumerations.SortBy.VOTE_AVERAGE_DESC,
      SortBy.TITLE.serializedName to com.uwetrottmann.tmdb2.enumerations.SortBy.ORIGINAL_TITLE_DESC,
      // Add other mappings as necessary
    )
    const val pageSize = 3;

    const val PREF_TMDB_API_KEY = "pref_tmdb_api_key"
    const val PREF_TVDB_API_KEY = "pref_tvdb_api_key"
    const val PREF_INCLUDE_ADULT = "pref_include_adult"
    const val PREF_METADATA_LANGUAGE = "pref_metadata_language"
    const val PREF_REGION = "pref_region"
    const val PREF_SHOW_SPECIAL_SEASON = "pref_show_special_season_key"
    const val PREF_SHOW_UNAIRD_EPISODE = "show_unaired_episode_key"
  }

  private val includeAdult get() = prefSettings.getBoolean(PREF_INCLUDE_ADULT)
  private val region get() = prefSettings.getString(PREF_REGION)
  private val language get() = prefSettings.getString(PREF_METADATA_LANGUAGE)
  private val showSpecialSeason get() = prefSettings.getBoolean(PREF_SHOW_SPECIAL_SEASON)
  private val showUnairedEpisode get() = prefSettings.getBoolean(PREF_SHOW_UNAIRD_EPISODE)


  private lateinit var tmdb: AppTmdb
  private lateinit var tvdb: AppTheTvdb
  private lateinit var trakt: AppTrakt
  override val defaultSettings: List<Setting> = listOf(
    SettingCategory(
      title = "API Access",
      key = "",
      items = listOf(
        SettingTextInput(
          "TMDB API Key", PREF_TMDB_API_KEY,
          "Enter your TMDB API key to access movie and TV show data from The Movie Database.",
          defaultValue = "4ef60b9d635f533695cbcaccb6603a57"
        ),
        SettingTextInput(
          "TVDB API Key", PREF_TVDB_API_KEY,
          "Enter your TVDB API key to access data from The TV Database for TV shows.",
          defaultValue = null
        )
      )
    ),

    SettingCategory(
      title = "Media content", key = "media_content_key",
      items = listOf(
//        SettingSwitch(
//          "Show English Movie/TV Show Only",
//          "show_english_media_only_key",
//          "Only display movies and TV shows in English.",
//          false
//        ),
        SettingSwitch(
          "Include Adult Content",
          PREF_INCLUDE_ADULT,
          "Include adult-rated movies and TV shows in your library.",
          false
        ),
        SettingSwitch(
          "Show Special Seasons",
          PREF_SHOW_SPECIAL_SEASON,
          "Display special seasons for TV shows, such as behind-the-scenes or bonus episodes.",
          true
        ),
        SettingSwitch(
          "Show Unaired Episodes",
          PREF_SHOW_UNAIRD_EPISODE,
          "Include episodes that are scheduled to air but haven't been broadcast yet.",
          true
        ),
        SettingList(
          "Metadata Language",
          PREF_METADATA_LANGUAGE,
          "Select the language for displaying movie and TV show metadata, such as titles, descriptions, and other details.",
          entryTitles = languageI3691Map.values.toList(),
          entryValues = languageI3691Map.keys.toList(),
          defaultEntryIndex = 0
        )
      )
    ),

    SettingCategory(
      title = "JustWatch",
      key = "",
      items = listOf(
        SettingList(
          "Region",
          PREF_REGION,
          "Select the region to filter content availability based on your location.",
          entryTitles = popularCountriesIsoToEnglishName.values.toList(),
          entryValues = popularCountriesIsoToEnglishName.keys.toList(),
          defaultEntryIndex = 0
        )
      )
    ),

    )

//  tmdb = AppTmdb(
//  httpHelper.okHttpClient,
//  prefSettings.getString(PREF_TMDB_API_KEY) ?: "4ef60b9d635f533695cbcaccb6603a57"
//  )
//  tvdb = AppTheTvdb(
//  httpHelper.okHttpClient,
//  prefSettings.getString(PREF_TVDB_API_KEY) ?: "4ef60b9d635f533695cbcaccb6603a57",
//  prefSettings,
//  )

  private lateinit var prefSettings: PrefSettings

  override

  fun init(prefSettings: PrefSettings, httpHelper: HttpHelper) {
    this.prefSettings = prefSettings
    tmdb = AppTmdb(
      httpHelper.okHttpClient,
      prefSettings.getString(PREF_TMDB_API_KEY) ?: "4ef60b9d635f533695cbcaccb6603a57"
    )
    tvdb = AppTheTvdb(
      httpHelper.okHttpClient,
      prefSettings.getString(PREF_TVDB_API_KEY) ?: "4ef60b9d635f533695cbcaccb6603a57",
      prefSettings,
    )
    trakt = AppTrakt(
      httpHelper.okHttpClient,
      "4ef60b9d635f533695cbcaccb6603a57",
      "4ef60b9d635f533695cbcaccb6603a57",
      prefSettings
    )
  }

  override fun onSettingsChanged(key: String, value: Any) {
    when (key) {
      PREF_TVDB_API_KEY -> {
        tvdb.apiKey(value.toString())
      }

      PREF_TMDB_API_KEY -> {
        tmdb.apiKey(value.toString())
      }
    }
  }


  override suspend fun onExtensionSelected() {
    TODO("TMDB EXTENSION Not yet implemented")
  }


  override suspend fun getHomeTabs(): List<Tab> =
    listOf("Features", "Movies", "TV Shows", "Networks", "Companies").map { Tab(it, it) }

  override fun getHomeFeed(tab: Tab?) = PagedData.Continuous<MediaItemsContainer> {
    val page = it?.toInt() ?: 1
    val sortBy = tab?.extras?.get("sort_by")?.let { value ->
      sortMapper[value]
    }
    val items = when (tab?.id) {
      "Features" -> getFeatures(page, pageSize, sortBy)
      "Movies" -> getMoviesList(movieGenres, page, pageSize, sortBy)
      "TV Shows" -> getShowList(showGenres, page, pageSize, sortBy)
      "Networks" -> getNetworks(networks, page, pageSize, sortBy)
      "Companies" -> getCompanies(companies, page, pageSize, sortBy)
      else -> TODO()
    }

    val continuation = if (items.size < pageSize) null else (page + 1).toString()
    Page(items, continuation)
  }

  private suspend fun getFeatures(
    page: Int,
    pageSize: Int,
    sortBy: com.uwetrottmann.tmdb2.enumerations.SortBy?
  ): List<MediaItemsContainer> {
    if(page != 1) return emptyList()

    val result = mutableListOf<MediaItemsContainer>()
    //get trending movie/show here
    val trending = withContext(Dispatchers.IO) {
      val trendingResponse = tmdb .trendingService().trendingAll(TimeWindow.DAY).execute()
      if (trendingResponse.isSuccessful) {
        val list = trendingResponse.body()?.results?.mapNotNull {
          when (it.media_type) {
            MediaType.MOVIE -> it.movie?.toMediaItem()
            MediaType.TV -> it.tvShow?.toMediaItem()
            MediaType.PERSON -> it.person?.toMediaItem()
          }
        }
        MediaItemsContainer.PageView(
          "Trending",
          items = list ?: listOf()
        )
      } else null
    }
    trending?.let { result.add(it) }

    val popularMovies = withContext(Dispatchers.IO) {
      val data = PagedData.Continuous<AVPMediaItem> {
        val continuation = it?.toInt() ?: 1
        val items = withContext(Dispatchers.IO) {
          val builder = tmdb.discoverMovie()
            .sort_by(sortBy)
            .page(continuation)
            .language(language)
            .region(region)
          if (includeAdult == true)
            builder.includeAdult()
          builder.build().execute().body()!!
        }
        Page(
          items.toMediaItemsList(),
          if (items.results.isNullOrEmpty()) null else (continuation + 1).toString()
        )
      }
      data.loadFirst()
      toMediaItemsContainer(
        "Popular movies",
        "Popular movies",
        data
      )
    }
    result.add(popularMovies)

    val popularTvShows = withContext(Dispatchers.IO) {
      val data = PagedData.Continuous<AVPMediaItem> {
        val continuation = it?.toInt() ?: 1
        val items = withContext(Dispatchers.IO) {
          val builder = tmdb.discoverTv()
            .sort_by(sortBy)
            .page(continuation)
            .language(language)
          builder.build().execute().body()!!
        }
        Page(
          items.toMediaItemsList(),
          if (items.results.isNullOrEmpty()) null else (continuation + 1).toString()
        )
      }
      data.loadFirst()
      toMediaItemsContainer(
        "Popular tvShows",
        "Popular tvShows",
        data
      )
    }
    result.add(popularTvShows)


    val popularActors = withContext(Dispatchers.IO) {
      val data = PagedData.Continuous<AVPMediaItem> {
        val continuation = it?.toInt() ?: 1
        val items = withContext(Dispatchers.IO) {
          tmdb.personService().popular(continuation).execute().body()!!
        }
        Page(
          items.toMediaItemsList(),
          if (items.results.isNullOrEmpty()) null else (continuation + 1).toString()
        )
      }
      data.loadFirst()
      toMediaItemsContainer(
        "Popular Actors",
        "Popular Actors",
        data
      )
    }
    result.add(popularActors)


    return result
  }

  override suspend fun getMediaDetail(avpMediaItem: AVPMediaItem): AVPMediaItem? {
    return when (avpMediaItem) {
      is AVPMediaItem.MovieItem -> getMovieDetail(avpMediaItem.movie.ids.tmdbId)
      is AVPMediaItem.ShowItem -> getShowDetail(avpMediaItem.show.ids.tmdbId)
      is AVPMediaItem.EpisodeItem -> getEpisodeDetail(avpMediaItem.episode.ids.tmdbId)
      is AVPMediaItem.SeasonItem -> getSeasonDetail(avpMediaItem)
      is AVPMediaItem.ActorItem -> getActorDetail(avpMediaItem.actor.id)
      else -> null
    }
  }

  override fun getKnowFor(actor: AVPMediaItem.ActorItem) = PagedData.Continuous<AVPMediaItem> {
    val page = it?.toInt() ?: 1
    val items = getKnowFor(actor, page, pageSize)
    val continuation = if (items.size < pageSize) null else (page + 1).toString()
    Page(items, continuation)
  }

  override fun getRecommended(avpMediaItem: AVPMediaItem): PagedData<AVPMediaItem> {
    return when (avpMediaItem) {
      is AVPMediaItem.MovieItem -> getMovieRecommendations(avpMediaItem.movie.ids.tmdbId)
      is AVPMediaItem.ShowItem -> getShowRecommendations(avpMediaItem.show.ids.tmdbId)
      else -> emptyList<AVPMediaItem>().toPaged() // Or handle other types as needed
    }
  }

  private fun getMovieRecommendations(tmdbId: Int?): PagedData<AVPMediaItem> {
    tmdbId ?: return emptyList<AVPMediaItem>().toPaged()
    return PagedData.Continuous { page ->
      val continuation = page?.toInt() ?: 1
      val response = withContext(Dispatchers.IO) {
        tmdb.moviesService().recommendations(tmdbId, continuation, language).execute()
      }
      if (response.isSuccessful) {
        val recommendations =
          response.body()?.results?.mapNotNull { it.toMediaItem() } ?: emptyList()
        Page(
          recommendations,
          if (recommendations.isEmpty()) null else (continuation + 1).toString()
        )
      } else {
        Page(emptyList(), null) // Handle error, maybe emit to throwableFlow
      }
    }
  }

  private fun getShowRecommendations(tmdbId: Int?): PagedData<AVPMediaItem> {
    tmdbId ?: return emptyList<AVPMediaItem>().toPaged()
    return PagedData.Continuous { page ->
      val continuation = page?.toInt() ?: 1
      val response = withContext(Dispatchers.IO) {
        tmdb.tvService().recommendations(tmdbId, continuation, language).execute()
      }
      if (response.isSuccessful) {
        val recommendations =
          response.body()?.results?.mapNotNull { it.toMediaItem() } ?: emptyList()
        Page(
          recommendations,
          if (recommendations.isEmpty()) null else (continuation + 1).toString()
        )
      } else {
        Page(emptyList(), null) // Handle error, maybe emit to throwableFlow
      }
    }
  }

  private suspend fun getSeasonDetail(seasonItem: AVPMediaItem.SeasonItem): AVPMediaItem? {
    val season = seasonItem.season;

    return if (season.showIds.tmdbId != null) {
      val response = withContext(Dispatchers.IO) {
        tmdb.tvSeasonsService().season(
          season.showIds.tmdbId!!,
          season.number,
          language
        ).execute().body()
      }
      response?.let {
        AVPMediaItem.SeasonItem(
          season = Season(
            title = it.name,
            number = it.season_number ?: -1,
            overview = it.overview,
            episodeCount = it.episodes?.size ?: 0,
            posterPath = it.poster_path,
            episodes = it.episodes?.filter { episode ->
              if (showUnairedEpisode == true) {
                true
              } else
              //episode.air_date?.before(Date(System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000))) == true
                episode.air_date?.before(Date(System.currentTimeMillis())) == true
            }?.map { episode ->
              Episode(
                Ids(tmdbId = episode.id),
                GeneralInfo(
                  title = episode.name ?: "",
                  backdrop = episode.still_path,
                  poster = episode.still_path,
                  overview = episode.overview,
                  releaseDateMsUTC = episode.air_date?.time,
                  originalTitle = episode.name ?: "",
                  homepage = null,
                    voteCount = episode.vote_count,
                  voteAverage = episode.vote_average,
                  rating = episode.rating?.toDouble()
                ),
                seasonNumber = episode.season_number ?: 0,
                episodeNumber = episode.episode_number ?: 0,
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

  private suspend fun getActorDetail(tmdbId: Int?): AVPMediaItem.ActorItem? {
    tmdbId ?: return null

    val response = withContext(Dispatchers.IO) {
      tmdb.personService().summary(
        tmdbId,
        language,
        AppendToResponse(
          AppendToResponseItem.MOVIE_CREDITS,
          AppendToResponseItem.TV_CREDITS,
          AppendToResponseItem.COMBINED_CREDITS,
          AppendToResponseItem.IMAGES,
          AppendToResponseItem.EXTERNAL_IDS,
          AppendToResponseItem.CONTENT_RATINGS
        )
      ).execute().body()
    }

    return response?.let {
      AVPMediaItem.ActorItem(
        actor = Actor(
          name = it.name ?: "unknown",
          id = it.id,
          role = it.also_known_as?.toString(),
          image = it.profile_path?.toImageHolder(),
        ),
      )
    }
  }

  private fun getEpisodeDetail(tmdbId: Int?): AVPMediaItem.EpisodeItem? {
    TODO("Not yet implemented")
  }

  private suspend fun getShowDetail(tmdbId: Int?): AVPMediaItem.ShowItem? {
    tmdbId ?: return null
    val response = withContext(Dispatchers.IO) {
      tmdb.tvService().tv(
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
          AppendToResponseItem.REVIEWS,
          AppendToResponseItem.CONTENT_RATINGS
        )
      ).execute()
    }

    val body = response.body()
    val showItem = body?.toMediaItem()

    if (showSpecialSeason == false) {
      val seasons = showItem?.show?.seasons?.filter { season -> season.number > 0 }
      showItem?.show?.seasons = seasons
    }
    return showItem

  }

  private suspend fun getMovieDetail(tmdbId: Int?): AVPMediaItem.MovieItem? {

    tmdbId ?: return null
    val response = withContext(Dispatchers.IO) {
      tmdb.moviesService().summary(
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
          AppendToResponseItem.REVIEWS,
          AppendToResponseItem.CONTENT_RATINGS
        )
      ).execute()
    }
    return response.body()?.toMediaItem()
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
      val items = withContext(Dispatchers.IO) {
        val builder = tmdb.discoverMovie()
          .sort_by(sortBy)
          .with_genres(DiscoverFilter(genre.key))
          .page(continuation)
          .language(language)
          .region(region)


        if (includeAdult == true)
          builder.includeAdult()
        builder.build().execute().body()!!
      }
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
      val more = withContext(Dispatchers.IO) {
        tmdb.discoverTv()
          .sort_by(sortBy)
          .with_genres(DiscoverFilter(genre.key))
          .page(continuation)
          .language(language)
          .watch_region(region)
          .build().execute().body()!!
      }
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
  override suspend fun quickSearch(query: String): List<SearchItem> {
    val results = mutableListOf<SearchItem>()
    val searchResult = SearchSuggestion.search(query)
    searchResult?.d?.forEach {
      results.add(SearchItem(it.l, false, System.currentTimeMillis()))
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
      val pageResult = withContext(Dispatchers.IO) {
        tmdb.searchService()
          .person(query, continuation, language, region, includeAdult)
          .execute()
          .body()
      }
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
      val pageResult = withContext(Dispatchers.IO) {
        tmdb.searchService()
          .tv(query, continuation, language, firstAirDateYear, includeAdult)
          .execute()
          .body()
      }
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
        withContext(Dispatchers.IO) {
          tmdb.searchService()
            .movie(query, continuation, language, region, includeAdult, year, year)
            .execute().body()
        }
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
      withContext(Dispatchers.IO) {
        tmdb.searchService().multi(query, 1, language, region, includeAdult).execute().body()
      }
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
              episode.airedSeason ?: 0,
              episode.airedEpisodeNumber ?: 0
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
                  originalTitle = episodeTitle,
                  homepage = null,
                  voteCount = episode.siteRatingCount,
                  voteAverage = episode.siteRating,
                  rating =  episode.siteRating
                ),
                seasonNumber = episode.airedSeason ?: 0,
                episodeNumber = episode.airedEpisodeNumber ?: 0,
                showIds = show.ids,
                showOriginTitle = show.generalInfo.title
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
      val response =
        withContext(Dispatchers.IO) { tmdb.tvService().tv(show.ids.tmdbId!!, language).execute() }
      if (response.isSuccessful) {
        val tvShow = response.body();
        tvShow?.seasons?.sortedBy { season -> season.season_number }?.forEach { season ->
          if (season?.season_number != null) {
            list.add(
              Season(
                season.name,
                season.season_number ?: 0,
                season.overview,
                season.episode_count ?: 0,
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

  private suspend fun getKnowFor(
    actor: AVPMediaItem.ActorItem,
    page: Int,
    pageSize: Int
  ): List<AVPMediaItem> {
    val personID = actor.actor.id ?: return emptyList()
    val response = withContext(Dispatchers.IO) {
      tmdb.personService().combinedCredits(personID, language).execute()
    }
    if (response.isSuccessful) {
      val credits = response.body()
      val cast = credits?.cast
      if (cast != null) {
        val startIndex = (page - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, cast.size)
        return cast.subList(startIndex, endIndex).flatMap {
          listOfNotNull(
            it.media?.movie?.toMediaItem(),
            it.media?.tvShow?.toMediaItem()
          )
        }
      }
    }
    return emptyList()
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
        val discover = withContext(Dispatchers.IO) {
          tmdb.discoverTv()
            .with_networks(DiscoverFilter(network.key))
            .page(continuation)
            .language(language)
            .watch_region(region)
            .build()
            .execute()
            .body()
        }
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
        val discover = withContext(Dispatchers.IO) {
          val builder = tmdb.discoverMovie()
            .with_companies(DiscoverFilter(company.key))
            .page(continuation)
            .language(language)
            .region(region)
          if (includeAdult == true)
            builder.includeAdult()

          builder.build()
            .execute()
            .body()
        }
        Page(
          discover?.toMediaItemsList() ?: emptyList(),
          if (discover?.results.isNullOrEmpty()) null else (continuation + 1).toString()
        )
      }
      data.loadFirst()
      toMediaItemsContainer(company.value, company.value, data)
    }.toList()
  }


  override suspend fun loadLinks(
    mediaItem: AVPMediaItem,
    subtitleCallback: (SubtitleData) -> Unit,
    callback: (StreamData) -> Unit
  ): Boolean {

    when (mediaItem) {
      is AVPMediaItem.MovieItem -> {
        val tmdbId = mediaItem.movie.ids.tmdbId
        if (tmdbId != null) {
          val response = tmdb.extendService().watchMovieProviders(tmdbId).execute()
          if (response.isSuccessful) {
            val results = response.body()?.results?.get(region)
            processProviderTypes(results, callback)
          }
        }
      }

      is AVPMediaItem.EpisodeItem -> {
        val tmdbId = mediaItem.seasonItem.showItem.show.ids.tmdbId
        if (tmdbId != null) {
          val response = tmdb.extendService().watchTvProviders(tmdbId).execute()
          if (response.isSuccessful) {
            val results = response.body()?.results?.get(region)
            processProviderTypes(results, callback)
          }
        }
      }

      else -> {
        // Handle other cases if necessary
      }
    }
    return true
  }

  private fun processProviderTypes(
    providerRegion: WatchProviders.CountryInfo?,
    callback: (StreamData) -> Unit
  ) {
    providerRegion?.let {
      val link = it.link ?: ""
      it.flatrate.forEach { provider ->
        callback(createStreamData(provider, "Subscription", link))
      }
      it.buy.forEach { provider ->
        callback(createStreamData(provider, "Buy", link))
      }
      it.ads.forEach { provider ->
        callback(createStreamData(provider, "Ad-supported", link))
      }
      it.free.forEach { provider ->
        callback(createStreamData(provider, "Free", link))
      }
      it.rent.forEach { provider ->
        callback(createStreamData(provider, "Rent", link))
      }
    }
  }

  private fun createStreamData(
    provider: WatchProviders.WatchProvider,
    type: String,
    link: String
  ): StreamData {
    return StreamData(
      originalUrl = link,
      providerName = "${provider.provider_name} ($type)",
      providerLogo = "https://image.tmdb.org/t/p/w500${provider.logo_path}",
      hostLogo = "https://www.justwatch.com/appassets/img/jw-icon.png",
      premiumType = PremiumType.JustWatch.ordinal,
    )
  }

}
