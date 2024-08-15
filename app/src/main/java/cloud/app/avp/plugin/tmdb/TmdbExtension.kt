package cloud.app.avp.plugin.tmdb

import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.ExtensionMetadata
import cloud.app.common.clients.mvdatabase.FeedClient
import cloud.app.common.clients.mvdatabase.SearchClient
import cloud.app.common.helpers.Page
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.AVPMediaItem.Companion.toMediaItemsContainer
import cloud.app.common.models.ExtensionType
import cloud.app.common.models.LoginType
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.QuickSearchItem
import cloud.app.common.models.SortBy
import cloud.app.common.models.Tab
import cloud.app.common.settings.Setting
import cloud.app.common.settings.SettingList
import cloud.app.common.settings.SettingSwitch
import cloud.app.common.settings.Settings
import com.uwetrottmann.tmdb2.entities.DiscoverFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class TmdbExtension : FeedClient, BaseExtension, SearchClient {
  private lateinit var tmdb: AppTmdb
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

  private lateinit var settings: Settings
  override fun init(settings: Settings, okhttpClient: OkHttpClient) {
    this.settings = settings
    tmdb = AppTmdb(okHttpClient = okhttpClient, settings.getString("pref_tmdb_api_key") ?: "4ef60b9d635f533695cbcaccb6603a57")
  }

  private val includeAdult get() = settings.getBoolean("tmdb_include_adult")
  private val region get() = settings.getString("tmdb_region")
  private val language get() = settings.getString("tmdb_language")

  override suspend fun onExtensionSelected() {
    TODO("TMDB EXTENSION Not yet implemented")
  }


  override suspend fun getHomeTabs(): List<Tab> =
    listOf("Movies", "TV Shows", "Actors").map { Tab(it, it) }

  override fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer> {
    return when (tab?.id) {
      "Movies" -> loadMoviesFeed(tab.extras)
      "TV Shows" -> loadTvShowsFeed(tab.extras)
      "Actors" -> loadActorsFeed(tab.extras)
      else -> loadMoviesFeed(tab?.extras)
    }
  }

  private fun loadActorsFeed(extras: Map<String, String>?): PagedData<MediaItemsContainer> {
    TODO("Not yet implemented")
  }

  private fun loadTvShowsFeed(extras: Map<String, String>?) =
    PagedData.Continuous<MediaItemsContainer> {
      val page = it?.toInt() ?: 1
      val sortBy = extras?.get("sort_by")?.let { value ->
        sortMapper[value]
      }
      val items = getShowList(showGenres, page, pageSize, sortBy)
      val continuation = if (items.size < pageSize) null else (page + 1).toString()
      Page(items, continuation)

    }

  private fun loadMoviesFeed(extras: Map<String, String>?) =
    PagedData.Continuous<MediaItemsContainer> {
      val page = it?.toInt() ?: 1
      val sortBy = extras?.get("sort_by")?.let { value ->
        sortMapper[value]
      }
      val items = getMoviesList(movieGenres, page, pageSize, sortBy)
      val continuation = if (items.size < pageSize) null else (page + 1).toString()
      Page(items, continuation)
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
  ) =
    genres.toPage(page, pageSize).map { genre ->
      val data = PagedData.Continuous<AVPMediaItem> {
        withContext(Dispatchers.IO) {
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

          val more = builder.build().execute().body()!!
          Page(
            more.toMediaItemsList(),
            if (more.results.isNullOrEmpty()) null else (continuation + 1).toString()
          )
        }
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
  ) =
    genres.toPage(page, pageSize).map { genre ->
      val data = PagedData.Continuous<AVPMediaItem> {
        withContext(Dispatchers.IO) {
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
    return withContext(Dispatchers.IO) {
      val searchResult = SearchSuggestion.search(query)
      searchResult?.d?.forEach {
        results.add(QuickSearchItem.SearchQueryItem(it.l, false))
      }
      results
    }
  }

  override suspend fun searchTabs(query: String?): List<Tab> =
    listOf("All", "Movies", "TV Shows", "Actors").map { Tab(it, it) }

  override fun searchFeed(query: String?, tab: Tab?): PagedData<MediaItemsContainer> {
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
    TODO("Not yet implemented")
  }

  private fun searchTvShowsFeed(
    query: String?,
    extras: Map<String, String>?
  ): PagedData<MediaItemsContainer> {
    TODO("Not yet implemented")
  }

  private fun searchMoviesFeed(
    query: String?,
    extras: Map<String, String>?
  ): PagedData<MediaItemsContainer> {
    val more = PagedData.Continuous<AVPMediaItem> {
      val continuation = it?.toInt() ?: 1
      val year = extras?.get("year")?.toInt()
      val pageResult = withContext(Dispatchers.IO) {
        tmdb.searchService().movie(query, continuation, language, region, includeAdult, year, year)
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
    withContext(Dispatchers.IO) {
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
  }


  companion object {

    val showGenres: Map<Int, String> = mapOf(
      10759 to "Action & Adventure",
      16 to "Animation",
      35 to "Comedy",
      80 to "Crime",
      99 to "Documentary",
      18 to "Drama",
      10751 to "Family",
      10762 to "Kids",
      9648 to "Mystery",
      10763 to "News",
      10764 to "Reality",
      10765 to "Sci-Fi & Fantasy",
      10766 to "Soap",
      10767 to "Talk",
      10768 to "War & Politics",
      37 to "Western"
    )

    val movieGenres: Map<Int, String> = mapOf(
      28 to "Action",
      12 to "Adventure",
      16 to "Animation",
      35 to "Comedy",
      80 to "Crime",
      99 to "Documentary",
      18 to "Drama",
      10751 to "Family",
      14 to "Fantasy",
      36 to "History",
      27 to "Horror",
      10402 to "Music",
      9648 to "Mystery",
      10749 to "Romance",
      878 to "Science Fiction",
      10770 to "TV Movie",
      53 to "Thriller",
      10752 to "War",
      37 to "Western"
    )

    val sortMapper: Map<String, com.uwetrottmann.tmdb2.enumerations.SortBy> = mapOf(
      SortBy.POPULARITY.serializedName to com.uwetrottmann.tmdb2.enumerations.SortBy.POPULARITY_DESC,
      SortBy.RELEASED.serializedName to com.uwetrottmann.tmdb2.enumerations.SortBy.RELEASE_DATE_DESC,
      SortBy.RATING.serializedName to com.uwetrottmann.tmdb2.enumerations.SortBy.VOTE_AVERAGE_DESC,
      SortBy.TITLE.serializedName to com.uwetrottmann.tmdb2.enumerations.SortBy.ORIGINAL_TITLE_DESC,
      // Add other mappings as necessary
    )
    const val pageSize = 3;
  }
}
