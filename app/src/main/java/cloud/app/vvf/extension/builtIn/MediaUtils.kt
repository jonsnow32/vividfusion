package cloud.app.vvf.extension.builtIn

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import cloud.app.vvf.MainActivity
import cloud.app.vvf.R
import cloud.app.vvf.common.exceptions.AppPermissionRequiredException
import cloud.app.vvf.common.models.AVPMediaItem
import cloud.app.vvf.common.models.ImageHolder.Companion.toImageHolder
import cloud.app.vvf.common.models.music.Artist
import cloud.app.vvf.common.models.music.Playlist
import cloud.app.vvf.common.models.music.Track
import cloud.app.vvf.common.models.user.User
import cloud.app.vvf.common.models.video.Video.LocalVideo
import cloud.app.vvf.common.models.video.VideoCollection
import cloud.app.vvf.utils.checkPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import java.util.Calendar

object MediaUtils {

  private var activity = WeakReference<MainActivity>(null)
  fun setActivity(mainActivity: MainActivity) {
    activity = WeakReference(mainActivity)
  }

  private const val CLASS_NAME = "MediaUtils"

  private val videoProjection = arrayOf(
    MediaStore.Video.Media._ID,
    MediaStore.Video.Media.DISPLAY_NAME,
    MediaStore.Video.Media.DATA,
    MediaStore.Video.Media.DURATION,
    MediaStore.Video.Media.SIZE,
    MediaStore.Video.Media.DATE_ADDED,
    MediaStore.Video.Media.ALBUM,
    MediaStore.Video.Media.WIDTH,
    MediaStore.Video.Media.HEIGHT
  )

  private val audioProjection = arrayOf(
    MediaStore.Audio.Media._ID,
    MediaStore.Audio.Media.DISPLAY_NAME,
    MediaStore.Audio.Media.DATA,
    MediaStore.Audio.Media.DURATION,
    MediaStore.Audio.Media.ARTIST,
    MediaStore.Audio.Media.ALBUM,
    MediaStore.Audio.Media.DATE_ADDED
  )

  private val albumArtCache = mutableMapOf<String, String?>() // Cache album art

  fun getPlaylistThumbnail(albumName: String?): String? {
    albumName ?: return null
    return albumArtCache[albumName]
  }

