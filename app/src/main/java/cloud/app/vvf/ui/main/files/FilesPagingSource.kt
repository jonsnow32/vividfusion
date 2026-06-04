package cloud.app.vvf.ui.main.files

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.paging.PagingSource
import androidx.paging.PagingState
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.music.Artist
import cloud.app.vvf.common.models.music.Track
import cloud.app.vvf.common.models.video.Video
import java.util.concurrent.TimeUnit

class FilesPagingSource(
  private val context: Context,
  private val filter: MediaFilter,
  private val query: String,
) : PagingSource<Int, AVPMediaItem>() {

  private data class MediaRef(val id: Long, val isVideo: Boolean, val dateAdded: Long)

  private val allRefs: List<MediaRef> by lazy { buildSortedRefs() }

  override fun getRefreshKey(state: PagingState<Int, AVPMediaItem>): Int? {
    return state.anchorPosition?.let { anchor ->
      state.closestPageToPosition(anchor)?.let { page ->
        page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
      }
    }
  }

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AVPMediaItem> {
    return try {
      val page = params.key ?: 0
      val pageSize = params.loadSize
      val from = page * pageSize
      val total = allRefs.size
      val slice = allRefs.subList(from.coerceAtMost(total), (from + pageSize).coerceAtMost(total))

      val videoIds = slice.filter { it.isVideo }.map { it.id }
      val audioIds = slice.filter { !it.isVideo }.map { it.id }

      val videos = if (videoIds.isNotEmpty()) loadVideos(videoIds) else emptyMap()
      val audios = if (audioIds.isNotEmpty()) loadAudios(audioIds) else emptyMap()

      val items = slice.mapNotNull { ref ->
        if (ref.isVideo) videos[ref.id] else audios[ref.id]
      }

      LoadResult.Page(
        data = items,
        prevKey = if (page == 0) null else page - 1,
        nextKey = if (from + pageSize >= total) null else page + 1
      )
    } catch (e: Exception) {
      LoadResult.Error(e)
    }
  }

  private fun buildSortedRefs(): List<MediaRef> {
    val refs = mutableListOf<MediaRef>()
    val trimmedQuery = query.trim()

    if (filter != MediaFilter.AUDIO) {
      val (selection, args) = if (trimmedQuery.isNotEmpty())
        "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ?" to arrayOf("%$trimmedQuery%")
      else null to null
      context.contentResolver.query(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DATE_ADDED),
        selection, args, null
      )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        while (cursor.moveToNext()) {
          refs.add(MediaRef(cursor.getLong(idCol), isVideo = true, cursor.getLong(dateCol)))
        }
      }
    }

    if (filter != MediaFilter.VIDEO) {
      val (selection, args) = if (trimmedQuery.isNotEmpty())
        "${MediaStore.Audio.Media.TITLE} LIKE ?" to arrayOf("%$trimmedQuery%")
      else null to null
      context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATE_ADDED),
        selection, args, null
      )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        while (cursor.moveToNext()) {
          refs.add(MediaRef(cursor.getLong(idCol), isVideo = false, cursor.getLong(dateCol)))
        }
      }
    }

    return refs.sortedByDescending { it.dateAdded }
  }

  private fun loadVideos(ids: List<Long>): Map<Long, AVPMediaItem.VideoItem> {
    val result = mutableMapOf<Long, AVPMediaItem.VideoItem>()
    val selection = "${MediaStore.Video.Media._ID} IN (${ids.joinToString(",")})"
    val projection = arrayOf(
      MediaStore.Video.Media._ID,
      MediaStore.Video.Media.DISPLAY_NAME,
      MediaStore.Video.Media.DURATION,
      MediaStore.Video.Media.SIZE,
      MediaStore.Video.Media.DATE_ADDED,
      MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
    )
    context.contentResolver.query(
      MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
      projection, selection, null, null
    )?.use { cursor ->
      val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
      val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
      val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
      val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
      val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
      val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
      while (cursor.moveToNext()) {
        val id = cursor.getLong(idCol)
        val name = cursor.getString(nameCol) ?: continue
        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
        val video = Video.LocalVideo(
          uri = contentUri.toString(),
          title = name.substringBeforeLast("."),
          duration = cursor.getLong(durationCol),
          thumbnailUri = contentUri.toString(),
          id = id.toString(),
          fileSize = cursor.getLong(sizeCol),
          dateAdded = cursor.getLong(dateCol),
          album = cursor.getString(bucketCol) ?: "",
        )
        result[id] = AVPMediaItem.VideoItem(video)
      }
    }
    return result
  }

  private fun loadAudios(ids: List<Long>): Map<Long, AVPMediaItem.TrackItem> {
    val result = mutableMapOf<Long, AVPMediaItem.TrackItem>()
    val selection = "${MediaStore.Audio.Media._ID} IN (${ids.joinToString(",")})"
    val projection = arrayOf(
      MediaStore.Audio.Media._ID,
      MediaStore.Audio.Media.TITLE,
      MediaStore.Audio.Media.ARTIST,
      MediaStore.Audio.Media.ALBUM,
      MediaStore.Audio.Media.ALBUM_ID,
      MediaStore.Audio.Media.DURATION,
      MediaStore.Audio.Media.DATE_ADDED,
    )
    context.contentResolver.query(
      MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
      projection, selection, null, null
    )?.use { cursor ->
      val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
      val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
      val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
      val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
      val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
      val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
      while (cursor.moveToNext()) {
        val id = cursor.getLong(idCol)
        val title = cursor.getString(titleCol) ?: continue
        val artist = cursor.getString(artistCol)?.takeIf { it != "<unknown>" } ?: ""
        val album = cursor.getString(albumCol)
        val albumId = cursor.getLong(albumIdCol)
        val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        val albumArtUri = ContentUris.withAppendedId(
          Uri.parse("content://media/external/audio/albumart"), albumId
        ).toString()
        val track = Track(
          uri = contentUri.toString(),
          id = id.toString(),
          title = title,
          artists = if (artist.isNotEmpty()) listOf(Artist(id = artist, name = artist)) else emptyList(),
          album = album,
          cover = albumArtUri,
          duration = cursor.getLong(durationCol),
        )
        result[id] = AVPMediaItem.TrackItem(track)
      }
    }
    return result
  }

  companion object {
    fun formatDuration(durationMs: Long?): String {
      if (durationMs == null || durationMs <= 0) return ""
      val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
      val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
      val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
      return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
      else "%d:%02d".format(minutes, seconds)
    }
  }
}
