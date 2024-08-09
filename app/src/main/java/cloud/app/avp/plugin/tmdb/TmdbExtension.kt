package cloud.app.avp.plugin.tmdb

import cloud.app.avp.network.api.tmdb.AppTmdb
import cloud.app.common.clients.BaseExtension
import cloud.app.common.clients.infos.FeedClient
import cloud.app.common.clients.infos.SearchClient
import cloud.app.common.helpers.Page
import cloud.app.common.helpers.PagedData
import cloud.app.common.models.AVPMediaItem
import cloud.app.common.models.AVPMediaItem.Companion.toMediaItemsContainer
import cloud.app.common.models.MediaItemsContainer
import cloud.app.common.models.QuickSearchItem
import cloud.app.common.models.Tab
import cloud.app.common.settings.Setting
import cloud.app.common.settings.SettingList
import cloud.app.common.settings.SettingSwitch
import cloud.app.common.settings.Settings
import com.uwetrottmann.tmdb2.entities.CastMember
import com.uwetrottmann.tmdb2.entities.DiscoverFilter
import com.uwetrottmann.tmdb2.entities.Movie
import com.uwetrottmann.tmdb2.entities.TvShow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TmdbExtension(val tmdb: AppTmdb) : FeedClient, BaseExtension, SearchClient {
  override val settingItems: List<Setting> = listOf(
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
    )
  )

  override fun setSettings(settings: Settings) {
    TODO("Not yet implemented")
  }

  override suspend fun onExtensionSelected() {
    TODO("TMDB EXTENSION Not yet implemented")
  }


  override suspend fun getHomeTabs(): List<Tab> =
    listOf("Movies", "TV Shows", "Actors").map { Tab(it, it) }

  override  fun getHomeFeed(tab: Tab?): PagedData<MediaItemsContainer> {
    return when (tab?.id) {
      "Movies" -> loadMoviesFeed()
      "TV Shows" -> loadTvShowsFeed()
      "Actors" -> loadActorsFeed()
      else -> loadMoviesFeed()
    }
  }

  private fun loadActorsFeed(): PagedData<MediaItemsContainer> {
    TODO("Not yet implemented")
  }

  private fun loadTvShowsFeed() = PagedData.Continuous<MediaItemsContainer> {
      val page = it?.toInt() ?: 1
      val items = getShowList(showGenres, page, pageSize)
      val continuation = if (items.size < pageSize) null else (page + 1).toString()
      Page(items, continuation)

  }

  private fun loadMoviesFeed() = PagedData.Continuous<MediaItemsContainer> {
      val page = it?.toInt() ?: 1
      val items = getMoviesList(movieGenres, page, pageSize)
      val continuation = if (items.size < pageSize) null else (page + 1).toString()
      Page(items, continuation)
  }

  private fun Map<Int, String>.toPage(page: Int, pageSize: Int): Map<Int, String> {
    val startIndex = (page - 1) * pageSize
    val endIndex = minOf(startIndex + pageSize, size)
    return entries.toList().subList(startIndex, endIndex).associate { it.key to it.value }
  }

  private suspend fun getMoviesList(genres: Map<Int, String>, page: Int, pageSize: Int) =
    genres.toPage(page, pageSize).map { genre ->
      val data = PagedData.Continuous<AVPMediaItem> {
        withContext(Dispatchers.IO) {
          val continuation = it?.toInt() ?: 1
          val more = tmdb.discoverMovie()
            //.sort_by(SortBy.entries.first { value -> value.toString() == tab?.extras?.get("sort_by") })
            .with_genres(DiscoverFilter(genre.key))
            .page(continuation)
            .build().execute().body()!!
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


  private suspend fun getShowList(genres: Map<Int, String>, page: Int, pageSize: Int) =
    genres.toPage(page, pageSize).map { genre ->
      val data = PagedData.Continuous<AVPMediaItem> {
        withContext(Dispatchers.IO) {
          val continuation = it?.toInt() ?: 1
          val more = tmdb.discoverTv()
            //.sort_by(SortBy.entries.first { value -> value.toString() == tab?.extras?.get("sort_by") })
            .with_genres(DiscoverFilter(genre.key))
            .page(continuation)
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
  override suspend fun quickSearch(query: String?): List<QuickSearchItem> {
    val results = mutableListOf<QuickSearchItem>()
    return withContext(Dispatchers.IO) {
      val mediaResultsPage = tmdb.searchService().multi(query, 1, "en", "en_US", true).execute().body()
      mediaResultsPage?.results?.forEach {
        it.movie?.let { movie ->
          results.add(QuickSearchItem.SearchMediaItem(movie.toMediaItem()))
        }
        it.tvShow?.let { tvShow ->
          results.add(QuickSearchItem.SearchMediaItem(tvShow.toMediaItem()))
        }
        it.person?.let { person ->
          results.add(QuickSearchItem.SearchMediaItem(person.toMediaItem()))
        }
      }
      results
    }
  }

  override suspend fun searchTabs(query: String?): List<Tab> =
    listOf("All", "Movies", "TV Shows", "Actors").map { Tab(it, it) }

  override fun searchFeed(query: String?, tab: Tab?): PagedData<MediaItemsContainer> {
    return when (tab?.id) {
      "Movies" -> searchMoviesFeed(query)
      "TV Shows" -> searchTvShowsFeed(query)
      "Actors" -> searchActorsFeed(query)
      else -> searchAll(query)
    }
  }

  private fun searchActorsFeed(query: String?): PagedData<MediaItemsContainer> {
    TODO("Not yet implemented")
  }

  private fun searchTvShowsFeed(query: String?): PagedData<MediaItemsContainer> {
    TODO("Not yet implemented")
  }

  private fun searchMoviesFeed(query: String?): PagedData<MediaItemsContainer> {
    TODO("Not yet implemented")
  }

  private fun List<AVPMediaItem>.toPaged() = PagedData.Single { this }
  private  fun searchAll(query: String?) = PagedData.Single {
    withContext(Dispatchers.IO) {
      val movies = mutableListOf<AVPMediaItem.MovieItem>()
      val shows = mutableListOf<AVPMediaItem.ShowItem>()
      val casts = mutableListOf<AVPMediaItem.ActorItem>()
      val mediaResultsPage = tmdb.searchService().multi(query, 1, "en", "en_US", true).execute().body()
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

    const val pageSize = 3;
  }
}
