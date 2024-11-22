package cloud.app.vvf.ui.detail.show.episode

import androidx.paging.PagingSource
import androidx.paging.PagingState
import cloud.app.vvf.common.models.AVPMediaItem

class EpisodePagingSource(
  private val episodes: List<AVPMediaItem.EpisodeItem>
) : PagingSource<Int, AVPMediaItem.EpisodeItem>() {

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AVPMediaItem.EpisodeItem> {
    val page = params.key ?: 0
    val pageSize = params.loadSize

    // Calculate the start and end indices for the page
    val startIndex = page * pageSize
    val endIndex = minOf(startIndex + pageSize, episodes.size)

    return try {
      // Check if we've reached the end of the list
      if (startIndex >= episodes.size) {
        LoadResult.Page(
          data = emptyList(),
          prevKey = if (page == 0) null else page - 1,
          nextKey = null
        )

      } else {
        LoadResult.Page(
          data = episodes.subList(startIndex, endIndex),
          prevKey = if (page == 0) null else page - 1,
          nextKey = if (endIndex == episodes.size) null else page + 1
        )
      }
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }

  override fun getRefreshKey(state: PagingState<Int, AVPMediaItem.EpisodeItem>): Int? {
    // Return the initial key (page number) for refreshing.
    return state.anchorPosition?.let { anchorPosition ->
      state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
        ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
    }
  }
}