  private fun getContentUris(type: String): List<Uri> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      listOf(
        if (type == "video") MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
        if (type == "video") MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_INTERNAL)
        else MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_INTERNAL)
      )
    } else {
      listOf(
        if (type == "video") MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
      )
    }
  }

  private fun queryMediaStore(
    context: Context,
    type: String,
    projection: Array<String>,
    selection: String?,
    selectionArgs: Array<String>?,
    sortOrder: String?,
    page: Int,
    pageSize: Int,
    processRow: (cursor: android.database.Cursor) -> Any?
  ): List<Any> {
    val items = mutableListOf<Any>()
    val uris = getContentUris(type)

    uris.forEach { uri ->
      context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        ?.use { cursor ->
          val startPosition = (page - 1) * pageSize
          val endPosition = minOf(startPosition + pageSize, cursor.count)

          if (startPosition >= cursor.count) return@use

          if (cursor.moveToPosition(startPosition)) {
            var currentPosition = startPosition
            do {
              val item = processRow(cursor) ?: continue
              items.add(item)
              currentPosition++
            } while (currentPosition < endPosition && cursor.moveToNext())
          }
        } ?: Timber.w("Query returned null cursor for URI: $uri")
    }

    return items
  }

  fun getAllVideos(
    context: Context,
    page: Int = 1,
    pageSize: Int = 20,
    minDuration: Long = 0
  ): List<LocalVideo> {
    require(page >= 1) { "Page number must be 1 or greater (1-based indexing)" }
    require(pageSize > 0) { "Page size must be positive" }
    require(minDuration >= 0) { "Minimum duration must be non-negative" }

    val selection = if (minDuration > 0) {
      "${MediaStore.Video.Media.DURATION} > ?"
    } else {
      null
    }
    val selectionArgs = if (minDuration > 0) arrayOf(minDuration.toString()) else null
    val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

    return queryMediaStore(
      context, "video", videoProjection, selection, selectionArgs, sortOrder, page, pageSize
    ) { cursor ->
      val id = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
        ?: return@queryMediaStore null
      val title =
        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
          ?: "Unknown Title"
      val data = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
        ?: return@queryMediaStore null
      val duration =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)) ?: 0L
      val size =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)) ?: 0L
      val dateAdded =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))
          ?.times(1000) ?: 0L
      val album = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM))
        ?: "Unknown Album"
      val width = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH))
      val height = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT))

      val thumbnailUri = ContentUris.withAppendedId(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        id.toLong()
      ).toString()

      LocalVideo(
        id = id,
        title = title,
        thumbnailUri = thumbnailUri,
        uri = data,
        duration = duration,
        fileSize = size,
        dateAdded = dateAdded,
        album = album,
        width = width,
        height = height
      )
    } as List<LocalVideo>
  }

  fun checkAudioPermission(context: Context) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Manifest.permission.READ_MEDIA_AUDIO
    } else {
      Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val isGranted = activity.get()?.checkPermission(context, permissions) == true
    if (isGranted == false) {
      throw AppPermissionRequiredException(
        BuiltInClient.javaClass.toString(),
        "BuiltInClient",
        permissions,
        message = context.getString(R.string.audio_permission_required_summary)
      )
    }
  }

  fun checkVideoPermission(context: Context) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Manifest.permission.READ_MEDIA_VIDEO
    } else {
      Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val isGranted = activity.get()?.checkPermission(context, permissions) == true
    if (isGranted == false) {
      throw AppPermissionRequiredException(
        BuiltInClient.javaClass.toString(),
        "BuiltInClient",
        permissions,
        message = context.getString(R.string.video_permission_required_summary)
      )
    }
  }

  suspend fun getVideoCollections(context: Context, minDuration: Long = 0): List<VideoCollection> {
    require(minDuration >= 0) { "Minimum duration must be non-negative" }

    val albumMap = mutableMapOf<String, MutableList<LocalVideo>>()
    val uris = getContentUris("video")

    withContext(Dispatchers.IO) {
      uris.forEach { uri ->
        val projection = arrayOf(
          MediaStore.Video.Media.ALBUM,
          MediaStore.Video.Media._ID,
          MediaStore.Video.Media.DISPLAY_NAME,
          MediaStore.Video.Media.DATA,
          MediaStore.Video.Media.DURATION,
          MediaStore.Video.Media.SIZE,
          MediaStore.Video.Media.DATE_ADDED,
          MediaStore.Video.Media.WIDTH,
          MediaStore.Video.Media.HEIGHT
        )
        val selection = if (minDuration > 0) {
          "${MediaStore.Video.Media.DURATION} > ?"
        } else {
          null
        }
        val selectionArgs = if (minDuration > 0) arrayOf(minDuration.toString()) else null
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
          ?.use { cursor ->
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
              val albumName = cursor.getStringOrNull(albumColumn) ?: "Unknown Album"
              val id = cursor.getStringOrNull(idColumn) ?: continue
              val title = cursor.getStringOrNull(titleColumn) ?: "Unknown Title"
              val data = cursor.getStringOrNull(dataColumn) ?: continue
              val duration = cursor.getLongOrNull(durationColumn) ?: 0L
              val size = cursor.getLongOrNull(sizeColumn) ?: 0L
              val dateAdded = cursor.getLongOrNull(dateAddedColumn)?.times(1000) ?: 0L
              val width = cursor.getIntOrNull(widthColumn)
              val height = cursor.getIntOrNull(heightColumn)

              val thumbnailUri = ContentUris.withAppendedId(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                id.toLong()
              ).toString()

              val video = LocalVideo(
                id = id,
                title = title,
                thumbnailUri = thumbnailUri,
                uri = data,
                duration = duration,
                fileSize = size,
                dateAdded = dateAdded,
                album = albumName,
                width = width,
                height = height
              )

              albumMap.getOrPut(albumName) { mutableListOf() }.add(video)
            }
          }
      }
    }

    return albumMap.map { (albumName, videoList) ->
      val totalDuration = videoList.sumOf { it.duration }
      val firstVideo = videoList.first()
      VideoCollection(
        id = albumName.hashCode().toString(),
        title = albumName,
        poster = firstVideo.thumbnailUri,
        uri = firstVideo.uri,
        duration = totalDuration,
        videos = videoList
      )
    }.sortedBy { it.title }
  }

  suspend fun getVideoByRangeOfDays(
    context: Context,
    from: Long,
    to: Long,
    page: Int = 1,
    pageSize: Int = 20,
    minDuration: Long = 0
  ): List<LocalVideo> {
    require(page >= 1) { "Page number must be 1 or greater (1-based indexing)" }
    require(pageSize > 0) { "Page size must be positive" }
    require(from <= to) { "From date must be less than or equal to To date" }
    require(minDuration >= 0) { "Minimum duration must be non-negative" }

    refreshMediaStore(context)

    val selection = if (minDuration > 0) {
      "${MediaStore.Video.Media.DATE_ADDED} >= ? AND ${MediaStore.Video.Media.DATE_ADDED} <= ? AND ${MediaStore.Video.Media.DURATION} > ?"
    } else {
      "${MediaStore.Video.Media.DATE_ADDED} >= ? AND ${MediaStore.Video.Media.DATE_ADDED} <= ?"
    }
    val selectionArgs = if (minDuration > 0) {
      arrayOf((from / 1000).toString(), (to / 1000).toString(), minDuration.toString())
    } else {
      arrayOf((from / 1000).toString(), (to / 1000).toString())
    }
    val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

    return queryMediaStore(
      context, "video", videoProjection, selection, selectionArgs, sortOrder, page, pageSize
    ) { cursor ->
      val id = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
        ?: return@queryMediaStore null
      val title =
        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
          ?: "Unknown Title"
      val data = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
        ?: return@queryMediaStore null
      val duration =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)) ?: 0L
      val size =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)) ?: 0L
      val dateAdded =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))
          ?.times(1000) ?: 0L
      val album = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM))
        ?: "Unknown Album"
      val width = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH))
      val height = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT))

      val thumbnailUri = ContentUris.withAppendedId(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        id.toLong()
      ).toString()

      LocalVideo(
        id = id,
        title = title,
        thumbnailUri = thumbnailUri,
        uri = data,
        duration = duration,
        fileSize = size,
        dateAdded = dateAdded,
        album = album,
        width = width,
        height = height
      )
    } as List<LocalVideo>
  }

  fun getVideoCount(context: Context, minDuration: Long = 0): Int {
    require(minDuration >= 0) { "Minimum duration must be non-negative" }

    var count = 0
    val uris = getContentUris("video")
    val selection = if (minDuration > 0) {
      "${MediaStore.Video.Media.DURATION} > ?"
    } else {
      null
    }
    val selectionArgs = if (minDuration > 0) arrayOf(minDuration.toString()) else null

    uris.forEach { uri ->
      context.contentResolver.query(
        uri,
        arrayOf(MediaStore.Video.Media._ID),
        selection,
        selectionArgs,
        null
      )?.use { cursor ->
        count += cursor.count
      }
    }
    return count
  }

  private val minimalProjection = arrayOf(MediaStore.Video.Media.DATE_ADDED)

  fun getOldestVideoYear(context: Context, minDuration: Long = 0): Int? {
    require(minDuration >= 0) { "Minimum duration must be non-negative" }

    val uris = getContentUris("video")
    val selection = if (minDuration > 0) {
      "${MediaStore.Video.Media.DURATION} > ?"
    } else {
      null
    }
    val selectionArgs = if (minDuration > 0) arrayOf(minDuration.toString()) else null
    val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} ASC"

    var oldestYear: Int? = null
    for (uri in uris) {
      context.contentResolver.query(uri, minimalProjection, selection, selectionArgs, sortOrder)
        ?.use { cursor ->
          if (cursor.moveToFirst()) {
            val dateAdded =
              cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))
                ?.times(1000) ?: return@use
            val calendar = Calendar.getInstance().apply { timeInMillis = dateAdded }
            val year = calendar.get(Calendar.YEAR)
            if (oldestYear == null || year < oldestYear!!) {
              oldestYear = year
            }
          }
        } ?: Timber.w("Query returned null cursor when finding oldest video year on $uri")
    }

    return oldestYear
  }

  fun getVideosByAlbum(
    context: Context,
    albumName: String,
    page: Int = 1,
    pageSize: Int = 20,
    minDuration: Long = 0
  ): List<LocalVideo> {
    require(page >= 1) { "Page number must be 1 or greater (1-based indexing)" }
    require(pageSize > 0) { "Page size must be positive" }
    require(albumName.isNotEmpty()) { "Album name cannot be empty" }
    require(minDuration >= 0) { "Minimum duration must be non-negative" }

    val selection = if (minDuration > 0) {
      "${MediaStore.Video.Media.ALBUM} = ? AND ${MediaStore.Video.Media.DURATION} > ?"
    } else {
      "${MediaStore.Video.Media.ALBUM} = ?"
    }
    val selectionArgs = if (minDuration > 0) {
      arrayOf(albumName, minDuration.toString())
    } else {
      arrayOf(albumName)
    }
    val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

    return queryMediaStore(
      context, "video", videoProjection, selection, selectionArgs, sortOrder, page, pageSize
    ) { cursor ->
      val id = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
        ?: return@queryMediaStore null
      val title =
        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
          ?: "Unknown Title"
      val data = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
        ?: return@queryMediaStore null
      val duration =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)) ?: 0L
      val size =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)) ?: 0L
      val dateAdded =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))
          ?.times(1000) ?: 0L
      val album = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM))
        ?: "Unknown Album"
      val width = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH))
      val height = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT))

      val thumbnailUri = ContentUris.withAppendedId(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        id.toLong()
      ).toString()

      LocalVideo(
        id = id,
        title = title,
        thumbnailUri = thumbnailUri,
        uri = data,
        duration = duration,
        fileSize = size,
        dateAdded = dateAdded,
        album = album,
        width = width,
        height = height
      )
    } as List<LocalVideo>
  }

  fun getAllVideosByAlbum(
    context: Context,
    albumName: String,
    minDuration: Long = 0
  ): List<LocalVideo> {
    return getVideosByAlbum(
      context,
      albumName,
      page = 1,
      pageSize = Int.MAX_VALUE,
      minDuration = minDuration
    )
  }

  fun getAllTracks(
    context: Context,
    page: Int = 1,
    pageSize: Int = 20,
    minDuration: Long = 0
  ): List<Track> {
    require(page >= 1) { "Page number must be 1 or greater (1-based indexing)" }
    require(pageSize > 0) { "Page size must be positive" }
    require(minDuration >= 0) { "Minimum duration must be non-negative" }

    val selection = if (minDuration > 0) {
      "${MediaStore.Audio.Media.DURATION} > ?"
    } else {
      null
    }
    val selectionArgs = if (minDuration > 0) arrayOf(minDuration.toString()) else null
    val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

    return queryMediaStore(
      context, "audio", audioProjection, selection, selectionArgs, sortOrder, page, pageSize
    ) { cursor ->
      val id = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
        ?: return@queryMediaStore null
      val title =
        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
          ?: "Unknown Title"
      val data = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        ?: return@queryMediaStore null
      val duration =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)) ?: 0L
      val artistName =
        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
          ?: "Unknown Artist"
      val album = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
        ?: "Unknown Album"
      val dateAdded =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED))
          ?.times(1000) ?: 0L

      val artist = Artist(
        id = artistName.hashCode().toString(),
        name = artistName,
        cover = null,
        followers = null,
        description = null,
        banners = listOf(),
        isFollowing = false,
        subtitle = null,
        extras = mapOf()
      )

      Track(
        id = id,
        uri = data,
        title = title,
        artists = listOf(artist),
        album = album,
        cover = null,
        duration = duration,
        plays = null,
        releaseDate = dateAdded,
        description = null
      )
    } as List<Track>
  }

  suspend fun getMusicCollections(context: Context, minDuration: Long = 0): List<Playlist> {
    require(minDuration >= 0) { "Minimum duration must be non-negative" }

    val albumMap = mutableMapOf<String, MutableList<Track>>()
    val uris = getContentUris("audio")

    withContext(Dispatchers.IO) {
      uris.forEach { uri ->
        val projection = arrayOf(
          MediaStore.Audio.Media.ALBUM,
          MediaStore.Audio.Media._ID,
          MediaStore.Audio.Media.DISPLAY_NAME,
          MediaStore.Audio.Media.DATA,
          MediaStore.Audio.Media.DURATION,
          MediaStore.Audio.Media.ARTIST,
          MediaStore.Audio.Media.DATE_ADDED
        )
        val selection = if (minDuration > 0) {
          "${MediaStore.Audio.Media.DURATION} > ?"
        } else {
          null
        }
        val selectionArgs = if (minDuration > 0) arrayOf(minDuration.toString()) else null
        val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
          ?.use { cursor ->
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
              val albumName = cursor.getStringOrNull(albumColumn) ?: "Unknown Album"
              val id = cursor.getStringOrNull(idColumn) ?: continue
              val title = cursor.getStringOrNull(titleColumn) ?: "Unknown Title"
              val data = cursor.getStringOrNull(dataColumn) ?: continue
              val duration = cursor.getLongOrNull(durationColumn) ?: 0L
              val artistName = cursor.getStringOrNull(artistColumn) ?: "Unknown Artist"
              val dateAdded = cursor.getLongOrNull(dateAddedColumn)?.times(1000) ?: 0L

              val artist = Artist(
                id = artistName.hashCode().toString(),
                name = artistName,
                cover = null,
                followers = null,
                description = null,
                banners = listOf(),
                isFollowing = false,
                subtitle = null,
                extras = mapOf()
              )

              val track = Track(
                id = id,
                uri = data,
                title = title,
                artists = listOf(artist),
                album = albumName,
                cover = null,
                duration = duration,
                plays = null,
                releaseDate = dateAdded,
                description = null
              )

              albumMap.getOrPut(albumName) { mutableListOf() }.add(track)
            }
          }
      }
    }

    return albumMap.map { (albumName, trackList) ->
      val totalDuration = trackList.sumOf { it.duration ?: 0L }
      val cover = getAlbumArt(context, albumName)?.toImageHolder()
      val authors = trackList
        .flatMap { it.artists }
        .distinctBy { it.id }
        .map { artist -> User(id = artist.id, name = artist.name) }
      val creationDate = trackList.mapNotNull { it.releaseDate }.minOrNull()?.toString()

      Playlist(
        id = albumName.hashCode().toString(),
        title = albumName,
        isEditable = false,
        cover = cover,
        authors = authors,
        creationDate = creationDate,
        duration = totalDuration,
        description = null,
        subtitle = null,
        tracks = trackList,
        extras = mapOf()
      )
    }.sortedBy { it.title }
  }

  fun searchVideos(
    context: Context,
    query: String,
    page: Int = 1,
    pageSize: Int = 20,
    minDuration: Long = 0
  ): List<LocalVideo> {
    require(page >= 1) { "Page number must be 1 or greater (1-based indexing)" }
    require(pageSize > 0) { "Page size must be positive" }
    require(minDuration >= 0) { "Minimum duration must be non-negative" }

    val selection = if (minDuration > 0) {
      "(${MediaStore.Video.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Video.Media.ALBUM} LIKE ?) AND ${MediaStore.Video.Media.DURATION} > ?"
    } else {
      "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Video.Media.ALBUM} LIKE ?"
    }
    val selectionArgs = if (minDuration > 0) {
      arrayOf("%$query%", "%$query%", minDuration.toString())
    } else {
      arrayOf("%$query%", "%$query%")
    }
    val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

    return queryMediaStore(
      context, "video", videoProjection, selection, selectionArgs, sortOrder, page, pageSize
    ) { cursor ->
      val id = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID))
        ?: return@queryMediaStore null
      val title =
        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME))
          ?: "Unknown Title"
      val data = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA))
        ?: return@queryMediaStore null
      val duration =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)) ?: 0L
      val size =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)) ?: 0L
      val dateAdded =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED))
          ?.times(1000) ?: 0L
      val album = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM))
        ?: "Unknown Album"
      val width = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH))
      val height = cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT))

      val thumbnailUri = ContentUris.withAppendedId(
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        id.toLong()
      ).toString()

      LocalVideo(
        id = id,
        title = title,
        thumbnailUri = thumbnailUri,
        uri = data,
        duration = duration,
        fileSize = size,
        dateAdded = dateAdded,
        album = album,
        width = width,
        height = height
      )
    } as List<LocalVideo>
  }

  fun refreshMediaStore(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      getContentUris("video").forEach { uri ->
        context.contentResolver.query(uri, arrayOf(MediaStore.Video.Media._ID), null, null, null)
          ?.close()
      }
      getContentUris("audio").forEach { uri ->
        context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media._ID), null, null, null)
          ?.close()
      }
    } else {
      context.sendBroadcast(android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE))
    }
  }


  private fun getAlbumArt(context: Context, albumName: String): String? {
    if (albumArtCache.containsKey(albumName)) return albumArtCache[albumName]

    val uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ALBUM_ART)
    val selection = "${MediaStore.Audio.Albums.ALBUM} = ?"
    val selectionArgs = arrayOf(albumName)

    val albumArt = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
      ?.use { cursor ->
        if (cursor.moveToFirst()) {
          val albumId = cursor.getLongOrNull(cursor.getColumnIndex(MediaStore.Audio.Albums._ID))
            ?: return@use null
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            cursor.getStringOrNull(cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART))
          } else {
            ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
              .toString()
          }
        } else null
      }

    albumArtCache[albumName] = albumArt
    return albumArt
  }

  fun renameMedia(context: Context, media: AVPMediaItem, newName: String): Boolean {
    require(newName.isNotBlank()) { "New name cannot be blank" }

    val (id, uri, mediaType) = when (media) {
      is AVPMediaItem.VideoItem -> {
        if (media.video is LocalVideo) Triple(
          (media.video as LocalVideo).id,
          media.video.uri,
          "video"
        )
        else throw IllegalArgumentException("Unsupported media type: ${media::class.java}")
      }

      is AVPMediaItem.TrackItem -> Triple(media.track.id, media.track.uri, "audio")
      else -> throw IllegalArgumentException("Unsupported media type: ${media::class.java}")
    }

    if (id.isBlank()) {
      Timber.e("Invalid MediaStore ID for $mediaType: $id")
      return false
    }

    val uris = getContentUris(mediaType)

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val selection = if (mediaType == "video") {
          "${MediaStore.Video.Media._ID} = ?"
        } else {
          "${MediaStore.Audio.Media._ID} = ?"
        }
        val selectionArgs = arrayOf(id)

        for (contentUri in uris) {
          val exists = context.contentResolver.query(
            contentUri,
            arrayOf(if (mediaType == "video") MediaStore.Video.Media._ID else MediaStore.Audio.Media._ID),
            selection,
            selectionArgs,
            null
          )?.use { cursor -> cursor.count > 0 } ?: false

          if (!exists) {
            Timber.w("No $mediaType found in MediaStore with ID: $id on $contentUri")
            continue
          }

          val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
          }

          val updatedRows =
            context.contentResolver.update(contentUri, contentValues, selection, selectionArgs)

          if (updatedRows > 0) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(contentUri, contentValues, selection, selectionArgs)
            Timber.d("Renamed $mediaType to $newName successfully via MediaStore on $contentUri")
            return true
          }
        }

        Timber.e("Failed to rename $mediaType: No rows updated (ID: $id) on any volume")
        return false
      } else {
        val oldFile = File(uri)
        if (!oldFile.exists()) {
          Timber.e("File does not exist: $uri")
          return false
        }

        val extension = oldFile.extension
        val newFileName = "$newName.$extension"
        val newFile = File(oldFile.parent, newFileName)

        if (oldFile.renameTo(newFile)) {
          val contentUri =
            if (mediaType == "video") MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
          val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DATA, newFile.absolutePath)
            put(MediaStore.MediaColumns.DISPLAY_NAME, newFileName)
          }

          val selection = if (mediaType == "video") {
            "${MediaStore.Video.Media._ID} = ?"
          } else {
            "${MediaStore.Audio.Media._ID} = ?"
          }
          val selectionArgs = arrayOf(id)

          val updatedRows =
            context.contentResolver.update(contentUri, contentValues, selection, selectionArgs)

          if (updatedRows > 0) {
            Timber.d("Renamed $mediaType to $newFileName successfully")
            return true
          } else {
            Timber.e("Failed to update MediaStore after renaming $mediaType (ID: $id)")
            return false
          }
        } else {
          Timber.e("Failed to rename file: $uri to $newFileName")
          return false
        }
      }
    } catch (e: Exception) {
      Timber.e(e, "Error renaming $mediaType: ${e.message}")
      return false
    }
  }

  fun deleteMedia(context: Context, media: AVPMediaItem): Boolean {
    val (id, uri, mediaType) = when (media) {
      is AVPMediaItem.VideoItem -> {
        if (media.video is LocalVideo) Triple(
          (media.video as LocalVideo).id,
          media.video.uri,
          "video"
        )
        else throw IllegalArgumentException("Unsupported media type: ${media::class.java}")
      }

      is AVPMediaItem.TrackItem -> Triple(media.track.id, media.track.uri, "audio")
      else -> throw IllegalArgumentException("Unsupported media type: ${media::class.java}")
    }

    if (id.isBlank()) {
      Timber.e("Invalid MediaStore ID for $mediaType: $id")
      return false
    }

    refreshMediaStore(context)
    val uris = getContentUris(mediaType)

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val selection = if (mediaType == "video") {
          "${MediaStore.Video.Media._ID} = ?"
        } else {
          "${MediaStore.Audio.Media._ID} = ?"
        }
        val selectionArgs = arrayOf(id)

        for (contentUri in uris) {
          val exists = context.contentResolver.query(
            contentUri,
            arrayOf(if (mediaType == "video") MediaStore.Video.Media._ID else MediaStore.Audio.Media._ID),
            selection,
            selectionArgs,
            null
          )?.use { cursor -> cursor.count > 0 } ?: false

          if (!exists) {
            Timber.w("No $mediaType found in MediaStore with ID: $id on $contentUri")
            continue
          }

          val deletedRows = context.contentResolver.delete(contentUri, selection, selectionArgs)

          if (deletedRows > 0) {
            Timber.d("Deleted $mediaType successfully via MediaStore (ID: $id) on $contentUri")
            return true
          }
        }

        Timber.e("Failed to delete $mediaType: No rows deleted (ID: $id) on any volume")
        return false
      } else {
        val file = File(uri)
        if (!file.exists()) {
          Timber.e("File does not exist: $uri")
          return false
        }

        if (file.delete()) {
          val contentUri =
            if (mediaType == "video") MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
          val selection = if (mediaType == "video") {
            "${MediaStore.Video.Media._ID} = ?"
          } else {
            "${MediaStore.Audio.Media._ID} = ?"
          }
          val selectionArgs = arrayOf(id)

          val deletedRows = context.contentResolver.delete(contentUri, selection, selectionArgs)

          if (deletedRows > 0) {
            Timber.d("Deleted $mediaType successfully (ID: $id)")
            return true
          } else {
            Timber.e("Failed to delete $mediaType from MediaStore after file deletion (ID: $id)")
            return false
          }
        } else {
          Timber.e("Failed to delete file: $uri")
          return false
        }
      }
    } catch (e: Exception) {
      Timber.e(e, "Error deleting $mediaType: ${e.message}")
      return false
    }
  }

  fun searchTracks(
    context: Context,
    query: String,
    page: Int = 1,
    pageSize: Int = 20,
    minDuration: Long = 0
  ): List<Track> {
    require(page >= 1) { "Page number must be 1 or greater (1-based indexing)" }
    require(pageSize > 0) { "Page size must be positive" }
    require(minDuration >= 0) { "Minimum duration must be non-negative" }

    val selection = if (minDuration > 0) {
      "(${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?) AND ${MediaStore.Audio.Media.DURATION} > ?"
    } else {
      "${MediaStore.Audio.Media.DISPLAY_NAME} LIKE ? OR ${MediaStore.Audio.Media.ALBUM} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?"
    }
    val selectionArgs = if (minDuration > 0) {
      arrayOf("%$query%", "%$query%", "%$query%", minDuration.toString())
    } else {
      arrayOf("%$query%", "%$query%", "%$query%")
    }
    val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

    return queryMediaStore(
      context, "audio", audioProjection, selection, selectionArgs, sortOrder, page, pageSize
    ) { cursor ->
      val id = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
        ?: return@queryMediaStore null
      val title =
        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
          ?: "Unknown Title"
      val data = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        ?: return@queryMediaStore null
      val duration =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)) ?: 0L
      val artistName =
        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
          ?: "Unknown Artist"
      val album = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
        ?: "Unknown Album"
      val dateAdded =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED))
          ?.times(1000) ?: 0L

      val artist = Artist(
        id = artistName.hashCode().toString(),
        name = artistName,
        cover = null,
        followers = null,
        description = null,
        banners = listOf(),
        isFollowing = false,
        subtitle = null,
        extras = mapOf()
      )

      // Fetch album art and convert to ImageHolder
      val cover = getAlbumArt(context, album)

      Track(
        id = id,
        uri = data,
        title = title,
        artists = listOf(artist),
        album = album,
        cover = cover, // Set the album art
        duration = duration,
        plays = null,
        releaseDate = dateAdded,
        description = null
      )
    } as List<Track>
  }

  fun getTracksByMinDuration(
    context: Context,
    minDuration: Long,
    page: Int = 1,
    pageSize: Int = 20
  ): List<Track> {
    require(page >= 1) { "Page number must be 1 or greater (1-based indexing)" }
    require(pageSize > 0) { "Page size must be positive" }
    require(minDuration >= 0) { "Minimum duration must be non-negative" }

    val selection = "${MediaStore.Audio.Media.DURATION} > ?"
    val selectionArgs = arrayOf(minDuration.toString())
    val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

    return queryMediaStore(
      context, "audio", audioProjection, selection, selectionArgs, sortOrder, page, pageSize
    ) { cursor ->
      val id = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
        ?: return@queryMediaStore null
      val title =
        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME))
          ?: "Unknown Title"
      val data = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        ?: return@queryMediaStore null
      val duration =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)) ?: 0L
      val artistName =
        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
          ?: "Unknown Artist"
      val album = cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
        ?: "Unknown Album"
      val dateAdded =
        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED))
          ?.times(1000) ?: 0L

      val artist = Artist(
        id = artistName.hashCode().toString(),
        name = artistName,
        cover = null,
        followers = null,
        description = null,
        banners = listOf(),
        isFollowing = false,
        subtitle = null,
        extras = mapOf()
      )

      // Fetch album art and convert to ImageHolder
      val cover = getAlbumArt(context, album)

      Track(
        id = id,
        uri = data,
        title = title,
        artists = listOf(artist),
        album = album,
        cover = cover, // Set the album art
        duration = duration,
        plays = null,
        releaseDate = dateAdded,
        description = null
      )
    } as List<Track>
  }
}
